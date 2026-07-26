package xyz.wagyourtail.jsmacros.fabric.client.note2midi;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;
import xyz.wagyourtail.jsmacros.client.note2midi.MidiRecorderController;
import xyz.wagyourtail.jsmacros.client.note2midi.Note2MidiHud;
import xyz.wagyourtail.jsmacros.client.note2midi.Note2MidiRecorder;
import xyz.wagyourtail.jsmacros.client.note2midi.gui.Note2MidiConfigScreen;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class Note2MidiFabric {
    private static boolean initialized;

    private Note2MidiFabric() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        MidiRecorderController controller = Note2MidiRecorder.controller();

        ClientTickEvents.END_CLIENT_TICK.register(client -> controller.onEndClientTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> controller.onDisconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> controller.shutdown());
        HudRenderCallback.EVENT.register(Note2MidiHud::render);
        registerCommands(controller);
    }

    private static void registerCommands(MidiRecorderController controller) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("midirec")
                        .then(literal("start").executes(context -> {
                            context.getSource().sendFeedback(Text.literal(controller.start()
                                    ? "Note2MIDI: recording" : "Note2MIDI: could not start"));
                            return 1;
                        }))
                        .then(literal("stop").executes(context -> {
                            context.getSource().sendFeedback(Text.literal(controller.stopAndExport()
                                    ? "Note2MIDI: exporting in background" : "Note2MIDI: not recording"));
                            return 1;
                        }))
                        .then(literal("status").executes(context -> {
                            var status = controller.getStatus();
                            context.getSource().sendFeedback(Text.literal("Note2MIDI: " + status.getState()
                                    + ", notes " + status.getCapturedNotes() + ", skipped " + status.getSkipped()));
                            return 1;
                        }))
                        .then(literal("cancel").executes(context -> {
                            context.getSource().sendFeedback(Text.literal(controller.cancel()
                                    ? "Note2MIDI: cancelled" : "Note2MIDI: export already running"));
                            return 1;
                        }))
                        .then(literal("open").executes(context -> {
                            controller.openOutputFolder();
                            return 1;
                        }))
                        .then(literal("config").executes(context -> {
                            context.getSource().getClient().setScreen(new Note2MidiConfigScreen(
                                    context.getSource().getClient().currentScreen));
                            return 1;
                        }))
        ));
    }
}
