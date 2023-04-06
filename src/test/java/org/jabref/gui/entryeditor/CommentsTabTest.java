package org.jabref.gui.entryeditor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.preferences.PreferencesService;
import org.jabref.logic.preferences.OwnerPreferences;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.undo.UndoManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentsTabTest {

    private CommentsTab commentsTab;

    @Test
    void testDetermineFieldsToShow() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        SuggestionProviders suggestionProviders = Mockito.mock(SuggestionProviders.class);
        UndoManager undoManager = Mockito.mock(UndoManager.class);
        DialogService dialogService = Mockito.mock(DialogService.class);
        PreferencesService preferences = Mockito.mock(PreferencesService.class);
        OwnerPreferences ownerPreferences = Mockito.mock(OwnerPreferences.class);
        Mockito.when(preferences.getOwnerPreferences()).thenReturn(ownerPreferences);
        Mockito.when(ownerPreferences.getDefaultOwner()).thenReturn("defaultOwner");
        StateManager stateManager = Mockito.mock(StateManager.class);
        ThemeManager themeManager = Mockito.mock(ThemeManager.class);
        BibEntryTypesManager entryTypesManager = Mockito.mock(BibEntryTypesManager.class);
        JournalAbbreviationRepository journalAbbreviationRepository = Mockito.mock(JournalAbbreviationRepository.class);

        commentsTab = new CommentsTab(
                preferences.getOwnerPreferences().getDefaultOwner(),
                databaseContext,
                suggestionProviders,
                undoManager,
                dialogService,
                preferences,
                stateManager,
                themeManager,
                null,
                entryTypesManager,
                null,
                journalAbbreviationRepository
        );

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.COMMENT, "This is a comment");
        entry.setField(new UnknownField("customcomment1"), "Custom comment 1");
        entry.setField(new UnknownField("customcomment2"), "Custom comment 2");

        Set<Field> expectedFields = new HashSet<>(Arrays.asList(StandardField.COMMENT, new UnknownField("customcomment1"), new UnknownField("customcomment2")));
        Set<Field> actualFields = commentsTab.determineFieldsToShow(entry);

        assertTrue(expectedFields.containsAll(actualFields) && actualFields.containsAll(expectedFields));
    }
}
