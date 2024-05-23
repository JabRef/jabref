package org.jabref.gui.keyboard;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyBindingRepositoryTest {
    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                // Correctly mapped
                Arguments.of(
                        List.of(KeyBinding.ABBREVIATE, KeyBinding.NEW_TECHREPORT, KeyBinding.PASTE),
                        List.of("ctrl+1", "alt+2", "shift+3")
                ),

                // Defaults on faulty data
                Arguments.of(
                        List.of(KeyBinding.ABBREVIATE, KeyBinding.NEW_TECHREPORT, KeyBinding.PASTE),
                        List.of(KeyBinding.ABBREVIATE.getDefaultKeyBinding(), KeyBinding.NEW_TECHREPORT.getDefaultKeyBinding())
                ));
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void parseStringLists(List<KeyBinding> keybindings, List<String> bindings) {
        List<String> bindNames = keybindings.stream().map(KeyBinding::getConstant).toList();
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository(bindNames, bindings);

        assertEquals(keyBindingRepository.get(bindNames.getFirst()), bindings.getFirst());
        assertEquals(keyBindingRepository.get(bindNames.get(1)), bindings.get(1));
    }
}
