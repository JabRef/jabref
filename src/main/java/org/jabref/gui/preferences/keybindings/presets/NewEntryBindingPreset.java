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
        keyBindings.put(KeyBinding.NEW_ARTICLE, "ctrl+shift+A");
        keyBindings.put(KeyBinding.NEW_BOOK, "ctrl+shift+B");
        keyBindings.put(KeyBinding.NEW_ENTRY, "ctrl+N");
        keyBindings.put(KeyBinding.NEW_ENTRY_FROM_PLAIN_TEXT, "ctrl+shift+N");
        keyBindings.put(KeyBinding.NEW_INBOOK, "ctrl+shift+I");
        keyBindings.put(KeyBinding.NEW_INPROCEEDINGS, "ctrl+shift+C");
        keyBindings.put(KeyBinding.NEW_MASTERSTHESIS, "ctrl+shift+M");
        keyBindings.put(KeyBinding.NEW_PHDTHESIS, "ctrl+shift+T");
        keyBindings.put(KeyBinding.NEW_PROCEEDINGS, "ctrl+shift+P");
        keyBindings.put(KeyBinding.NEW_TECHREPORT, "ctrl+shift+R");
        keyBindings.put(KeyBinding.NEW_UNPUBLISHED, "ctrl+shift+U");

        return keyBindings;
    }
}
