package org.jabref.gui.entryeditor;

import java.util.List;

import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class CommentsTabTest {

    private static final UserSpecificCommentField OTHER_USERS_COMMENT = new UserSpecificCommentField("other-user");

    private CommentsTab commentsTab;

    @Start
    public void onStart(Stage stage) {
        StateManager stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());

        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getOwnerPreferences().getDefaultOwner()).thenReturn("test-user");
        when(preferences.getEntryEditorPreferences().shouldShowUserCommentsFields()).thenReturn(true);

        commentsTab = new CommentsTab(
                new CountingUndoManager(),
                mock(UndoAction.class),
                mock(RedoAction.class),
                preferences,
                mock(JournalAbbreviationRepository.class),
                stateManager,
                mock(PreviewPanel.class));
    }

    /// A comment field added outside this tab (e.g. via the source tab) shows up without re-selecting the entry.
    @Test
    void commentFieldSetFromOutsideIsShown(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        robot.interact(() -> {
            commentsTab.currentEntryProperty().set(entry);
            commentsTab.notifyAboutFocus(entry);
        });
        assertEquals(List.of(StandardField.COMMENT), List.copyOf(commentsTab.getShownFields()));

        robot.interact(() -> entry.setField(OTHER_USERS_COMMENT, "from the source tab"));

        assertEquals(List.of(StandardField.COMMENT, OTHER_USERS_COMMENT), List.copyOf(commentsTab.getShownFields()));
    }

    /// A comment field removed outside this tab disappears without re-selecting the entry.
    @Test
    void commentFieldClearedFromOutsideIsHidden(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(OTHER_USERS_COMMENT, "from the source tab");
        robot.interact(() -> {
            commentsTab.currentEntryProperty().set(entry);
            commentsTab.notifyAboutFocus(entry);
        });
        assertEquals(List.of(StandardField.COMMENT, OTHER_USERS_COMMENT), List.copyOf(commentsTab.getShownFields()));

        robot.interact(() -> entry.clearField(OTHER_USERS_COMMENT));

        assertEquals(List.of(StandardField.COMMENT), List.copyOf(commentsTab.getShownFields()));
    }

    /// Changes to a *non-comment* field must not rebuild the tab's field list.
    @Test
    void nonCommentFieldChangeKeepsShownFields(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        robot.interact(() -> {
            commentsTab.currentEntryProperty().set(entry);
            commentsTab.notifyAboutFocus(entry);
        });

        robot.interact(() -> entry.setField(StandardField.TITLE, "A title"));

        assertEquals(List.of(StandardField.COMMENT), List.copyOf(commentsTab.getShownFields()));
    }

    /// Field changes of an entry that is no longer shown must not affect this tab.
    @Test
    void changesOfPreviousEntryAreIgnored(FxRobot robot) {
        BibEntry first = new BibEntry(StandardEntryType.Article);
        BibEntry second = new BibEntry(StandardEntryType.Book);
        robot.interact(() -> {
            commentsTab.currentEntryProperty().set(first);
            commentsTab.notifyAboutFocus(first);
            commentsTab.currentEntryProperty().set(second);
            commentsTab.notifyAboutFocus(second);
        });

        robot.interact(() -> first.setField(OTHER_USERS_COMMENT, "belongs to the first entry"));

        List<Field> shown = List.copyOf(commentsTab.getShownFields());
        assertEquals(List.of(StandardField.COMMENT), shown);
    }
}
