package org.jabref.gui.welcome.quicksettings.viewmodel;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class MainFileDirectoryDialogViewModelTest {

    private MainFileDirectoryDialogViewModel viewModel;

    @BeforeEach
    void setUp() {
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        viewModel = new MainFileDirectoryDialogViewModel(preferences, mock(DialogService.class));
    }

    @Test
    void unsetMainFileDirectoryIsNotValid() {
        assertFalse(viewModel.pathValidProperty().get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankPathIsNotValid(String path) {
        viewModel.pathProperty().set(path);

        assertFalse(viewModel.pathValidProperty().get());
    }

    @Test
    void enteredPathIsValid() {
        viewModel.pathProperty().set("/home/user/papers");

        assertTrue(viewModel.pathValidProperty().get());
    }
}
