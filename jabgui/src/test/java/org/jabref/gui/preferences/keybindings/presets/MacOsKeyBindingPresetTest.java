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
        assertEquals(Map.ofEntries(
                        Map.entry(KeyBinding.EDITOR_DELETE, "ctrl+D"),
                        Map.entry(KeyBinding.EDITOR_BACKWARD, "ctrl+B"),
                        Map.entry(KeyBinding.EDITOR_FORWARD, "ctrl+F"),
                        Map.entry(KeyBinding.EDITOR_WORD_BACKWARD, ""),
                        Map.entry(KeyBinding.EDITOR_WORD_FORWARD, ""),
                        Map.entry(KeyBinding.EDITOR_BEGINNING_DOC, ""),
                        Map.entry(KeyBinding.EDITOR_END_DOC, ""),
                        Map.entry(KeyBinding.EDITOR_CAPITALIZE, ""),
                        Map.entry(KeyBinding.EDITOR_LOWERCASE, ""),
                        Map.entry(KeyBinding.EDITOR_UPPERCASE, ""),
                        Map.entry(KeyBinding.EDITOR_KILL_WORD, ""),
                        Map.entry(KeyBinding.EDITOR_KILL_WORD_BACKWARD, ""),
                        Map.entry(KeyBinding.EDITOR_BEGINNING, "ctrl+A"),
                        Map.entry(KeyBinding.EDITOR_END, "ctrl+E"),
                        Map.entry(KeyBinding.EDITOR_UP, "ctrl+P"),
                        Map.entry(KeyBinding.EDITOR_DOWN, "ctrl+N"),
                        Map.entry(KeyBinding.EDITOR_KILL_LINE, "ctrl+K"),
                        Map.entry(KeyBinding.EDITOR_CENTER_CARET, "ctrl+L"),
                        Map.entry(KeyBinding.EDITOR_INSERT_LINE_AFTER, "ctrl+O"),
                        Map.entry(KeyBinding.EDITOR_TRANSPOSE_CHARACTERS, "ctrl+T")),
                new MacOsKeyBindingPreset().getKeyBindings());
    }
}
