package xyz.wagyourtail.jsmacros.client.note2midi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import xyz.wagyourtail.jsmacros.client.JsMacros;

import javax.sound.midi.InvalidMidiDataException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MidiRecorderController {
    public enum State { IDLE, RECORDING, EXPORTING, ERROR }

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Path configPath;
    private final ConcurrentLinkedQueue<CapturedNote> pending = new ConcurrentLinkedQueue<>();
    private final List<CapturedNote> captured = new ArrayList<>();
    private final CaptureDiagnostics diagnostics = new CaptureDiagnostics();
    private final SoundEventCaptureBackend soundBackend = new SoundEventCaptureBackend(this);
    private final PacketCaptureBackend packetBackend = new PacketCaptureBackend();
    private final ExecutorService exportExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Note2MIDI Exporter");
        thread.setDaemon(false);
        return thread;
    });

    private volatile State state = State.IDLE;
    private volatile RecorderConfig config;
    private volatile ClientSnapshot snapshot = ClientSnapshot.empty();
    private volatile NoteCaptureBackend activeBackend;
    private volatile long clientTickCounter;
    private volatile long startClientTick;
    private volatile long startNanoTime;
    private volatile double startX;
    private volatile double startY;
    private volatile double startZ;
    private volatile String startServer = "unknown-server";
    private volatile String startDimension = "unknown-dimension";
    private volatile String recentInstrument = "-";
    private volatile int recentMidiNote = -1;
    private volatile String lastError;
    private volatile String previousDimension;

    public MidiRecorderController(Path configPath) {
        this.configPath = configPath.toAbsolutePath();
        this.config = loadConfigSafely();
    }

    public boolean start() {
        return callOnClientThread(this::startOnClientThread, false);
    }

    private boolean startOnClientThread() {
        if (state == State.RECORDING || state == State.EXPORTING) return false;
        stopBackends();
        pending.clear();
        synchronized (captured) {
            captured.clear();
        }
        diagnostics.reset();
        lastError = null;
        recentInstrument = "-";
        recentMidiNote = -1;
        updateSnapshot();
        startClientTick = clientTickCounter;
        startNanoTime = System.nanoTime();
        startServer = snapshot.server;
        startDimension = snapshot.dimension;
        startX = snapshot.playerX;
        startY = snapshot.playerY;
        startZ = snapshot.playerZ;
        previousDimension = snapshot.dimension;
        activeBackend = config.captureMode == RecorderConfig.CaptureMode.STRICT_PACKET ? packetBackend : soundBackend;
        state = State.RECORDING;
        try {
            activeBackend.start();
        } catch (RuntimeException ex) {
            state = State.ERROR;
            lastError = ex.getMessage();
            activeBackend = null;
            return false;
        }
        chat("Note2MIDI recording started (" + activeBackend.source().name().toLowerCase(Locale.ROOT) + ").");
        return true;
    }

    public boolean stopAndExport() {
        return stopAndExport(null);
    }

    public boolean stopAndExport(String requestedPath) {
        return callOnClientThread(() -> beginExport(requestedPath, false), false);
    }

    public boolean exportCaptured(String requestedPath) {
        return callOnClientThread(() -> beginExport(requestedPath, true), false);
    }

    private boolean beginExport(String requestedPath, boolean allowIdle) {
        if (state == State.EXPORTING || (!allowIdle && state != State.RECORDING)) return false;
        if (state != State.RECORDING && state != State.IDLE && state != State.ERROR) return false;
        if (state == State.RECORDING) {
            drainPending();
            stopBackends();
        }
        List<CapturedNote> notes;
        synchronized (captured) {
            notes = List.copyOf(captured);
        }
        RecorderConfig exportConfig = config;
        long exportStartTick = startClientTick;
        state = State.EXPORTING;
        activeBackend = null;

        exportExecutor.submit(() -> {
            try {
                Path output = reserveOutputPath(requestedPath);
                MidiExportResult result = new MidiExporter().export(notes, exportConfig, exportStartTick, output);
                long captureSkipped = diagnostics.skippedDuringCapture();
                client.execute(() -> {
                    state = State.IDLE;
                    chat(String.format(Locale.ROOT,
                            "Note2MIDI saved %s | notes %d | tracks %d | %.2fs | skipped %d",
                            result.getPath(), result.getNoteCount(), result.getTrackCount(), result.getDurationSeconds(),
                            result.getSkippedCount() + captureSkipped));
                });
            } catch (IOException | InvalidMidiDataException | RuntimeException ex) {
                client.execute(() -> {
                    state = State.ERROR;
                    lastError = ex.getMessage();
                    chat("Note2MIDI export failed: " + ex.getMessage());
                });
            }
        });
        return true;
    }

    public boolean cancel() {
        return callOnClientThread(() -> {
            if (state == State.EXPORTING) return false;
            stopBackends();
            pending.clear();
            synchronized (captured) {
                captured.clear();
            }
            state = State.IDLE;
            activeBackend = null;
            lastError = null;
            chat("Note2MIDI recording cancelled.");
            return true;
        }, false);
    }

    public boolean reloadConfig() {
        return callOnClientThread(() -> {
            NoteCaptureBackend before = activeBackend;
            if (before != null) before.stop();
            RecorderConfig loaded = loadConfigSafely();
            config = loaded;
            if (state == State.RECORDING) {
                activeBackend = loaded.captureMode == RecorderConfig.CaptureMode.STRICT_PACKET
                        ? packetBackend : soundBackend;
                activeBackend.start();
            }
            chat("Note2MIDI config reloaded.");
            return true;
        }, false);
    }

    public boolean saveConfig() {
        try {
            config.save(configPath);
            return true;
        } catch (IOException ex) {
            lastError = ex.getMessage();
            return false;
        }
    }

    public void capture(CaptureSource source, String soundId, float pitch, float volume,
                        double x, double y, double z) {
        NoteCaptureBackend backend = activeBackend;
        if (state != State.RECORDING || backend == null || !backend.isRunning() || backend.source() != source) return;
        if (!NoteFilter.acceptsSoundId(soundId)) {
            diagnostics.incrementNonNoteSounds();
            return;
        }
        if (!Float.isFinite(pitch) || pitch <= 0.0f || !Float.isFinite(volume)
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            diagnostics.incrementInvalidEvents();
            return;
        }
        ClientSnapshot current = snapshot;
        pending.offer(new CapturedNote(soundId, pitch, volume, x, y, z, System.nanoTime(),
                clientTickCounter, current.server, current.dimension));
    }

    public void incrementMissingEntity() {
        if (state == State.RECORDING && activeBackend == packetBackend) diagnostics.incrementMissingEntities();
    }

    public void onEndClientTick() {
        clientTickCounter++;
        if (state == State.RECORDING && activeBackend == soundBackend) {
            soundBackend.ensureRegisteredAfterProfileReload();
        }
        String beforeDimension = snapshot.dimension;
        updateSnapshot();
        if (state == State.RECORDING && previousDimension != null
                && !previousDimension.equals(snapshot.dimension) && !beforeDimension.equals("unknown-dimension")) {
            RecorderConfig.DimensionChangeBehavior behavior = config.dimensionChangeBehavior;
            previousDimension = snapshot.dimension;
            if (behavior == RecorderConfig.DimensionChangeBehavior.STOP_AND_EXPORT) {
                beginExport(null, false);
                return;
            }
            if (behavior == RecorderConfig.DimensionChangeBehavior.CANCEL) {
                cancel();
                return;
            }
        }
        if (state == State.RECORDING) drainPending();
    }

    public void onDisconnect() {
        if (!client.isOnThread()) {
            client.execute(this::onDisconnect);
            return;
        }
        if (state == State.RECORDING) {
            if (config.autoExportOnDisconnect) stopAndExport();
            else cancel();
        }
    }

    public void shutdown() {
        callOnClientThread(() -> {
            if (state == State.RECORDING) beginExport(null, false);
            return true;
        }, false);
        exportExecutor.shutdown();
        try {
            exportExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void drainPending() {
        CapturedNote note;
        while ((note = pending.poll()) != null) {
            if (!passesDistanceFilter(note)) {
                diagnostics.incrementDistanceFiltered();
                continue;
            }
            synchronized (captured) {
                captured.add(note);
            }
            MappedNote mapped = new InstrumentMapper(config).map(note);
            recentInstrument = note.getInstrument();
            recentMidiNote = mapped == null ? -1 : mapped.getMidiNote();
        }
    }

    private boolean passesDistanceFilter(CapturedNote note) {
        RecorderConfig.DistanceMode mode = config.distanceMode;
        if (mode == RecorderConfig.DistanceMode.OFF) return true;
        double centerX;
        double centerY;
        double centerZ;
        if (mode == RecorderConfig.DistanceMode.START_POSITION) {
            centerX = startX;
            centerY = startY;
            centerZ = startZ;
        } else if (mode == RecorderConfig.DistanceMode.FIXED_POSITION) {
            centerX = config.fixedX;
            centerY = config.fixedY;
            centerZ = config.fixedZ;
        } else {
            centerX = snapshot.playerX;
            centerY = snapshot.playerY;
            centerZ = snapshot.playerZ;
        }
        return NoteFilter.withinRadius(note.getX(), note.getY(), note.getZ(),
                centerX, centerY, centerZ, config.filterRadius);
    }

    private void updateSnapshot() {
        String server = "singleplayer";
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null && info.address != null && !info.address.isBlank()) server = info.address;
        String dimension = client.world == null ? "unknown-dimension"
                : client.world.getRegistryKey().getValue().toString();
        double x = snapshot.playerX;
        double y = snapshot.playerY;
        double z = snapshot.playerZ;
        if (client.player != null) {
            x = client.player.getX();
            y = client.player.getY();
            z = client.player.getZ();
        }
        snapshot = new ClientSnapshot(server, dimension, x, y, z);
    }

    private Path reserveOutputPath(String requestedPath) throws IOException {
        String baseName = FilenameUtil.recordingBaseName(LocalDateTime.now(), startServer, startDimension);
        Path macroFolder = JsMacros.core.config.macroFolder.toPath().toAbsolutePath();
        if (requestedPath == null || requestedPath.isBlank()) {
            return FilenameUtil.reserveUniqueMidiFile(macroFolder.resolve("midi"), baseName);
        }
        Path requested = Path.of(requestedPath);
        if (!requested.isAbsolute()) requested = macroFolder.resolve(requested);
        requested = requested.toAbsolutePath();
        if (Files.isDirectory(requested) || !requested.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mid")) {
            return FilenameUtil.reserveUniqueMidiFile(requested, baseName);
        }
        Path parent = requested.getParent();
        String filename = requested.getFileName().toString();
        return FilenameUtil.reserveUniqueMidiFile(parent, filename.substring(0, filename.length() - 4));
    }

    private RecorderConfig loadConfigSafely() {
        try {
            return RecorderConfig.load(configPath);
        } catch (IOException ex) {
            lastError = ex.getMessage();
            RecorderConfig fallback = new RecorderConfig();
            try {
                fallback.save(configPath);
            } catch (IOException ignored) {
            }
            return fallback;
        }
    }

    private void stopBackends() {
        soundBackend.stop();
        packetBackend.stop();
    }

    public void openOutputFolder() {
        Path output = JsMacros.core.config.macroFolder.toPath().resolve("midi").toAbsolutePath();
        try {
            Files.createDirectories(output);
            Util.getOperatingSystem().open(output.toFile());
        } catch (IOException ex) {
            chat("Could not open Note2MIDI folder: " + ex.getMessage());
        }
    }

    private void chat(String message) {
        if (client.inGameHud != null) client.inGameHud.getChatHud().addMessage(Text.literal(message));
        JsMacros.LOGGER.info(message);
    }

    private <T> T callOnClientThread(java.util.concurrent.Callable<T> task, T fallback) {
        if (client.isOnThread()) {
            try {
                return task.call();
            } catch (Exception ex) {
                lastError = ex.getMessage();
                return fallback;
            }
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        client.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            lastError = ex.getMessage();
            return fallback;
        }
    }

    public RecorderStatus getStatus() {
        NoteCaptureBackend backend = activeBackend;
        return new RecorderStatus(state.name(), getElapsedMillis(), getCapturedNoteCount(), recentInstrument,
                recentMidiNote, backend == null ? "none" : backend.source().name(), diagnostics.skippedDuringCapture(), lastError);
    }

    public List<CapturedNote> getCapturedNotes() {
        synchronized (captured) {
            return Collections.unmodifiableList(new ArrayList<>(captured));
        }
    }

    public RecorderConfig getConfig() { return config; }
    public Path getConfigPath() { return configPath; }
    public State getState() { return state; }
    public long getClientTickCounter() { return clientTickCounter; }
    public long getStartClientTick() { return startClientTick; }
    public long getStartNanoTime() { return startNanoTime; }
    public long getElapsedMillis() { return state == State.RECORDING ? Math.max(0, (System.nanoTime() - startNanoTime) / 1_000_000L) : 0; }
    public int getCapturedNoteCount() { synchronized (captured) { return captured.size(); } }
    public String getRecentInstrument() { return recentInstrument; }
    public int getRecentMidiNote() { return recentMidiNote; }
    public CaptureDiagnostics getDiagnostics() { return diagnostics; }

    private record ClientSnapshot(String server, String dimension, double playerX, double playerY, double playerZ) {
        private static ClientSnapshot empty() {
            return new ClientSnapshot("singleplayer", "unknown-dimension", 0.0, 0.0, 0.0);
        }
    }
}
