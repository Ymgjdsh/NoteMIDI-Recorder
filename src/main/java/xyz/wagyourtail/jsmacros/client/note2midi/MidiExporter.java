package xyz.wagyourtail.jsmacros.client.note2midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MidiExporter {
    private static final int META_TRACK_NAME = 0x03;
    private static final int META_END_OF_TRACK = 0x2f;
    private static final int META_TEMPO = 0x51;
    private static final int META_TIME_SIGNATURE = 0x58;

    public MidiExportResult export(List<CapturedNote> capturedNotes, RecorderConfig config,
                                   long startClientTick, Path file) throws IOException, InvalidMidiDataException {
        config.validate();
        Sequence sequence = new Sequence(Sequence.PPQ, config.ppq);
        Track tempoTrack = sequence.createTrack();
        addTextMeta(tempoTrack, META_TRACK_NAME, "Note2MIDI Tempo", 0);
        addTempo(tempoTrack, config.bpm);
        addMeta(tempoTrack, META_TIME_SIGNATURE, new byte[]{4, 2, 24, 8}, 0);

        InstrumentMapper mapper = new InstrumentMapper(config);
        TimelineQuantizer quantizer = new TimelineQuantizer(config);
        List<ScheduledNote> scheduled = new ArrayList<>();
        int skipped = 0;
        long order = 0L;
        for (CapturedNote captured : capturedNotes) {
            MappedNote mapped = mapper.map(captured);
            if (mapped == null) {
                skipped++;
                continue;
            }
            scheduled.add(new ScheduledNote(mapped,
                    quantizer.midiTick(captured.getClientTick(), startClientTick), order++));
        }
        scheduled.sort(Comparator.comparingLong(ScheduledNote::tick).thenComparingLong(ScheduledNote::order));

        Map<String, List<ScheduledNote>> tracks = new LinkedHashMap<>();
        for (ScheduledNote note : scheduled) {
            tracks.computeIfAbsent(note.note().getInstrument(), ignored -> new ArrayList<>()).add(note);
        }

        long noteLength = quantizer.noteLengthMidiTicks();
        long lastTick = 0L;
        for (Map.Entry<String, List<ScheduledNote>> entry : tracks.entrySet()) {
            Track track = sequence.createTrack();
            List<ScheduledNote> notes = entry.getValue();
            MappedNote first = notes.get(0).note();
            addTextMeta(track, META_TRACK_NAME, entry.getKey(), 0);
            addShort(track, ShortMessage.PROGRAM_CHANGE, first.getChannel(), first.getProgram(), 0, 0);
            for (ScheduledNote scheduledNote : notes) {
                MappedNote note = scheduledNote.note();
                long onTick = scheduledNote.tick();
                long offTick = onTick + noteLength;
                addShort(track, ShortMessage.NOTE_ON, note.getChannel(), note.getMidiNote(), note.getVelocity(), onTick);
                addShort(track, ShortMessage.NOTE_OFF, note.getChannel(), note.getMidiNote(), 0, offTick);
                lastTick = Math.max(lastTick, offTick);
            }
            addEndOfTrack(track, lastTick + 1);
        }
        addEndOfTrack(tempoTrack, lastTick + 1);

        try {
            int bytes = MidiSystem.write(sequence, 1, file.toFile());
            if (bytes <= 0) throw new IOException("JDK MIDI writer does not support Format 1");
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(file);
            throw ex;
        }

        double seconds = lastTick * 60.0 / (config.bpm * config.ppq);
        return new MidiExportResult(file.toAbsolutePath(), scheduled.size(), tracks.size() + 1,
                skipped, lastTick, seconds);
    }

    private static void addTempo(Track track, int bpm) throws InvalidMidiDataException {
        int microsPerQuarter = 60_000_000 / bpm;
        addMeta(track, META_TEMPO, new byte[]{
                (byte) (microsPerQuarter >>> 16),
                (byte) (microsPerQuarter >>> 8),
                (byte) microsPerQuarter
        }, 0);
    }

    private static void addTextMeta(Track track, int type, String text, long tick) throws InvalidMidiDataException {
        addMeta(track, type, text.getBytes(StandardCharsets.UTF_8), tick);
    }

    private static void addEndOfTrack(Track track, long tick) throws InvalidMidiDataException {
        addMeta(track, META_END_OF_TRACK, new byte[0], tick);
    }

    private static void addMeta(Track track, int type, byte[] data, long tick) throws InvalidMidiDataException {
        MetaMessage message = new MetaMessage();
        message.setMessage(type, data, data.length);
        track.add(new MidiEvent(message, tick));
    }

    private static void addShort(Track track, int command, int channel, int data1, int data2, long tick)
            throws InvalidMidiDataException {
        ShortMessage message = new ShortMessage();
        message.setMessage(command, channel, data1, data2);
        track.add(new MidiEvent(message, tick));
    }

    private record ScheduledNote(MappedNote note, long tick, long order) {
    }
}
