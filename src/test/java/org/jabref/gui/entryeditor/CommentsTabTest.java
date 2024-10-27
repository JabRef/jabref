package org.jabref.gui.entryeditor;

import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GUITest
@ExtendWith(ApplicationExtension.class)
class CommentsTabTest {

    private final String ownerName = "user1";

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
    private GuiPreferences preferences;
    @Mock
    private ThemeManager themeManager;
    @Mock
    private TaskExecutor taskExecutor;
    @Mock
    private JournalAbbreviationRepository journalAbbreviationRepository;
    @Mock
    private OwnerPreferences ownerPreferences;

    @Mock
    private EntryEditorPreferences entryEditorPreferences;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(preferences.getOwnerPreferences()).thenReturn(ownerPreferences);
        when(ownerPreferences.getDefaultOwner()).thenReturn(ownerName);
        when(preferences.getEntryEditorPreferences()).thenReturn(entryEditorPreferences);
        when(entryEditorPreferences.shouldShowUserCommentsFields()).thenReturn(true);
        when(databaseContext.getMode()).thenReturn(BibDatabaseMode.BIBLATEX);
        BibEntryType entryTypeMock = mock(BibEntryType.class);
        when(entryTypesManager.enrich(any(), any())).thenReturn(Optional.of(entryTypeMock));

        commentsTab = new CommentsTab(
                preferences,
                databaseContext,
                suggestionProviders,
                undoManager,
                mock(UndoAction.class),
                mock(RedoAction.class),
                dialogService,
                themeManager,
                taskExecutor,
                journalAbbreviationRepository,
                OptionalObjectProperty.empty()
        );
    }

    @Test
    void emptyCommentShownIfGloballyEnabled() {
        final UserSpecificCommentField ownerComment = new UserSpecificCommentField(ownerName);
        when(entryEditorPreferences.shouldShowUserCommentsFields()).thenReturn(true);

        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.COMMENT, "Standard comment text");

        SequencedSet<Field> fields = commentsTab.determineFieldsToShow(entry);

        assertEquals(Set.of(StandardField.COMMENT, ownerComment), fields);
    }

    @Test
    void emptyCommentFieldNotShownIfGloballyDisabled() {
        when(entryEditorPreferences.shouldShowUserCommentsFields()).thenReturn(false);

        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.COMMENT, "Standard comment text");

        SequencedSet<Field> fields = commentsTab.determineFieldsToShow(entry);

        assertEquals(Set.of(StandardField.COMMENT), fields);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void commentFieldShownIfContainsText(boolean shouldShowUserCommentsFields) {
        final UserSpecificCommentField ownerComment = new UserSpecificCommentField(ownerName);
        when(entryEditorPreferences.shouldShowUserCommentsFields()).thenReturn(shouldShowUserCommentsFields);

        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.COMMENT, "Standard comment text")
                .withField(ownerComment, "User-specific comment text");

        SequencedSet<Field> fields = commentsTab.determineFieldsToShow(entry);

        assertEquals(Set.of(StandardField.COMMENT, ownerComment), fields);
    }

    @Test
    void determineFieldsToShowWorksForMultipleUsers() {
        final UserSpecificCommentField ownerComment = new UserSpecificCommentField(ownerName);
        final UserSpecificCommentField otherUsersComment = new UserSpecificCommentField("other-user-id");

        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.COMMENT, "Standard comment text")
                .withField(ownerComment, "User-specific comment text")
                .withField(otherUsersComment, "other-user-id comment text");

        SequencedSet<Field> fields = commentsTab.determineFieldsToShow(entry);

        assertEquals(Set.of(StandardField.COMMENT, ownerComment, otherUsersComment), fields);
    }

    @Test
    void differentiateCaseInUserName() {
        UserSpecificCommentField field1 = new UserSpecificCommentField("USER");
        UserSpecificCommentField field2 = new UserSpecificCommentField("user");
        assertNotEquals(field1, field2, "Two UserSpecificCommentField instances with usernames that differ only by case should be considered different");
    }
}
