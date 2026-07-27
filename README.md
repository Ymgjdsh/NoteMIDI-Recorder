# Note2MIDI Recorder (JsMacros Backport)

Note2MIDI Recorder is a Fabric client mod for Minecraft 1.20.4. It is built from
the official JsMacros `backports/1.20.4` codebase (JsMacros 1.9.3) and adds a
passive note-block recorder with standard MIDI Format 1 export.

## Runtime versions

- Minecraft 1.20.4
- Fabric Loader 0.15.0 or newer
- Java 17
- JsMacros 1.9.x is included in the jar

This jar uses the `jsmacros` mod id and replaces the normal JsMacros jar. Do not
install an additional JsMacros jar at the same time. Mod Menu is optional.

## Install

1. Install Fabric Loader for Minecraft 1.20.4.
2. Put the release `*-fabric.jar` from `dist` in the instance `mods` folder.
3. Remove any other JsMacros jar from that folder.
4. Start Minecraft with Java 17.

The release jar embeds the Fabric API modules it needs. The `*-fabric-dev.jar`
is for development and must not be installed.

## Use

These are real client commands and are not sent to the server:

```text
/midirec start
/midirec stop
/midirec status
/midirec cancel
/midirec open
/midirec config
```

`start` creates a new session. `stop` unregisters the active capture backend
immediately and exports on the background exporter thread. The completion chat
message contains the absolute path, note count, track count, duration and
skipped count. `cancel` discards the current session. `open` opens the MIDI
directory. `config` opens the General, Mapping and Export tabs.

The default output directory is the `midi` subdirectory of the real JsMacros
macro folder. No working-directory path is hardcoded. Filenames include the
date, time, server and dimension, are sanitized for Windows, and never replace
an existing file.

## Capture modes

`Sound event` is the default. It registers one read-only listener on the
JsMacros `Sound` event and uses the event's sound id, volume, pitch and position.

`Strict packet` observes Yarn 1.20.4 `PlaySoundS2CPacket` and
`PlaySoundFromEntityS2CPacket` at the tail of the corresponding
`ClientPlayNetworkHandler` methods. Bundle packets naturally dispatch through
those methods. The mixin never cancels, replaces or edits a packet or sound.
Entity sound packets whose entity is unavailable are skipped and counted.

Only ids beginning with `minecraft:block.note_block.` enter the recorder queue.
Capture callbacks only validate and enqueue immutable data. Distance filtering,
HUD state and session storage run on the Minecraft client tick; MIDI file I/O
runs on the dedicated `Note2MIDI Exporter` thread.

## Timing and MIDI

The recorder owns a monotonic client tick counter, independent of `player.age`.
Every note stores the absolute client tick and `System.nanoTime()`. MIDI ticks
are calculated from the absolute delta from the session start:

```text
midiTick = round(deltaGameTicks * PPQ * BPM / 1200.0)
```

The defaults are 120 BPM, PPQ 480, two-game-tick quantization and a two-game-tick
note length. Notes from the same original client tick always receive the same
MIDI tick and stable sorting does not turn chords into arpeggios.

Quantization corrects small arrival jitter. It cannot reconstruct server intent
or perfectly remove network latency; the settings screen states this explicitly.

Exports use only `javax.sound.midi` and `MidiSystem.write(sequence, 1, file)`:

- Track 0: name, tempo, 4/4 time signature and end-of-track
- One remaining track per mapped instrument
- Track name and 0-based General MIDI Program Change at tick 0
- NOTE_ON and fixed-duration NOTE_OFF events
- Channel 9 fixed keys 36, 38 and 42 for basedrum, snare and hat
- Fixed velocity or clamped volume-derived velocity

All Minecraft 1.20.4 note-block instruments have defaults. Mob imitation and
unknown/custom instruments safely fall back or can be skipped. JSON and the
Mapping tab can change base note, program, channel and fixed percussion key.

## Configuration

The generated `note2midi-recorder.json` is stored in the JsMacros config folder.
It contains capture mode, distance mode and radius, fixed coordinates, BPM, PPQ,
quantization, note length, velocity settings, lifecycle behavior, HUD state and
the complete instrument mapping table.

Distance modes are off, recording-start position, follow player and fixed
position. The inclusive default radius is 64 blocks. Disconnect defaults to
stop and export. Dimension changes can continue, stop and export, or cancel.

## JsMacros API

The global `MidiRecorder` library is registered before scripts run:

```javascript
MidiRecorder.start();
Time.sleep(10000);
const status = MidiRecorder.getStatus();
Chat.log(`Captured ${status.getCapturedNotes()} notes`);
MidiRecorder.stop();
```

Available methods:

```text
MidiRecorder.start()
MidiRecorder.stop()
MidiRecorder.cancel()
MidiRecorder.getStatus()
MidiRecorder.getCapturedNotes()
MidiRecorder.export(path)
MidiRecorder.reloadConfig()
```

`export(path)` exports the retained buffer and also stops an active recording.
Relative paths resolve under the JsMacros macro folder. A path without a `.mid`
extension is treated as an output directory.

## Build and test

Use a Java 17 `JAVA_HOME`:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build
```

Unit tests cover pitch octaves, note-block id filtering, distance boundaries,
absolute quantization, same-tick chord export, filename sanitation and collision
handling, listener registration idempotence, MIDI Format 1 structure and JDK
MIDI round-trip reading.

## Known limitations

- Timing is the client-observed timeline; delayed or reordered server delivery
  cannot be perfectly repaired.
- Strict packet mode records network sound instructions, while Sound event mode
  follows client sound playback and can include locally produced note sounds.
- General MIDI has only 16 channels. If custom mappings assign different
  programs to the same melodic channel, program selection is inherently shared.
- Custom player-head sounds that do not use the required note-block sound-id
  prefix are intentionally ignored.

The JsMacros base remains MPL-2.0. See `LICENSE` for the upstream license.
