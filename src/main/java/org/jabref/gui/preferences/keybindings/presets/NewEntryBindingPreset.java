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
        keyBindings.put(KeyBinding.NEW_ARTICLE, "Ctrl+shift+A");
        keyBindings.put(KeyBinding.NEW_BOOK, "Ctrl+shift+B");
        keyBindings.put(KeyBinding.NEW_ENTRY, "Ctrl+N");
        keyBindings.put(KeyBinding.NEW_ENTRY_FROM_PLAIN_TEXT, "Ctrl+shift+N");
        keyBindings.put(KeyBinding.NEW_INBOOK, "Ctrl+shift+I");
        keyBindings.put(KeyBinding.NEW_INPROCEEDINGS, "Ctrl+shift+C");
        keyBindings.put(KeyBinding.NEW_MASTERSTHESIS, "Ctrl+shift+M");
        keyBindings.put(KeyBinding.NEW_PHDTHESIS, "Ctrl+shift+T");
        keyBindings.put(KeyBinding.NEW_PROCEEDINGS, "Ctrl+shift+P");
        keyBindings.put(KeyBinding.NEW_TECHREPORT, "Ctrl+shift+R");
        keyBindings.put(KeyBinding.NEW_UNPUBLISHED, "Ctrl+shift+U");

        return keyBindings;
    }
}
