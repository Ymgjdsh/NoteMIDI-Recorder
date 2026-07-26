package xyz.wagyourtail.jsmacros.client.note2midi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstrumentMapperTest {
    @Test
    void pitchOctavesAreMappedAroundInstrumentBase() {
        int base = 66;
        assertEquals(base - 12, InstrumentMapper.midiNote(base, 0.5));
        assertEquals(base, InstrumentMapper.midiNote(base, 1.0));
        assertEquals(base + 12, InstrumentMapper.midiNote(base, 2.0));
    }

    @Test
    void percussionUsesGeneralMidiKeysByDefault() {
        RecorderConfig config = new RecorderConfig();
        InstrumentMapper mapper = new InstrumentMapper(config);
        CapturedNote note = note("basedrum", 2.0f);
        assertEquals(36, mapper.map(note).getMidiNote());

        config.percussionPreservePitch = true;
        assertEquals(54, new InstrumentMapper(config).map(note).getMidiNote());
    }

    private static CapturedNote note(String instrument, float pitch) {
        return new CapturedNote(CapturedNote.NOTE_BLOCK_PREFIX + instrument, pitch, 1.0f,
                0, 0, 0, 1, 1, "server", "dimension");
    }
}
