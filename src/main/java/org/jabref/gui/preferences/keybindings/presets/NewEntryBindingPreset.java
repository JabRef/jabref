package org.jabref.gui.preferences.keybindings.presets;

import java.util.HashMap;
import java.util.Map;

import org.jabref.gui.keyboard.KeyBinding;

public class NewEntryBindingPreset implements KeyBindingPreset {

    private static final Map<KeyBinding, String> KEY_BINDINGS = new HashMap<>();

    static {
        // Clear conflicting default presets
        KEY_BINDINGS.put(KeyBinding.PULL_CHANGES_FROM_SHARED_DATABASE, "");

        // Add new entry presets
        KEY_BINDINGS.put(KeyBinding.NEW_ARTICLE,"Ctrl+shift+A");
        KEY_BINDINGS.put(KeyBinding.NEW_BOOK,"Ctrl+shift+B");
        KEY_BINDINGS.put(KeyBinding.NEW_ENTRY,"Ctrl+N");
        KEY_BINDINGS.put(KeyBinding.NEW_ENTRY_FROM_PLAIN_TEXT,"Ctrl+shift+N");
        KEY_BINDINGS.put(KeyBinding.NEW_INBOOK,"Ctrl+shift+I");
        KEY_BINDINGS.put(KeyBinding.NEW_INPROCEEDINGS,"Ctrl+shift+C");
        KEY_BINDINGS.put(KeyBinding.NEW_MASTERSTHESIS,"Ctrl+shift+M");
        KEY_BINDINGS.put(KeyBinding.NEW_PHDTHESIS,"Ctrl+shift+T");
        KEY_BINDINGS.put(KeyBinding.NEW_PROCEEDINGS,"Ctrl+shift+P");
        KEY_BINDINGS.put(KeyBinding.NEW_TECHREPORT,"Ctrl+shift+R");
        KEY_BINDINGS.put(KeyBinding.NEW_UNPUBLISHED,"Ctrl+shift+U");
    }

    @Override
    public String getName() {
        return "New Entries";
    }

    @Override
    public Map<KeyBinding, String> getKeyBindings() {
        return KEY_BINDINGS;
    }
}
