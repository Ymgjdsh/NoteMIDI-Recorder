package xyz.wagyourtail.jsmacros.client.note2midi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

public final class Note2MidiHud {
    private static final int WIDTH = 196;
    private static final int HEIGHT = 39;

    private Note2MidiHud() {
    }

    public static void render(DrawContext context, float tickDelta) {
        MidiRecorderController controller = Note2MidiRecorder.controller();
        RecorderConfig config = controller.getConfig();
        if (!config.hudEnabled || controller.getState() == MidiRecorderController.State.IDLE) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int x = 6;
        int y = 6;
        context.fill(x, y, x + WIDTH, y + HEIGHT, 0xb0000000);
        int color = controller.getState() == MidiRecorderController.State.RECORDING ? 0xffff4040 : 0xffffb020;
        context.fill(x + 7, y + 7, x + 13, y + 13, color);

        long totalSeconds = controller.getElapsedMillis() / 1000L;
        String first = String.format(Locale.ROOT, "%s %02d:%02d  Notes %d",
                controller.getState() == MidiRecorderController.State.RECORDING ? "REC" : controller.getState().name(),
                totalSeconds / 60, totalSeconds % 60, controller.getCapturedNoteCount());
        String second = "Last " + controller.getRecentInstrument() + "  MIDI "
                + (controller.getRecentMidiNote() < 0 ? "-" : controller.getRecentMidiNote());
        second = client.textRenderer.trimToWidth(second, WIDTH - 14);
        String third = "Q " + quantizationName(config.quantization) + "  Radius "
                + (config.distanceMode == RecorderConfig.DistanceMode.OFF ? "off" : Math.round(config.filterRadius));
        context.drawTextWithShadow(client.textRenderer, first, x + 18, y + 5, 0xffffffff);
        context.drawTextWithShadow(client.textRenderer, second, x + 7, y + 16, 0xffdddddd);
        context.drawTextWithShadow(client.textRenderer, third, x + 7, y + 27, 0xffaaaaaa);
    }

    private static String quantizationName(RecorderConfig.Quantization quantization) {
        return switch (quantization) {
            case OFF -> "Off";
            case GAME_TICK_1 -> "1 tick";
            case GAME_TICK_2 -> "2 ticks";
            case SIXTEENTH -> "1/16";
            case EIGHTH -> "1/8";
            case QUARTER -> "1/4";
        };
    }
}
