package xyz.wagyourtail.jsmacros.client.note2midi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimelineQuantizerTest {
    @Test
    void usesAbsoluteDeltaWithoutAccumulation() {
        RecorderConfig config = new RecorderConfig();
        config.quantization = RecorderConfig.Quantization.OFF;
        TimelineQuantizer quantizer = new TimelineQuantizer(config);
        assertEquals(0, quantizer.midiTick(100, 100));
        assertEquals(48, quantizer.midiTick(101, 100));
        assertEquals(480, quantizer.midiTick(110, 100));
    }

    @Test
    void sameClientTickAlwaysProducesSameMidiTick() {
        RecorderConfig config = new RecorderConfig();
        config.quantization = RecorderConfig.Quantization.GAME_TICK_2;
        TimelineQuantizer quantizer = new TimelineQuantizer(config);
        assertEquals(quantizer.midiTick(109, 100), quantizer.midiTick(109, 100));
    }
}
