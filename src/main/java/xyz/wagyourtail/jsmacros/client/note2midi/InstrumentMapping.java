package xyz.wagyourtail.jsmacros.client.note2midi;

public final class InstrumentMapping {
    public int baseMidiNote;
    public int program;
    public int channel;
    public int percussionKey;
    public boolean enabled;

    public InstrumentMapping() {
        this(66, 0, 0, -1, true);
    }

    public InstrumentMapping(int baseMidiNote, int program, int channel, int percussionKey, boolean enabled) {
        this.baseMidiNote = baseMidiNote;
        this.program = program;
        this.channel = channel;
        this.percussionKey = percussionKey;
        this.enabled = enabled;
    }

    public InstrumentMapping copy() {
        return new InstrumentMapping(baseMidiNote, program, channel, percussionKey, enabled);
    }

    public void validate() {
        baseMidiNote = clamp(baseMidiNote, 0, 127);
        program = clamp(program, 0, 127);
        channel = clamp(channel, 0, 15);
        percussionKey = percussionKey < 0 ? -1 : clamp(percussionKey, 0, 127);
        if (percussionKey >= 0) {
            channel = 9;
        } else if (channel == 9) {
            channel = 10;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
