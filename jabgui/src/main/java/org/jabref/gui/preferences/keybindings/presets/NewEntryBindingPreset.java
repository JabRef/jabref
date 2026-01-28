package org.jabref.gui.preferences.keybindings.presets;

import java.util.HashMap;
import java.util.Map;

import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

public class NewEntryBindingPreset implements KeyBindingPreset {

    @Override
    public String getName() {
        return Localization.lang("New entry by type");
    }

    @Override
    public Map<KeyBinding, String> getKeyBindings() {
        final Map<KeyBinding, String> keyBindings = new HashMap<>();

        // Clear conflicting default presets
        keyBindings.put(KeyBinding.PULL_CHANGES_FROM_SHARED_DATABASE, "");
        keyBindings.put(KeyBinding.COPY_PREVIEW, "");

        // Add new entry presets
        keyBindings.put(KeyBinding.NEW_ARTICLE, "shortcut+shift+A");
        keyBindings.put(KeyBinding.NEW_BOOK, "shortcut+shift+B");
        keyBindings.put(KeyBinding.ADD_ENTRY, "shortcut+N");
        keyBindings.put(KeyBinding.ADD_ENTRY_IDENTIFIER, "shortcut+alt+shift+N");
        keyBindings.put(KeyBinding.ADD_ENTRY_PLAINTEXT, "shortcut+shift+N");
        keyBindings.put(KeyBinding.NEW_INBOOK, "shortcut+shift+I");
        keyBindings.put(KeyBinding.NEW_INPROCEEDINGS, "shortcut+shift+C");
        keyBindings.put(KeyBinding.NEW_MASTERSTHESIS, "shortcut+shift+M");
        keyBindings.put(KeyBinding.NEW_PHDTHESIS, "shortcut+shift+T");
        keyBindings.put(KeyBinding.NEW_PROCEEDINGS, "shortcut+shift+P");
        keyBindings.put(KeyBinding.NEW_TECHREPORT, "shortcut+shift+R");
        keyBindings.put(KeyBinding.NEW_UNPUBLISHED, "shortcut+shift+U");

        return keyBindings;
    }
}
