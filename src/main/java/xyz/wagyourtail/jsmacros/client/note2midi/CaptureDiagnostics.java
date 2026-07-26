package xyz.wagyourtail.jsmacros.client.note2midi;

import java.util.concurrent.atomic.AtomicLong;

public final class CaptureDiagnostics {
    private final AtomicLong nonNoteSounds = new AtomicLong();
    private final AtomicLong distanceFiltered = new AtomicLong();
    private final AtomicLong missingEntities = new AtomicLong();
    private final AtomicLong invalidEvents = new AtomicLong();

    public void reset() {
        nonNoteSounds.set(0);
        distanceFiltered.set(0);
        missingEntities.set(0);
        invalidEvents.set(0);
    }

    public void incrementNonNoteSounds() { nonNoteSounds.incrementAndGet(); }
    public void incrementDistanceFiltered() { distanceFiltered.incrementAndGet(); }
    public void incrementMissingEntities() { missingEntities.incrementAndGet(); }
    public void incrementInvalidEvents() { invalidEvents.incrementAndGet(); }
    public long getNonNoteSounds() { return nonNoteSounds.get(); }
    public long getDistanceFiltered() { return distanceFiltered.get(); }
    public long getMissingEntities() { return missingEntities.get(); }
    public long getInvalidEvents() { return invalidEvents.get(); }

    public long skippedDuringCapture() {
        return distanceFiltered.get() + missingEntities.get() + invalidEvents.get();
    }
}
