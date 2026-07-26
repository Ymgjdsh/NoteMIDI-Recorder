package xyz.wagyourtail.jsmacros.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import xyz.wagyourtail.jsmacros.client.note2midi.gui.Note2MidiConfigScreen;

public class ModMenuEntry implements ModMenuApi {
    private final Note2MidiScreen note2MidiScreenFactory = new Note2MidiScreen();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return note2MidiScreenFactory;
    }

    public static class Note2MidiScreen implements ConfigScreenFactory<Note2MidiConfigScreen> {
        @Override
        public Note2MidiConfigScreen create(Screen parent) {
            return new Note2MidiConfigScreen(parent);
        }
    }
}
