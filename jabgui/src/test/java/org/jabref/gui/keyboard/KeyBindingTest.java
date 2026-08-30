package org.jabref.gui.keyboard;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyBindingTest {

    private static Stream<Arguments> platformDefaults() {
        return Stream.of(
                // On macOS, alt (option) plus a letter inserts a special character (e.g. ⌥F inserts ƒ),
                // so these bindings use a shortcut-based default there
                Arguments.of("shortcut+alt+F", KeyBinding.LOOKUP_DOC_IDENTIFIER, true),
                Arguments.of("alt+F", KeyBinding.LOOKUP_DOC_IDENTIFIER, false),
                Arguments.of("shortcut+alt+G", KeyBinding.FOCUS_GROUP_LIST, true),
                Arguments.of("alt+s", KeyBinding.FOCUS_GROUP_LIST, false),
                // bindings without a macOS-specific default keep the same default on all platforms
                Arguments.of("shortcut+F", KeyBinding.SEARCH, true),
                Arguments.of("shortcut+F", KeyBinding.SEARCH, false)
        );
    }

    @ParameterizedTest
    @MethodSource("platformDefaults")
    void defaultKeyBindingDependsOnPlatform(String expectedBinding, KeyBinding keyBinding, boolean isMacOs) {
        assertEquals(expectedBinding, keyBinding.getDefaultKeyBinding(isMacOs));
    }
}
