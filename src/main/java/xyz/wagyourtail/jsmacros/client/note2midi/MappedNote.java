package xyz.wagyourtail.jsmacros.client.note2midi;

public final class MappedNote {
    private final CapturedNote source;
    private final String instrument;
    private final int midiNote;
    private final int velocity;
    private final int channel;
    private final int program;

    public MappedNote(CapturedNote source, String instrument, int midiNote, int velocity, int channel, int program) {
        this.source = source;
        this.instrument = instrument;
        this.midiNote = midiNote;
        this.velocity = velocity;
        this.channel = channel;
        this.program = program;
    }

    public CapturedNote getSource() { return source; }
    public String getInstrument() { return instrument; }
    public int getMidiNote() { return midiNote; }
    public int getVelocity() { return velocity; }
    public int getChannel() { return channel; }
    public int getProgram() { return program; }
}
