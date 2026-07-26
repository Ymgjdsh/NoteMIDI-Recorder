package xyz.wagyourtail.jsmacros.client.note2midi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MidiExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesReadableFormatOneWithMusicalEventsAndChordTiming() throws Exception {
        RecorderConfig config = new RecorderConfig();
        config.quantization = RecorderConfig.Quantization.GAME_TICK_2;
        Path file = FilenameUtil.reserveUniqueMidiFile(tempDir, "chord");
        List<CapturedNote> notes = List.of(
                note("harp", 1.0f, 104),
                note("harp", 2.0f, 104),
                note("bass", 1.0f, 108)
        );

        MidiExportResult result = new MidiExporter().export(notes, config, 100, file);
        Sequence sequence = MidiSystem.getSequence(file.toFile());

        assertEquals(1, MidiSystem.getMidiFileFormat(file.toFile()).getType());
        assertEquals(480, sequence.getResolution());
        assertEquals(3, result.getNoteCount());
        assertTrue(sequence.getTracks().length >= 3);
        assertTrue(hasMetaType(sequence.getTracks()[0], 0x51));
        assertTrue(hasCommand(sequence, ShortMessage.PROGRAM_CHANGE));
        assertTrue(hasCommand(sequence, ShortMessage.NOTE_ON));
        assertTrue(hasCommand(sequence, ShortMessage.NOTE_OFF));

        long[] harpOnTicks = noteOnTicks(sequence, 0);
        assertTrue(harpOnTicks.length >= 2);
        assertEquals(harpOnTicks[0], harpOnTicks[1]);
    }

    private static CapturedNote note(String instrument, float pitch, long tick) {
        return new CapturedNote(CapturedNote.NOTE_BLOCK_PREFIX + instrument, pitch, 1.0f,
                0, 64, 0, tick * 50_000_000L, tick, "test", "overworld");
    }

    private static boolean hasMetaType(Track track, int type) {
        for (int i = 0; i < track.size(); i++) {
            MidiMessage message = track.get(i).getMessage();
            if (message instanceof MetaMessage && ((MetaMessage) message).getType() == type) return true;
        }
        return false;
    }

    private static boolean hasCommand(Sequence sequence, int command) {
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiMessage message = track.get(i).getMessage();
                if (message instanceof ShortMessage && ((ShortMessage) message).getCommand() == command) return true;
            }
        }
        return false;
    }

    private static long[] noteOnTicks(Sequence sequence, int channel) {
        java.util.ArrayList<Long> ticks = new java.util.ArrayList<>();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof ShortMessage) {
                    ShortMessage message = (ShortMessage) event.getMessage();
                    if (message.getCommand() == ShortMessage.NOTE_ON && message.getChannel() == channel
                            && message.getData2() > 0) ticks.add(event.getTick());
                }
            }
        }
        return ticks.stream().mapToLong(Long::longValue).toArray();
    }
}
