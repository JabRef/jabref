package org.jabref.gui.preferences.general;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemePreset;
import org.jabref.http.manager.HttpServerManager;
import org.jabref.languageserver.controller.LanguageServerController;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.remote.server.RemoteListenerServerManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralTabViewModelTest {

    @Test
    void validateSettingsRejectsMissingTheme(@TempDir Path tempDir) {
        GeneralTabViewModel viewModel = createViewModel(tempDir);
        viewModel.selectedThemeProperty().set(null);
        viewModel.selectedThemeColorSchemeProperty().set(ThemeColorScheme.FOLLOW_SYSTEM);

        assertFalse(viewModel.validateSettings());
    }

    @Test
    void validateSettingsRejectsMissingThemeColorScheme(@TempDir Path tempDir) {
        GeneralTabViewModel viewModel = createViewModel(tempDir);
        viewModel.selectedThemeProperty().set(ThemePreset.JABREF);
        viewModel.selectedThemeColorSchemeProperty().set(null);

        assertFalse(viewModel.validateSettings());
    }

    private GeneralTabViewModel createViewModel(Path tempDir) {
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getSSLPreferences().getTruststorePath()).thenReturn(tempDir.resolve("truststore"));

        return new GeneralTabViewModel(
                mock(DialogService.class),
                preferences,
                mock(HttpServerManager.class),
                mock(LanguageServerController.class),
                mock(UiMessageHandler.class),
                mock(RemoteListenerServerManager.class),
                mock(StateManager.class));
    }
}
