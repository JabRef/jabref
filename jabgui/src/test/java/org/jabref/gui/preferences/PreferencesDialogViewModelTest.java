package org.jabref.gui.preferences;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class PreferencesDialogViewModelTest {

    @Test
    void skipsTabIfRequiredClassCannotBeLoaded() {
        Optional<PreferencesTab> tab = PreferencesDialogViewModel.createTab(() -> {
            throw new NoClassDefFoundError("org/jabref/logic/util/LocalizedNumbersUtils");
        });

        assertEquals(Optional.empty(), tab);
    }

    @Test
    void skipsTabIfRequiredClassFailureIsWrapped() {
        Optional<PreferencesTab> tab = PreferencesDialogViewModel.createTab(() -> {
            throw new RuntimeException(new NoClassDefFoundError("org/jabref/logic/util/LocalizedNumbersUtils"));
        });

        assertEquals(Optional.empty(), tab);
    }

    @Test
    void propagatesUnrelatedInitializationFailure() {
        assertThrows(IllegalArgumentException.class, () -> PreferencesDialogViewModel.<PreferencesTab>createTab(() -> {
            throw new IllegalArgumentException("Unrelated failure");
        }));
    }
}
