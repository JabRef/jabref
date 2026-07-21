package org.jabref.gui.preferences.entryeditor;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
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

class EntryEditorTabViewModelTest {

    private DialogService dialogService;
    private EntryEditorTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);

        viewModel = new EntryEditorTabViewModel(
                dialogService,
                EntryEditorPreferences.getDefault(),
                MrDlibPreferences.getDefault(),
                AbbreviationPreferences.getDefault(),
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
