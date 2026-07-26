package xyz.wagyourtail.jsmacros.client.note2midi;

import java.nio.file.Path;

public final class MidiExportResult {
    private final Path path;
    private final int noteCount;
    private final int trackCount;
    private final int skippedCount;
    private final long durationMidiTicks;
    private final double durationSeconds;

    public MidiExportResult(Path path, int noteCount, int trackCount, int skippedCount,
                            long durationMidiTicks, double durationSeconds) {
        this.path = path;
        this.noteCount = noteCount;
        this.trackCount = trackCount;
        this.skippedCount = skippedCount;
        this.durationMidiTicks = durationMidiTicks;
        this.durationSeconds = durationSeconds;
    }

    public Path getPath() { return path; }
    public int getNoteCount() { return noteCount; }
    public int getTrackCount() { return trackCount; }
    public int getSkippedCount() { return skippedCount; }
    public long getDurationMidiTicks() { return durationMidiTicks; }
    public double getDurationSeconds() { return durationSeconds; }
}
