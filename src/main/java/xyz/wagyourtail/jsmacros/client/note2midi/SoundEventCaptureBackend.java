package xyz.wagyourtail.jsmacros.client.note2midi;

import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventSound;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.IEventListener;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;

public final class SoundEventCaptureBackend implements NoteCaptureBackend, IEventListener {
    private final MidiRecorderController controller;
    private final RegistrationGuard registration = new RegistrationGuard();

    public SoundEventCaptureBackend(MidiRecorderController controller) {
        this.controller = controller;
    }

    @Override
    public CaptureSource source() {
        return CaptureSource.SOUND_EVENT;
    }

    @Override
    public void start() {
        registration.start(() -> JsMacros.core.eventRegistry.addListener("Sound", this));
    }

    @Override
    public void stop() {
        registration.stop(() -> JsMacros.core.eventRegistry.removeListener("Sound", this));
    }

    @Override
    public boolean isRunning() {
        return registration.isRegistered();
    }

    public void ensureRegisteredAfterProfileReload() {
        if (registration.isRegistered() && !JsMacros.core.eventRegistry.getListeners("Sound").contains(this)) {
            JsMacros.core.eventRegistry.addListener("Sound", this);
        }
    }

    @Override
    public boolean joined() {
        return false;
    }

    @Override
    public EventContainer<?> trigger(BaseEvent event) {
        if (event instanceof EventSound) {
            EventSound sound = (EventSound) event;
            controller.capture(source(), sound.sound, sound.pitch, sound.volume,
                    sound.position.x, sound.position.y, sound.position.z);
        }
        return null;
    }
}
