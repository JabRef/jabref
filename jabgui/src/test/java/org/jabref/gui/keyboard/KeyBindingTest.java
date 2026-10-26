package org.jabref.gui.keyboard;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
// [utest->req~macos.text-editing-shortcuts~1]
class KeyBindingTest {

    @Test
    @EnabledOnOs(OS.MAC)
    void defaultKeyBindingUsesMacSpecificDefaultOnMacOs() {
        assertEquals("shortcut+alt+F", KeyBinding.LOOKUP_DOC_IDENTIFIER.getDefaultKeyBinding());
        assertEquals("shortcut+alt+G", KeyBinding.FOCUS_GROUP_LIST.getDefaultKeyBinding());
    }

    @Test
    @DisabledOnOs(OS.MAC)
    void defaultKeyBindingUsesCrossPlatformDefaultOnOtherPlatforms() {
        assertEquals("alt+F", KeyBinding.LOOKUP_DOC_IDENTIFIER.getDefaultKeyBinding());
        assertEquals("alt+s", KeyBinding.FOCUS_GROUP_LIST.getDefaultKeyBinding());
    }

    @Test
    void bindingsAffectedByOptionKeyCharacterInsertionHaveMacSpecificDefaults() {
        // On macOS, alt (option) plus a letter inserts a special character (e.g. ⌥F inserts ƒ),
        // so these bindings use a shortcut-based default there
        assertEquals("shortcut+alt+F", KeyBinding.LOOKUP_DOC_IDENTIFIER.getDefaultMacKeyBinding());
        assertEquals("shortcut+alt+G", KeyBinding.FOCUS_GROUP_LIST.getDefaultMacKeyBinding());
    }

    @Test
    void macSpecificDefaultsDoNotChangeOtherPlatforms() {
        assertEquals("alt+F", KeyBinding.LOOKUP_DOC_IDENTIFIER.getDefaultNonMacKeyBinding());
        assertEquals("alt+s", KeyBinding.FOCUS_GROUP_LIST.getDefaultNonMacKeyBinding());
    }

    @Test
    void bindingsWithoutMacSpecificDefaultAreIdenticalOnAllPlatforms() {
        assertEquals(KeyBinding.SEARCH.getDefaultNonMacKeyBinding(), KeyBinding.SEARCH.getDefaultMacKeyBinding());
    }

    @Test
    void macOsEditorDefaultsUseNativeControlAndCommandBindings() {
        assertEquals("ctrl+D", KeyBinding.EDITOR_DELETE.getDefaultMacKeyBinding());
        assertEquals("ctrl+B", KeyBinding.EDITOR_BACKWARD.getDefaultMacKeyBinding());
        assertEquals("ctrl+F", KeyBinding.EDITOR_FORWARD.getDefaultMacKeyBinding());
        assertEquals("alt+LEFT", KeyBinding.EDITOR_WORD_BACKWARD.getDefaultMacKeyBinding());
        assertEquals("alt+RIGHT", KeyBinding.EDITOR_WORD_FORWARD.getDefaultMacKeyBinding());
        assertEquals("ctrl+A", KeyBinding.EDITOR_BEGINNING.getDefaultMacKeyBinding());
        assertEquals("ctrl+E", KeyBinding.EDITOR_END.getDefaultMacKeyBinding());
        assertEquals("shortcut+UP", KeyBinding.EDITOR_BEGINNING_DOC.getDefaultMacKeyBinding());
        assertEquals("shortcut+DOWN", KeyBinding.EDITOR_END_DOC.getDefaultMacKeyBinding());
        assertEquals("ctrl+P", KeyBinding.EDITOR_UP.getDefaultMacKeyBinding());
        assertEquals("ctrl+N", KeyBinding.EDITOR_DOWN.getDefaultMacKeyBinding());
        assertEquals("ctrl+K", KeyBinding.EDITOR_KILL_LINE.getDefaultMacKeyBinding());
        assertEquals("alt+BACK_SPACE", KeyBinding.EDITOR_KILL_WORD_BACKWARD.getDefaultMacKeyBinding());
        assertEquals("ctrl+L", KeyBinding.EDITOR_CENTER_CARET.getDefaultMacKeyBinding());
        assertEquals("ctrl+O", KeyBinding.EDITOR_INSERT_LINE_AFTER.getDefaultMacKeyBinding());
        assertEquals("ctrl+T", KeyBinding.EDITOR_TRANSPOSE_CHARACTERS.getDefaultMacKeyBinding());
        assertEquals("", KeyBinding.EDITOR_CAPITALIZE.getDefaultMacKeyBinding());
        assertEquals("", KeyBinding.EDITOR_LOWERCASE.getDefaultMacKeyBinding());
        assertEquals("", KeyBinding.EDITOR_UPPERCASE.getDefaultMacKeyBinding());
    }

    @Test
    void macOsEditorDefaultsDoNotChangeNonMacOsBindings() {
        assertEquals("", KeyBinding.EDITOR_BEGINNING.getDefaultNonMacKeyBinding());
        assertEquals("", KeyBinding.EDITOR_TRANSPOSE_CHARACTERS.getDefaultNonMacKeyBinding());
    }
}
