MidiRecorder.start();
Time.sleep(10000);

const status = MidiRecorder.getStatus();
Chat.log(`Note2MIDI captured ${status.getCapturedNotes()} notes`);

MidiRecorder.stop();
