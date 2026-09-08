package org.jabref.gui.importer.actions;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddGroupImportEntriesActionTest {

    private final AddGroupImportEntriesAction action = new AddGroupImportEntriesAction();
    private final ParserResult parserResult = new ParserResult();
    private CliPreferences preferences;

    @BeforeEach
    void setUp() {
        preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getLibraryPreferences().shouldAddImportedEntries()).thenReturn(true);
        when(preferences.getLibraryPreferences().getAddImportedEntriesGroupName()).thenReturn("Imported entries");
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
    }

    /// The group is written into a library the user has not changed, and this runs before the tab
    /// is attached to it, so nothing else can report that the library differs from its file.
    @Test
    void addingTheGroupMarksTheLibraryAsChanged() {
        action.performAction(parserResult, mock(DialogService.class), preferences);

        assertTrue(parserResult.getChangedOnMigration());
    }

    /// A library that already has the group is left alone, and must not be reported as changed.
    @Test
    void aLibraryThatAlreadyHasTheGroupIsNotMarked() {
        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
        action.addImportedEntriesGroupIfNeeded(databaseContext, preferences);

        ParserResult reopened = new ParserResult();
        reopened.setMetaData(databaseContext.getMetaData());
        action.performAction(reopened, mock(DialogService.class), preferences);

        assertFalse(reopened.getChangedOnMigration());
    }

    /// Nothing is written, and nothing is reported, when the preference is off.
    @Test
    void nothingIsMarkedWhenTheFeatureIsOff() {
        when(preferences.getLibraryPreferences().shouldAddImportedEntries()).thenReturn(false);

        action.performAction(parserResult, mock(DialogService.class), preferences);

        assertFalse(parserResult.getChangedOnMigration());
    }
}
