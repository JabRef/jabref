package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
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
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests for the Comments tab, driven through the same entrypoint the entry editor uses
/// ([EntryEditorTab#notifyAboutFocus]) instead of clicking through the real editor, so they
/// stay fast: no stage is shown and no editor ever really receives the keyboard focus.
@ExtendWith(ApplicationExtension.class)
class CommentsTabTest {

    private static final String DEFAULT_OWNER = "Test User";
    private static final UserSpecificCommentField OWN_COMMENT = new UserSpecificCommentField("test-user");
    private static final UserSpecificCommentField ANNAS_COMMENT = new UserSpecificCommentField("anna");
    private static final UserSpecificCommentField ZOES_COMMENT = new UserSpecificCommentField("zoe");

    private RecordingCommentsTab commentsTab;

    @Start
    public void onStart(Stage stage) {
        commentsTab = createTab(true);
    }

    /// The general comment is offered even when the entry has no comment at all.
    // [utest->req~entry-editor.comments-tab~1]
    @Test
    void generalCommentIsShownForEntryWithoutComments(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "A title");

        show(robot, entry);

        assertEquals(List.of(StandardField.COMMENT), shownFields());
        assertTrue(isEditable(StandardField.COMMENT));
    }

    /// The general comment comes first, all other comment fields follow sorted by name.
    // [utest->req~entry-editor.comments-tab~1]
    @Test
    void userCommentsAreSortedByName(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(ZOES_COMMENT, "zoe's comment")
                .withField(OWN_COMMENT, "my comment")
                .withField(ANNAS_COMMENT, "anna's comment")
                .withField(StandardField.COMMENT, "general comment");

        show(robot, entry);

        assertEquals(List.of(StandardField.COMMENT, ANNAS_COMMENT, OWN_COMMENT, ZOES_COMMENT), shownFields());
    }

    /// Only the general comment and the current user's own comment can be edited here.
    // [utest->req~entry-editor.comments-tab~1]
    @Test
    void otherUsersCommentsAreReadOnly(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(OWN_COMMENT, "my comment")
                .withField(ANNAS_COMMENT, "anna's comment");

        show(robot, entry);

        assertTrue(isEditable(StandardField.COMMENT));
        assertTrue(isEditable(OWN_COMMENT));
        assertFalse(isEditable(ANNAS_COMMENT));
    }

    /// Read-only is not only about typing: dropping an image onto another user's comment would copy
    /// the file into the library and insert a Markdown link into that user's field.
    // [utest->req~entry-editor.comments-tab~1]
    @Test
    void otherUsersCommentsRejectDroppedFiles(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(ANNAS_COMMENT, "anna's comment");
        show(robot, entry);
        TextInputControl readOnlyInput = textInputOf(commentsTab.editors.get(ANNAS_COMMENT).getNode()).orElseThrow();

        // The dragboard is never read: the editor must reject the drag before looking at its content.
        DragEvent dragOver = dragEvent(DragEvent.DRAG_OVER, readOnlyInput);
        DragEvent dragDropped = dragEvent(DragEvent.DRAG_DROPPED, readOnlyInput);
        robot.interact(() -> {
            Event.fireEvent(readOnlyInput, dragOver);
            Event.fireEvent(readOnlyInput, dragDropped);
        });

        assertFalse(dragOver.isAccepted());
        assertFalse(dragDropped.isDropCompleted());
        assertEquals("anna's comment", readOnlyInput.getText());
        assertEquals(Optional.of("anna's comment"), entry.getField(ANNAS_COMMENT));
    }

    /// While the own comment is unset, it is offered as a chip instead of an empty editor.
    // [utest->req~entry-editor.comments-tab~1]
    @Test
    void ownCommentIsOfferedAsChipWhileUnset(FxRobot robot) {
        show(robot, new BibEntry(StandardEntryType.Article));

        assertTrue(ownCommentChip().isPresent());
    }

    @Test
    void noChipIsShownForAnAlreadySetOwnComment(FxRobot robot) {
        show(robot, new BibEntry(StandardEntryType.Article).withField(OWN_COMMENT, "my comment"));

        assertEquals(Optional.empty(), ownCommentChip());
    }

    /// Without the user-specific comments preference, the own comment is not offered at all.
    @Test
    void noChipIsShownWhenUserCommentFieldsAreDisabled(FxRobot robot) {
        robot.interact(() -> commentsTab = createTab(false));

        show(robot, new BibEntry(StandardEntryType.Article));

        assertEquals(Optional.empty(), ownCommentChip());
    }

    /// Clicking the chip replaces it by an empty, focused editor for the own comment.
    // [utest->req~entry-editor.comments-tab~1]
    @Test
    void clickingTheChipShowsAndFocusesAnEmptyOwnCommentEditor(FxRobot robot) {
        show(robot, new BibEntry(StandardEntryType.Article));

        clickOwnCommentChip(robot);

        assertEquals(List.of(StandardField.COMMENT, OWN_COMMENT), shownFields());
        assertEquals(Optional.empty(), ownCommentChip());
        assertEquals(List.of(OWN_COMMENT), commentsTab.focusRequests);
    }

    /// The chip-added editor is still empty, so a rebuild triggered from outside must not drop it.
    // [utest->req~entry-editor.comments-tab.live-refresh~1]
    @Test
    void emptyOwnCommentEditorSurvivesAnExternalChange(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        show(robot, entry);
        clickOwnCommentChip(robot);

        robot.interact(() -> entry.setField(ANNAS_COMMENT, "from the source tab"));

        assertEquals(List.of(StandardField.COMMENT, ANNAS_COMMENT, OWN_COMMENT), shownFields());
    }

    /// Deleting the own comment's last character must not remove the editor being typed in.
    // [utest->req~entry-editor.comments-tab.live-refresh~1]
    @Test
    void ownCommentEditorStaysVisibleWhenItsContentIsCleared(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(OWN_COMMENT, "my comment");
        show(robot, entry);

        robot.interact(() -> entry.clearField(OWN_COMMENT));

        assertEquals(List.of(StandardField.COMMENT, OWN_COMMENT), shownFields());
    }

    /// The empty own comment editor is a decision about *this* entry: another entry starts over.
    // [utest->req~entry-editor.comments-tab~1]
    @Test
    void emptyOwnCommentEditorIsDroppedOnEntrySwitch(FxRobot robot) {
        BibEntry first = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "First");
        BibEntry second = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Second");
        show(robot, first);
        clickOwnCommentChip(robot);

        show(robot, second);
        assertEquals(List.of(StandardField.COMMENT), shownFields());

        show(robot, first);
        assertEquals(List.of(StandardField.COMMENT), shownFields());
        assertTrue(ownCommentChip().isPresent());
    }

    /// A comment field added outside this tab (e.g. via the source tab) shows up without re-selecting the entry.
    // [utest->req~entry-editor.comments-tab.live-refresh~1]
    @Test
    void commentFieldSetFromOutsideIsShown(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        show(robot, entry);
        assertEquals(List.of(StandardField.COMMENT), shownFields());

        robot.interact(() -> entry.setField(ANNAS_COMMENT, "from the source tab"));

        assertEquals(List.of(StandardField.COMMENT, ANNAS_COMMENT), shownFields());
    }

    /// A comment field removed outside this tab disappears without re-selecting the entry.
    // [utest->req~entry-editor.comments-tab.live-refresh~1]
    @Test
    void commentFieldClearedFromOutsideIsHidden(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(ANNAS_COMMENT, "from the source tab");
        show(robot, entry);
        assertEquals(List.of(StandardField.COMMENT, ANNAS_COMMENT), shownFields());

        robot.interact(() -> entry.clearField(ANNAS_COMMENT));

        assertEquals(List.of(StandardField.COMMENT), shownFields());
    }

    /// Changes to a *non-comment* field must not rebuild the tab's field list.
    // [utest->req~entry-editor.comments-tab.live-refresh~1]
    @Test
    void nonCommentFieldChangeKeepsShownFields(FxRobot robot) {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        show(robot, entry);

        robot.interact(() -> entry.setField(StandardField.TITLE, "A title"));

        assertEquals(List.of(StandardField.COMMENT), shownFields());
    }

    /// Field changes of an entry that is no longer shown must not affect this tab.
    // [utest->req~entry-editor.comments-tab.live-refresh~1]
    @Test
    void changesOfPreviousEntryAreIgnored(FxRobot robot) {
        BibEntry first = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "First");
        BibEntry second = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Second");
        show(robot, first);
        show(robot, second);

        robot.interact(() -> first.setField(ANNAS_COMMENT, "belongs to the first entry"));

        assertEquals(List.of(StandardField.COMMENT), shownFields());
    }

    /// The chip focuses its new editor deferred; when the tab was rebound to another entry in
    /// the meantime, that callback must not focus the other entry's own comment editor.
    @Test
    void deferredChipFocusIsSkippedWhenTheEntryChangedInTheMeantime(FxRobot robot) {
        BibEntry first = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "First");
        BibEntry second = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Second")
                .withField(OWN_COMMENT, "already written");
        show(robot, first);

        robot.interact(() -> {
            ownCommentChip().orElseThrow().fire();
            // The entry editor moves on to another entry before the deferred focus block runs.
            commentsTab.currentEntryProperty().set(second);
            commentsTab.notifyAboutFocus(second);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(), commentsTab.focusRequests);
        assertEquals(List.of(StandardField.COMMENT, OWN_COMMENT), shownFields());
    }

    private void show(FxRobot robot, BibEntry entry) {
        robot.interact(() -> {
            commentsTab.currentEntryProperty().set(entry);
            commentsTab.notifyAboutFocus(entry);
        });
    }

    private void clickOwnCommentChip(FxRobot robot) {
        robot.interact(() -> ownCommentChip().orElseThrow().fire());
        // The chip focuses its new editor in a deferred block
        WaitForAsyncUtils.waitForFxEvents();
    }

    /// A drag event without a dragboard: a handler that inspects the dragboard before checking
    /// whether the editor may be changed at all fails with a [NullPointerException].
    private static DragEvent dragEvent(EventType<DragEvent> eventType, Node target) {
        return new DragEvent(null, target, eventType, null, 0, 0, 0, 0,
                TransferMode.COPY, null, null, null);
    }

    private List<Field> shownFields() {
        return List.copyOf(commentsTab.getShownFields());
    }

    private Optional<Button> ownCommentChip() {
        return commentsTab.gridPane.getChildren().stream()
                                   .filter(Button.class::isInstance)
                                   .map(Button.class::cast)
                                   .filter(button -> button.getStyleClass().contains("all-fields-add-chip"))
                                   .findFirst();
    }

    private boolean isEditable(Field field) {
        return textInputOf(commentsTab.editors.get(field).getNode()).orElseThrow().isEditable();
    }

    private static Optional<TextInputControl> textInputOf(Node node) {
        if (node instanceof TextInputControl textInput) {
            return Optional.of(textInput);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Optional<TextInputControl> found = textInputOf(child);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static RecordingCommentsTab createTab(boolean showUserCommentsFields) {
        StateManager stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());

        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getOwnerPreferences().getDefaultOwner()).thenReturn(DEFAULT_OWNER);
        when(preferences.getEntryEditorPreferences().shouldShowUserCommentsFields()).thenReturn(showUserCommentsFields);

        return new RecordingCommentsTab(
                new CountingUndoManager(),
                mock(UndoAction.class),
                mock(RedoAction.class),
                preferences,
                mock(JournalAbbreviationRepository.class),
                stateManager,
                mock(PreviewPanel.class));
    }

    /// Records focus requests instead of relying on the real keyboard focus, which would need a
    /// shown stage and does not tell apart "not requested" from "request went nowhere".
    private static class RecordingCommentsTab extends CommentsTab {

        private final List<Field> focusRequests = new ArrayList<>();

        RecordingCommentsTab(UndoManager undoManager,
                             UndoAction undoAction,
                             RedoAction redoAction,
                             GuiPreferences preferences,
                             JournalAbbreviationRepository journalAbbreviationRepository,
                             StateManager stateManager,
                             PreviewPanel previewPanel) {
            super(undoManager, undoAction, redoAction, preferences, journalAbbreviationRepository, stateManager, previewPanel);
        }

        @Override
        public void requestFocus(Field field) {
            focusRequests.add(field);
            super.requestFocus(field);
        }
    }
}
