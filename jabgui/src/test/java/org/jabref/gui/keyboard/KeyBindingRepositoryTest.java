package org.jabref.gui.keyboard;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.scene.input.KeyCombination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyBindingRepositoryTest {
    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                // Correctly mapped
                Arguments.of(
                        List.of(KeyBinding.MERGE_ENTRIES, KeyBinding.NEW_TECHREPORT, KeyBinding.PASTE),
                        List.of("shortcut+1", "alt+2", "shift+3")
                ),

                // Defaults on faulty data
                Arguments.of(
                        List.of(KeyBinding.MERGE_ENTRIES, KeyBinding.NEW_TECHREPORT, KeyBinding.PASTE),
                        List.of(KeyBinding.MERGE_ENTRIES.getDefaultKeyBinding(), KeyBinding.NEW_TECHREPORT.getDefaultKeyBinding())
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

    @Test
    void findsConflictingBinding() {
        KeyBindingRepository repo = new KeyBindingRepository(
                List.of(KeyBinding.MERGE_ENTRIES.getConstant(), KeyBinding.PASTE.getConstant()),
                List.of("alt+2", "shift+3"));

        Optional<KeyBinding> conflict = repo.findConflictingKeyBinding(
                KeyBinding.PASTE, KeyCombination.valueOf("alt+2"));

        assertEquals(Optional.of(KeyBinding.MERGE_ENTRIES), conflict);
    }

    @Test
    void ignoresBindingItself() {
        KeyBindingRepository repo = new KeyBindingRepository(
                List.of(KeyBinding.MERGE_ENTRIES.getConstant(), KeyBinding.PASTE.getConstant()),
                List.of("alt+2", "shift+3"));

        Optional<KeyBinding> conflict = repo.findConflictingKeyBinding(
                KeyBinding.MERGE_ENTRIES, KeyCombination.valueOf("alt+2"));

        assertEquals(Optional.empty(), conflict);
    }

    @Test
    void returnsEmptyForFreeCombination() {
        KeyBindingRepository repo = new KeyBindingRepository(
                List.of(KeyBinding.MERGE_ENTRIES.getConstant(), KeyBinding.PASTE.getConstant()),
                List.of("alt+2", "shift+3"));

        Optional<KeyBinding> conflict = repo.findConflictingKeyBinding(
                KeyBinding.PASTE, KeyCombination.valueOf("alt+8"));

        assertEquals(Optional.empty(), conflict);
    }
}
