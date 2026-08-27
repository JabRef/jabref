package org.jabref.gui.welcome.quicksettings.viewmodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.preferences.GuiPreferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThemeDialogViewModelTest {

    @Test
    void customThemeRequiresExistingFile(@TempDir Path tempDir) throws IOException {
        ThemeDialogViewModel viewModel = createViewModel();
        viewModel.customThemeEnabledProperty().set(true);

        viewModel.customPathToThemeProperty().set("");
        assertFalse(viewModel.isValidConfiguration());

        viewModel.customPathToThemeProperty().set(tempDir.resolve("missing.css").toString());
        assertFalse(viewModel.isValidConfiguration());

        Path customTheme = Files.createFile(tempDir.resolve("custom.css"));
        viewModel.customPathToThemeProperty().set(customTheme.toString());
        assertTrue(viewModel.isValidConfiguration());
    }

    private ThemeDialogViewModel createViewModel() {
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getWorkspacePreferences()).thenReturn(WorkspacePreferences.getDefault());

        return new ThemeDialogViewModel(preferences, mock(DialogService.class));
    }
}
