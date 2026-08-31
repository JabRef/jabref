package org.jabref.gui.keyboard;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
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
}
