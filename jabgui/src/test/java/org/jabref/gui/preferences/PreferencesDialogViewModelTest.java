package org.jabref.gui.preferences;

import java.util.Optional;

import org.jabref.gui.preferences.ai.AiTab;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class PreferencesDialogViewModelTest {

    @Test
    void skipsAiTabIfRequiredClassCannotBeLoaded() {
        Optional<AiTab> aiTab = PreferencesDialogViewModel.createAiTab(() -> {
            throw new NoClassDefFoundError("org/jabref/logic/util/LocalizedNumbersUtils");
        });

        assertEquals(Optional.empty(), aiTab);
    }

    @Test
    void skipsAiTabIfRequiredClassFailureIsWrapped() {
        Optional<AiTab> aiTab = PreferencesDialogViewModel.createAiTab(() -> {
            throw new RuntimeException(new NoClassDefFoundError("org/jabref/logic/util/LocalizedNumbersUtils"));
        });

        assertEquals(Optional.empty(), aiTab);
    }

    @Test
    void propagatesUnrelatedInitializationFailure() {
        assertThrows(IllegalArgumentException.class, () -> PreferencesDialogViewModel.createAiTab(() -> {
            throw new IllegalArgumentException("Unrelated failure");
        }));
    }
}
