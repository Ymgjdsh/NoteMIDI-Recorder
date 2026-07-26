package xyz.wagyourtail.jsmacros.client.note2midi;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationGuardTest {
    @Test
    void repeatedStartAndStopNeverDuplicateRegistration() {
        RegistrationGuard guard = new RegistrationGuard();
        AtomicInteger registrations = new AtomicInteger();
        AtomicInteger removals = new AtomicInteger();

        assertTrue(guard.start(registrations::incrementAndGet));
        assertFalse(guard.start(registrations::incrementAndGet));
        assertEquals(1, registrations.get());
        assertTrue(guard.stop(removals::incrementAndGet));
        assertFalse(guard.stop(removals::incrementAndGet));
        assertEquals(1, removals.get());
        assertTrue(guard.start(registrations::incrementAndGet));
        assertEquals(2, registrations.get());
    }
}
