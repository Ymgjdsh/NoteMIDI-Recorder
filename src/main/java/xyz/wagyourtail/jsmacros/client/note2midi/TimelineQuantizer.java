package xyz.wagyourtail.jsmacros.client.note2midi;

public final class TimelineQuantizer {
    private final RecorderConfig config;

    public TimelineQuantizer(RecorderConfig config) {
        this.config = config;
    }

    public long midiTick(long absoluteClientTick, long startClientTick) {
        long deltaGameTicks = Math.max(0L, absoluteClientTick - startClientTick);
        if (config.quantization == RecorderConfig.Quantization.GAME_TICK_2) {
            deltaGameTicks = Math.round(deltaGameTicks / 2.0) * 2L;
        }
        long raw = Math.round(deltaGameTicks * config.ppq * config.bpm / 1200.0);
        return quantizeMidiTick(raw);
    }

    public long noteLengthMidiTicks() {
        return Math.max(1L, Math.round(config.noteLengthGameTicks * config.ppq * config.bpm / 1200.0));
    }

    private long quantizeMidiTick(long raw) {
        long grid;
        switch (config.quantization) {
            case SIXTEENTH:
                grid = Math.max(1L, Math.round(config.ppq / 4.0));
                break;
            case EIGHTH:
                grid = Math.max(1L, Math.round(config.ppq / 2.0));
                break;
            case QUARTER:
                grid = config.ppq;
                break;
            case OFF:
            case GAME_TICK_1:
            case GAME_TICK_2:
            default:
                return raw;
        }
        return Math.round(raw / (double) grid) * grid;
    }
}
