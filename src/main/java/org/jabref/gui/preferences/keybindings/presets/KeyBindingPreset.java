package org.jabref.gui.preferences.keybindings.presets;

import java.util.Map;

import org.jabref.gui.keyboard.KeyBinding;

public interface KeyBindingPreset {
    String getName();

    Map<KeyBinding, String> getKeyBindings();
}
