package xyz.wagyourtail.jsmacros.client.note2midi;

public final class InstrumentMapper {
    private final RecorderConfig config;

    public InstrumentMapper(RecorderConfig config) {
        this.config = config;
    }

    public MappedNote map(CapturedNote note) {
        InstrumentMapping mapping = config.mappings.get(note.getInstrument());
        String trackInstrument = note.getInstrument();
        if (mapping == null) {
            if (config.unknownInstrumentBehavior == RecorderConfig.UnknownInstrumentBehavior.SKIP) {
                return null;
            }
            mapping = config.mappings.get("harp");
            trackInstrument = "fallback." + note.getInstrument();
        }
        if (mapping == null || !mapping.enabled || !Float.isFinite(note.getPitch()) || note.getPitch() <= 0.0f) {
            return null;
        }

        int midiNote = midiNote(mapping.baseMidiNote, note.getPitch());
        if (mapping.percussionKey >= 0 && !config.percussionPreservePitch) {
            midiNote = mapping.percussionKey;
        }
        int velocity = config.velocityMode == RecorderConfig.VelocityMode.FIXED
                ? config.fixedVelocity
                : clamp(Math.round(note.getVolume() * 127.0f), 1, 127);
        return new MappedNote(note, trackInstrument, midiNote, velocity, mapping.channel, mapping.program);
    }

    public static int midiNote(int baseMidiNote, double pitch) {
        if (!Double.isFinite(pitch) || pitch <= 0.0) {
            throw new IllegalArgumentException("pitch must be finite and positive");
        }
        return clamp(baseMidiNote + (int) Math.round(12.0 * (Math.log(pitch) / Math.log(2.0))), 0, 127);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
