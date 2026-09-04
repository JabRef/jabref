package org.jabref.gui.libraryproperties.general;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralPropertiesViewModelTest {

    @Test
    void leavesKeywordSeparatorUnsetWhenLibraryUsesGlobalFallback() {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("user");
        GeneralPropertiesViewModel viewModel = new GeneralPropertiesViewModel(databaseContext, mock(DialogService.class), preferences, mock(UndoManager.class));

        viewModel.setValues();
        viewModel.storeSettings();

        assertEquals("", viewModel.keywordSeparatorProperty().get());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getKeywordSeparator());
    }
}
