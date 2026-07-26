package xyz.wagyourtail.jsmacros.client.note2midi;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RegistrationGuard {
    private final AtomicBoolean registered = new AtomicBoolean();

    public boolean start(Runnable registration) {
        if (!registered.compareAndSet(false, true)) return false;
        try {
            registration.run();
            return true;
        } catch (RuntimeException ex) {
            registered.set(false);
            throw ex;
        }
    }

    public boolean stop(Runnable unregistration) {
        if (!registered.compareAndSet(true, false)) return false;
        unregistration.run();
        return true;
    }

    public boolean isRegistered() {
        return registered.get();
    }
}
