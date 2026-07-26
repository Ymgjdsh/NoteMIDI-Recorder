package xyz.wagyourtail.jsmacros.client.note2midi.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import xyz.wagyourtail.jsmacros.client.note2midi.InstrumentMapping;
import xyz.wagyourtail.jsmacros.client.note2midi.Note2MidiRecorder;
import xyz.wagyourtail.jsmacros.client.note2midi.RecorderConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public final class Note2MidiConfigScreen extends Screen {
    private enum Page { GENERAL, MAPPING, EXPORT }

    private final Screen parent;
    private final RecorderConfig config;
    private Page page = Page.GENERAL;
    private String selectedInstrument;

    public Note2MidiConfigScreen(Screen parent) {
        super(Text.literal("Note2MIDI Recorder"));
        this.parent = parent;
        this.config = Note2MidiRecorder.controller().getConfig();
        this.selectedInstrument = config.mappings.keySet().iterator().next();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(310, width - 12);
        int left = (width - contentWidth) / 2;
        int gap = 6;
        int columnWidth = (contentWidth - gap) / 2;

        addTab(Page.GENERAL, left, 28, (contentWidth - 4) / 3);
        addTab(Page.MAPPING, left + (contentWidth - 4) / 3 + 2, 28, (contentWidth - 4) / 3);
        addTab(Page.EXPORT, left + 2 * ((contentWidth - 4) / 3 + 2), 28, (contentWidth - 4) / 3);

        if (page == Page.GENERAL) {
            initGeneral(left, columnWidth, gap);
        } else if (page == Page.MAPPING) {
            initMapping(left, contentWidth, columnWidth, gap);
        } else {
            initExport(left, columnWidth, gap);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(width / 2 - 75, height - 27, 150, 20).build());
    }

    private void addTab(Page tab, int x, int y, int buttonWidth) {
        String name = switch (tab) {
            case GENERAL -> "General";
            case MAPPING -> "Mapping";
            case EXPORT -> "Export";
        };
        addDrawableChild(ButtonWidget.builder(Text.literal(page == tab ? "[" + name + "]" : name), button -> {
            page = tab;
            clearAndInit();
        }).dimensions(x, y, buttonWidth, 20).build());
    }

    private void initGeneral(int left, int columnWidth, int gap) {
        int right = left + columnWidth + gap;
        int y = 58;
        addEnumCycle(left, y, columnWidth, "Capture", RecorderConfig.CaptureMode.values(), config.captureMode,
                value -> config.captureMode = value);
        addEnumCycle(right, y, columnWidth, "Quantize", RecorderConfig.Quantization.values(), config.quantization,
                value -> config.quantization = value);
        y += 24;
        addEnumCycle(left, y, columnWidth, "Distance", RecorderConfig.DistanceMode.values(), config.distanceMode,
                value -> config.distanceMode = value);
        addDrawableChild(new DoubleConfigSlider(right, y, columnWidth, "Radius", 0, 256,
                config.filterRadius, value -> config.filterRadius = value));
        y += 24;
        addDrawableChild(new IntConfigSlider(left, y, columnWidth, "BPM", 20, 300,
                config.bpm, value -> config.bpm = value));
        addDrawableChild(new IntConfigSlider(right, y, columnWidth, "Note length (ticks)", 1, 20,
                config.noteLengthGameTicks, value -> config.noteLengthGameTicks = value));
    }

    private void initMapping(int left, int contentWidth, int columnWidth, int gap) {
        int right = left + columnWidth + gap;
        int y = 58;
        List<String> instruments = new ArrayList<>(config.mappings.keySet());
        addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values(instruments).initially(selectedInstrument)
                .build(left, y, contentWidth, 20, Text.literal("Instrument"), (button, value) -> {
                    selectedInstrument = value;
                    clearAndInit();
                }));
        InstrumentMapping mapping = config.mappings.get(selectedInstrument);
        y += 24;
        addDrawableChild(new IntConfigSlider(left, y, columnWidth, "Base note", 0, 127,
                mapping.baseMidiNote, value -> mapping.baseMidiNote = value));
        addDrawableChild(new IntConfigSlider(right, y, columnWidth, "GM program", 0, 127,
                mapping.program, value -> mapping.program = value));
        y += 24;
        addDrawableChild(new IntConfigSlider(left, y, columnWidth, "MIDI channel", 0, 15,
                mapping.channel, value -> mapping.channel = value));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(mapping.enabled)
                .build(right, y, columnWidth, 20, Text.literal("Enabled"),
                        (button, value) -> mapping.enabled = value));
        y += 24;
        if (mapping.percussionKey >= 0) {
            addDrawableChild(new IntConfigSlider(left, y, contentWidth, "GM percussion key", 0, 127,
                    mapping.percussionKey, value -> mapping.percussionKey = value));
        } else {
            ButtonWidget melodic = addDrawableChild(ButtonWidget.builder(Text.literal("Melodic instrument (no fixed key)"),
                    button -> { }).dimensions(left, y, contentWidth, 20).build());
            melodic.active = false;
        }
    }

    private void initExport(int left, int columnWidth, int gap) {
        int right = left + columnWidth + gap;
        int y = 58;
        addEnumCycle(left, y, columnWidth, "Velocity", RecorderConfig.VelocityMode.values(), config.velocityMode,
                value -> config.velocityMode = value);
        addDrawableChild(new IntConfigSlider(right, y, columnWidth, "Fixed velocity", 1, 127,
                config.fixedVelocity, value -> config.fixedVelocity = value));
        y += 24;
        addDrawableChild(CyclingButtonWidget.onOffBuilder(config.percussionPreservePitch)
                .build(left, y, columnWidth, 20, Text.literal("Percussion pitch"),
                        (button, value) -> config.percussionPreservePitch = value));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoExportOnDisconnect)
                .build(right, y, columnWidth, 20, Text.literal("Export on disconnect"),
                        (button, value) -> config.autoExportOnDisconnect = value));
        y += 24;
        addEnumCycle(left, y, columnWidth, "Dimension", RecorderConfig.DimensionChangeBehavior.values(),
                config.dimensionChangeBehavior, value -> config.dimensionChangeBehavior = value);
        addDrawableChild(CyclingButtonWidget.onOffBuilder(config.hudEnabled)
                .build(right, y, columnWidth, 20, Text.literal("Recording HUD"),
                        (button, value) -> config.hudEnabled = value));
    }

    private <T extends Enum<T>> void addEnumCycle(int x, int y, int buttonWidth, String label,
                                                   T[] values, T initial, java.util.function.Consumer<T> consumer) {
        addDrawableChild(CyclingButtonWidget.<T>builder(value -> Text.literal(pretty(value.name())))
                .values(values).initially(initial)
                .build(x, y, buttonWidth, 20, Text.literal(label), (button, value) -> consumer.accept(value)));
    }

    private static String pretty(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    @Override
    public void close() {
        config.validate();
        Note2MidiRecorder.controller().saveConfig();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xffffffff);
        if (page == Page.GENERAL) {
            context.drawCenteredTextWithShadow(textRenderer,
                    "Quantization corrects jitter; it cannot remove network latency.", width / 2,
                    Math.min(height - 42, 138), 0xffaaaaaa);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class IntConfigSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer consumer;

        private IntConfigSlider(int x, int y, int width, String label, int min, int max,
                                int current, IntConsumer consumer) {
            super(x, y, width, 20, Text.empty(), (current - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.consumer = consumer;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + current()));
        }

        @Override
        protected void applyValue() {
            consumer.accept(current());
        }
    }

    private static final class DoubleConfigSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer consumer;

        private DoubleConfigSlider(int x, int y, int width, String label, double min, double max,
                                   double current, DoubleConsumer consumer) {
            super(x, y, width, 20, Text.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.consumer = consumer;
            updateMessage();
        }

        private double current() {
            return min + value * (max - min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + Math.round(current())));
        }

        @Override
        protected void applyValue() {
            consumer.accept(current());
        }
    }
}
