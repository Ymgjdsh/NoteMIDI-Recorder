package xyz.wagyourtail.jsmacros.client.api.library.impl;

import xyz.wagyourtail.jsmacros.client.note2midi.CapturedNote;
import xyz.wagyourtail.jsmacros.client.note2midi.Note2MidiRecorder;
import xyz.wagyourtail.jsmacros.client.note2midi.RecorderStatus;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.core.library.Library;

import java.util.List;

@Library("MidiRecorder")
public final class FMidiRecorder extends BaseLibrary {
    public boolean start() {
        return Note2MidiRecorder.controller().start();
    }

    public boolean stop() {
        return Note2MidiRecorder.controller().stopAndExport();
    }

    public boolean cancel() {
        return Note2MidiRecorder.controller().cancel();
    }

    public RecorderStatus getStatus() {
        return Note2MidiRecorder.controller().getStatus();
    }

    public List<CapturedNote> getCapturedNotes() {
        return Note2MidiRecorder.controller().getCapturedNotes();
    }

    public boolean export(String path) {
        return Note2MidiRecorder.controller().exportCaptured(path);
    }

    public boolean reloadConfig() {
        return Note2MidiRecorder.controller().reloadConfig();
    }
}
