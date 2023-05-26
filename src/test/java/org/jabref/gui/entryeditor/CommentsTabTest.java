package org.jabref.gui.entryeditor;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.Optional;
import java.util.Set;

import javax.swing.undo.UndoManager;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@GUITest
@ExtendWith(ApplicationExtension.class)
class CommentsTabTest {

    private CommentsTab commentsTab;

    @Mock
    private BibEntryTypesManager entryTypesManager;
    @Mock
    private BibDatabaseContext databaseContext;
    @Mock
    private SuggestionProviders suggestionProviders;
    @Mock
    private UndoManager undoManager;
    @Mock
    private DialogService dialogService;
    @Mock
    private PreferencesService preferences;
    @Mock
    private StateManager stateManager;
    @Mock
    private ThemeManager themeManager;
    @Mock
    private TaskExecutor taskExecutor;
    @Mock
    private JournalAbbreviationRepository journalAbbreviationRepository;
    @Mock
    private IndexingTaskManager indexingTaskManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        commentsTab = new CommentsTab(
                "TestName",
                databaseContext,
                suggestionProviders,
                undoManager,
                dialogService,
                preferences,
                stateManager,
                themeManager,
                indexingTaskManager,
                entryTypesManager,
                taskExecutor,
                journalAbbreviationRepository
        );
    }

    @Test
    void testDetermineFieldsToShowWithoutEntryType() {
        BibEntry entry = new BibEntry();

        // Add a standard comment and a user-specific comment to the entry
        Field standardComment = StandardField.COMMENT;
        UserSpecificCommentField userComment = new UserSpecificCommentField("TestName");

        entry.setField(standardComment, "Standard comment text");
        entry.setField(userComment, "User-specific comment text");

        // Run the method under test
        Set<Field> fields = commentsTab.determineFieldsToShow(entry);

        // Verify that the result is an empty set
        assertTrue(fields.isEmpty());
    }

    @Test
    void testDetermineFieldsToShow() {
        BibEntry entry = new BibEntry();
        EntryType entryType = StandardEntryType.Book;
        entry.setType(entryType);
        String username = "user1";

        // Mock dependencies
        when(databaseContext.getMode()).thenReturn(BibDatabaseMode.BIBLATEX);

        // Mocking BibEntryType
        BibEntryType entryTypeMock = mock(BibEntryType.class);
        when(entryTypesManager.enrich(any(), any())).thenReturn(Optional.of(entryTypeMock));

        // Add a standard comment and a user-specific comment to the entry
        Field standardComment = StandardField.COMMENT;
        UserSpecificCommentField userComment = new UserSpecificCommentField(username); // Use username

        entry.setField(standardComment, "Standard comment text");
        entry.setField(userComment, "User-specific comment text");

        // Run the method under test
        Set<Field> fields = commentsTab.determineFieldsToShow(entry);

        // Verify that both comments are in the resulting set
        assertTrue(fields.contains(standardComment));
        assertTrue(fields.contains(userComment));

        // Verify that user-specific comment is in the resulting set
        UserSpecificCommentField defaultCommentField = new UserSpecificCommentField(username);
        assertTrue(fields.contains(defaultCommentField));
    }

    @Test
    public void testDifferentiateCaseInUserName() {
        UserSpecificCommentField field1 = new UserSpecificCommentField("USER");
        UserSpecificCommentField field2 = new UserSpecificCommentField("user");

        assertNotEquals(field1, field2, "Two UserSpecificCommentField instances with usernames that differ only by case should be considered different");

        assertNotEquals(field1.hashCode(), field2.hashCode(),
                "Hash codes of two UserSpecificCommentField instances with usernames that differ only by case should be different");
    }
}
