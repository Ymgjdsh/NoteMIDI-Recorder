package xyz.wagyourtail.jsmacros.client.note2midi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecorderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum CaptureMode { SOUND_EVENT, STRICT_PACKET }
    public enum DistanceMode { OFF, START_POSITION, FOLLOW_PLAYER, FIXED_POSITION }
    public enum Quantization { OFF, GAME_TICK_1, GAME_TICK_2, SIXTEENTH, EIGHTH, QUARTER }
    public enum VelocityMode { FIXED, VOLUME }
    public enum UnknownInstrumentBehavior { FALLBACK, SKIP }
    public enum DimensionChangeBehavior { CONTINUE, STOP_AND_EXPORT, CANCEL }

    public CaptureMode captureMode = CaptureMode.SOUND_EVENT;
    public DistanceMode distanceMode = DistanceMode.START_POSITION;
    public Quantization quantization = Quantization.GAME_TICK_2;
    public VelocityMode velocityMode = VelocityMode.VOLUME;
    public UnknownInstrumentBehavior unknownInstrumentBehavior = UnknownInstrumentBehavior.FALLBACK;
    public DimensionChangeBehavior dimensionChangeBehavior = DimensionChangeBehavior.CONTINUE;
    public int bpm = 120;
    public int ppq = 480;
    public int noteLengthGameTicks = 2;
    public int fixedVelocity = 96;
    public double filterRadius = 64.0;
    public double fixedX = 0.0;
    public double fixedY = 64.0;
    public double fixedZ = 0.0;
    public boolean percussionPreservePitch = false;
    public boolean autoExportOnDisconnect = true;
    public boolean hudEnabled = true;
    public Map<String, InstrumentMapping> mappings = defaultMappings();

    public static RecorderConfig load(Path path) throws IOException {
        RecorderConfig config;
        if (!Files.exists(path)) {
            config = new RecorderConfig();
            config.save(path);
            return config;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            config = GSON.fromJson(reader, RecorderConfig.class);
        } catch (RuntimeException ex) {
            throw new IOException("Invalid recorder config: " + path, ex);
        }
        if (config == null) {
            config = new RecorderConfig();
        }
        config.validate();
        return config;
    }

    public void save(Path path) throws IOException {
        validate();
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        }
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void validate() {
        bpm = clamp(bpm, 20, 300);
        ppq = clamp(ppq, 24, 9600);
        noteLengthGameTicks = clamp(noteLengthGameTicks, 1, 200);
        fixedVelocity = clamp(fixedVelocity, 1, 127);
        filterRadius = Math.max(0.0, Math.min(4096.0, filterRadius));
        if (captureMode == null) captureMode = CaptureMode.SOUND_EVENT;
        if (distanceMode == null) distanceMode = DistanceMode.START_POSITION;
        if (quantization == null) quantization = Quantization.GAME_TICK_2;
        if (velocityMode == null) velocityMode = VelocityMode.VOLUME;
        if (unknownInstrumentBehavior == null) unknownInstrumentBehavior = UnknownInstrumentBehavior.FALLBACK;
        if (dimensionChangeBehavior == null) dimensionChangeBehavior = DimensionChangeBehavior.CONTINUE;

        Map<String, InstrumentMapping> defaults = defaultMappings();
        if (mappings == null) mappings = new LinkedHashMap<>();
        defaults.forEach((name, mapping) -> mappings.putIfAbsent(name, mapping));
        mappings.values().removeIf(java.util.Objects::isNull);
        mappings.values().forEach(InstrumentMapping::validate);
    }

    public static Map<String, InstrumentMapping> defaultMappings() {
        Map<String, InstrumentMapping> result = new LinkedHashMap<>();
        put(result, "harp", 66, 0, 0);
        putPercussion(result, "basedrum", 42, 36);
        putPercussion(result, "snare", 66, 38);
        putPercussion(result, "hat", 66, 42);
        put(result, "bass", 42, 33, 1);
        put(result, "flute", 78, 73, 2);
        put(result, "bell", 90, 14, 3);
        put(result, "guitar", 54, 24, 4);
        put(result, "chime", 90, 14, 3);
        put(result, "xylophone", 90, 13, 5);
        put(result, "iron_xylophone", 78, 11, 6);
        put(result, "cow_bell", 78, 112, 7);
        put(result, "didgeridoo", 42, 58, 8);
        put(result, "bit", 66, 80, 10);
        put(result, "banjo", 66, 105, 11);
        put(result, "pling", 66, 10, 12);
        put(result, "imitate.zombie", 66, 52, 13);
        put(result, "imitate.skeleton", 66, 52, 13);
        put(result, "imitate.creeper", 66, 52, 14);
        put(result, "imitate.ender_dragon", 66, 53, 14);
        put(result, "imitate.wither_skeleton", 66, 53, 15);
        put(result, "imitate.piglin", 66, 53, 15);
        put(result, "custom_head", 66, 52, 15);
        return result;
    }

    private static void put(Map<String, InstrumentMapping> map, String name, int base, int program, int channel) {
        map.put(name, new InstrumentMapping(base, program, channel, -1, true));
    }

    private static void putPercussion(Map<String, InstrumentMapping> map, String name, int base, int key) {
        map.put(name, new InstrumentMapping(base, 0, 9, key, true));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
