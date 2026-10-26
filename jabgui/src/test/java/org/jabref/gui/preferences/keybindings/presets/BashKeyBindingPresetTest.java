package org.jabref.gui.preferences.keybindings.presets;

import java.util.Map;

import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.os.OS;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@NullMarked
// [utest->req~macos.text-editing-shortcuts~1]
class BashKeyBindingPresetTest {

    @Test
    void presetUsesPlatformAppropriateEditorBindings() {
        Map<KeyBinding, String> bindings = new BashKeyBindingPreset().getKeyBindings();

        if (OS.OS_X) {
            assertEquals("ctrl+A", bindings.get(KeyBinding.EDITOR_BEGINNING));
            assertEquals("alt+BACK_SPACE", bindings.get(KeyBinding.EDITOR_KILL_WORD_BACKWARD));
            assertEquals("ctrl+T", bindings.get(KeyBinding.EDITOR_TRANSPOSE_CHARACTERS));
            assertFalse(bindings.containsKey(KeyBinding.EDITOR_CAPITALIZE));
            assertFalse(bindings.containsKey(KeyBinding.EDITOR_LOWERCASE));
            assertFalse(bindings.containsKey(KeyBinding.EDITOR_UPPERCASE));
        } else {
            assertEquals("shortcut+A", bindings.get(KeyBinding.EDITOR_BEGINNING));
            assertEquals("alt+DELETE", bindings.get(KeyBinding.EDITOR_KILL_WORD_BACKWARD));
            assertFalse(bindings.containsKey(KeyBinding.EDITOR_TRANSPOSE_CHARACTERS));
        }
    }
}
