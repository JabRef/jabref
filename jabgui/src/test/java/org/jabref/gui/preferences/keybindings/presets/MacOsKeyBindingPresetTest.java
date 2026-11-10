package org.jabref.gui.preferences.keybindings.presets;

import java.util.Map;

import org.jabref.gui.keyboard.KeyBinding;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class MacOsKeyBindingPresetTest {

    @Test
        // [utest->req~macos.text-editing-shortcuts~1]
    void presetContainsMacOsTextEditingShortcuts() {
        assertEquals(Map.of(
                KeyBinding.EDITOR_DELETE, "ctrl+D",
                KeyBinding.EDITOR_BACKWARD, "ctrl+B",
                KeyBinding.EDITOR_FORWARD, "ctrl+F",
                KeyBinding.EDITOR_BEGINNING, "ctrl+A",
                KeyBinding.EDITOR_END, "ctrl+E",
                KeyBinding.EDITOR_UP, "ctrl+P",
                KeyBinding.EDITOR_DOWN, "ctrl+N",
                KeyBinding.EDITOR_KILL_LINE, "ctrl+K",
                KeyBinding.EDITOR_INSERT_LINE_AFTER, "ctrl+O",
                KeyBinding.EDITOR_TRANSPOSE_CHARACTERS, "ctrl+T"),
                new MacOsKeyBindingPreset().getKeyBindings());
    }
}
