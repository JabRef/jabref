package org.jabref.gui.preferences.general;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.edit.CopyToPreferences;
import org.jabref.gui.preferences.DonationPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.Theme;
import org.jabref.http.manager.HttpServerManager;
import org.jabref.languageserver.controller.LanguageServerController;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GeneralTabViewModelTest {

    private DialogService dialogService;
    private GeneralTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getWorkspacePreferences()).thenReturn(new WorkspacePreferences(Language.ENGLISH, false, 12, Theme.system(), false, true, true, true, true, java.util.List.of()));
        when(preferences.getLibraryPreferences()).thenReturn(LibraryPreferences.getDefault());
        when(preferences.getFilePreferences()).thenReturn(FilePreferences.getDefault());
        when(preferences.getSearchPreferences()).thenReturn(SearchPreferences.getDefault());
        when(preferences.getRemotePreferences()).thenReturn(RemotePreferences.getDefault());
        when(preferences.getCopyToPreferences()).thenReturn(CopyToPreferences.getDefault());
        when(preferences.getDonationPreferences()).thenReturn(DonationPreferences.getDefault());
        when(preferences.getSSLPreferences()).thenReturn(SSLPreferences.getDefault());
        when(preferences.shouldEnableMscKeywordDescriptions()).thenReturn(false);

        viewModel = new GeneralTabViewModel(
                dialogService,
                preferences,
                mock(HttpServerManager.class),
                mock(LanguageServerController.class),
                mock(UiMessageHandler.class),
                mock(RemoteListenerServerManager.class),
                mock(StateManager.class),
                mock(TaskExecutor.class));
    }

    @Test
    void mscPopupShowsWhenCheckboxTurnsOnButNotDuringInitialization() {
        viewModel.setValues();

        verifyNoInteractions(dialogService);

        when(dialogService.showConfirmationDialogAndWait(
                eq(Localization.lang("License agreement for MSC codes")),
                anyString(),
                eq(Localization.lang("Accept")),
                eq(Localization.lang("Decline"))))
                .thenReturn(false);

        viewModel.enableMscKeywordDescriptionsProperty().set(true);

        verify(dialogService).showConfirmationDialogAndWait(
                eq(Localization.lang("License agreement for MSC codes")),
                anyString(),
                eq(Localization.lang("Accept")),
                eq(Localization.lang("Decline")));
        assertFalse(viewModel.enableMscKeywordDescriptionsProperty().get());
    }
}
