package xyz.wagyourtail.jsmacros.client.note2midi;

public interface NoteCaptureBackend {
    CaptureSource source();
    void start();
    void stop();
    boolean isRunning();
}
