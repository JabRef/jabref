package org.jabref.gui.walkthrough.declarative.sideeffect;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.gui.walkthrough.Walkthrough;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@NullMarked
class OpenPreferencesSideEffectTest {

    @Test
    void opensAndClosesPreferencesDialog() {
        DialogService dialogService = mock(DialogService.class);
        PreferencesDialogView preferencesDialog = mock(PreferencesDialogView.class);
        Walkthrough walkthrough = mock(Walkthrough.class);
        OpenPreferencesSideEffect sideEffect = new OpenPreferencesSideEffect(dialogService, preferencesDialog);

        assertTrue(sideEffect.forward(walkthrough));
        assertTrue(sideEffect.backward(walkthrough));

        verify(dialogService).showCustomDialog(preferencesDialog);
        verify(preferencesDialog).close();
    }
}
