package org.jabref.gui.preferences.keybindings.presets;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingCategory;

import org.jspecify.annotations.NullMarked;

/// Restores the native macOS text-editing shortcuts (e.g., <kbd>Control</kbd> + <kbd>A</kbd>),
/// for instance after another preset or the user changed the editor key bindings.
@NullMarked
public class MacOsKeyBindingPreset implements KeyBindingPreset {

    @Override
    public String getName() {
        return "macOS";
    }

    @Override
    // [impl->req~macos.text-editing-shortcuts~1]
    public Map<KeyBinding, String> getKeyBindings() {
        return Arrays.stream(KeyBinding.values())
                     .filter(keyBinding -> keyBinding.getCategory() == KeyBindingCategory.EDITOR)
                     .filter(keyBinding -> !keyBinding.getDefaultMacKeyBinding().isEmpty())
                     .collect(Collectors.toMap(keyBinding -> keyBinding, KeyBinding::getDefaultMacKeyBinding));
    }
}
