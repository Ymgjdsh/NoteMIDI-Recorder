package xyz.wagyourtail.jsmacros.client.note2midi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteFilterTest {
    @Test
    void rejectsNonNoteBlockSounds() {
        assertFalse(NoteFilter.acceptsSoundId("minecraft:block.stone.break"));
        assertFalse(NoteFilter.acceptsSoundId("minecraft:block.note_block."));
        assertFalse(NoteFilter.acceptsSoundId(null));
        assertTrue(NoteFilter.acceptsSoundId("minecraft:block.note_block.harp"));
    }

    @Test
    void distanceBoundaryIsInclusive() {
        assertTrue(NoteFilter.withinRadius(3, 4, 0, 0, 0, 0, 5));
        assertFalse(NoteFilter.withinRadius(3.001, 4, 0, 0, 0, 0, 5));
    }
}
