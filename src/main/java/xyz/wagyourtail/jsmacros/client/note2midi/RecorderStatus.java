package xyz.wagyourtail.jsmacros.client.note2midi;

public final class RecorderStatus {
    private final String state;
    private final long elapsedMillis;
    private final int capturedNotes;
    private final String recentInstrument;
    private final int recentMidiNote;
    private final String backend;
    private final long skipped;
    private final String error;

    public RecorderStatus(String state, long elapsedMillis, int capturedNotes, String recentInstrument,
                          int recentMidiNote, String backend, long skipped, String error) {
        this.state = state;
        this.elapsedMillis = elapsedMillis;
        this.capturedNotes = capturedNotes;
        this.recentInstrument = recentInstrument;
        this.recentMidiNote = recentMidiNote;
        this.backend = backend;
        this.skipped = skipped;
        this.error = error;
    }

    public String getState() { return state; }
    public long getElapsedMillis() { return elapsedMillis; }
    public int getCapturedNotes() { return capturedNotes; }
    public String getRecentInstrument() { return recentInstrument; }
    public int getRecentMidiNote() { return recentMidiNote; }
    public String getBackend() { return backend; }
    public long getSkipped() { return skipped; }
    public String getError() { return error; }
}
