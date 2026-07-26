package xyz.wagyourtail.jsmacros.client.note2midi;

import xyz.wagyourtail.jsmacros.client.JsMacros;

public final class Note2MidiRecorder {
    private static MidiRecorderController controller;

    private Note2MidiRecorder() {
    }

    public static synchronized void initialize() {
        if (controller != null) return;
        controller = new MidiRecorderController(
                JsMacros.core.config.configFolder.toPath().resolve("note2midi-recorder.json"));
    }

    public static MidiRecorderController controller() {
        if (controller == null) throw new IllegalStateException("Note2MIDI has not initialized");
        return controller;
    }
}
