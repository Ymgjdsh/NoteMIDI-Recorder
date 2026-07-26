package xyz.wagyourtail.jsmacros.client.note2midi;

public final class PacketCaptureBackend implements NoteCaptureBackend {
    private final RegistrationGuard registration = new RegistrationGuard();

    @Override
    public CaptureSource source() {
        return CaptureSource.STRICT_PACKET;
    }

    @Override
    public void start() {
        registration.start(() -> { });
    }

    @Override
    public void stop() {
        registration.stop(() -> { });
    }

    @Override
    public boolean isRunning() {
        return registration.isRegistered();
    }
}
