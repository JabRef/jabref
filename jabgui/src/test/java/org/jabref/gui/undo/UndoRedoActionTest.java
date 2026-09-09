package org.jabref.gui.undo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoSuspension;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.ChangeSet;
import org.jabref.model.undo.UndoableFieldChange;
import org.jabref.model.undo.UndoableRemoveString;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Pins that the two actions act on the journal of the library the user is looking at, and read
/// it when they run rather than when they were built. One instance of each serves every library
/// the session opens, so holding a journal would tie the menu to whichever library happened to
/// exist first.
@NullMarked
class UndoRedoActionTest {

    private final HeadlessGuiUndoManager journalOfA = new HeadlessGuiUndoManager();
    private final HeadlessGuiUndoManager journalOfB = new HeadlessGuiUndoManager();
    /// Given distinct paths on purpose: `BibDatabaseContext#equals` compares content, so two empty
    /// libraries are equal, and stubbing by one would answer for the other.
    private final BibDatabaseContext libraryA = libraryAt("a.bib");
    private final BibDatabaseContext libraryB = libraryAt("b.bib");
    private final OptionalObjectProperty<LibraryTab> activeTab = OptionalObjectProperty.empty();
    private final OptionalObjectProperty<BibDatabaseContext> activeDatabase = OptionalObjectProperty.empty();

    private DialogService dialogService;
    private BibEntry entryInA;
    private BibEntry entryInB;
    private LibraryTab tabA;
    private LibraryTab tabB;
    private UndoAction undoAction;
    private RedoAction redoAction;

    @BeforeEach
    void setUp() {
        entryInA = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Einstein");
        entryInB = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Curie");

        tabA = mock(LibraryTab.class);
        when(tabA.getBibDatabaseContext()).thenReturn(libraryA);
        tabB = mock(LibraryTab.class);
        when(tabB.getBibDatabaseContext()).thenReturn(libraryB);

        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeTabProperty()).thenReturn(activeTab);
        when(stateManager.activeDatabaseProperty()).thenReturn(activeDatabase);
        when(stateManager.getUndoManager(libraryA)).thenReturn(journalOfA);
        when(stateManager.getUndoManager(libraryB)).thenReturn(journalOfB);

        dialogService = mock(DialogService.class);
        undoAction = new UndoAction(dialogService, stateManager);
        redoAction = new RedoAction(dialogService, stateManager);
    }

    @Test
    void undoReversesTheChangeInTheActiveLibraryOnly() {
        // [utest->req~logic.undo.journal-per-library~1]
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));
        journalOfB.addEdit(setAuthor(entryInB, "Meitner"));
        showLibrary(tabA, libraryA);

        undoAction.execute();

        assertEquals(Optional.of("Einstein"), entryInA.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Meitner"), entryInB.getField(StandardField.AUTHOR), "the other library was undone as well");
    }

    /// The reason the journal is resolved on every run: the same action instance serves whichever
    /// library is in front, and switching tabs has to switch what it undoes.
    @Test
    void undoFollowsTheActiveLibraryWhenTheUserSwitchesTabs() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));
        journalOfB.addEdit(setAuthor(entryInB, "Meitner"));

        showLibrary(tabA, libraryA);
        undoAction.execute();

        assertEquals(Optional.of("Einstein"), entryInA.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Meitner"), entryInB.getField(StandardField.AUTHOR), "undone in the library that was not in front");

        showLibrary(tabB, libraryB);
        undoAction.execute();

        assertEquals(Optional.of("Einstein"), entryInA.getField(StandardField.AUTHOR), "undone again in the library switched away from");
        assertEquals(Optional.of("Curie"), entryInB.getField(StandardField.AUTHOR));
    }

    @Test
    void redoReappliesTheChangeInTheActiveLibraryOnly() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));
        journalOfB.addEdit(setAuthor(entryInB, "Meitner"));
        journalOfA.undo();
        journalOfB.undo();

        showLibrary(tabA, libraryA);
        redoAction.execute();

        assertEquals(Optional.of("Bohr"), entryInA.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Curie"), entryInB.getField(StandardField.AUTHOR), "redone in the library that was not in front");

        showLibrary(tabB, libraryB);
        redoAction.execute();

        assertEquals(Optional.of("Bohr"), entryInA.getField(StandardField.AUTHOR), "redone again in the library switched away from");
        assertEquals(Optional.of("Meitner"), entryInB.getField(StandardField.AUTHOR));
    }

    @Test
    void theNotificationSaysWhatWasUndoneAndRedone() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));
        showLibrary(tabA, libraryA);

        undoAction.execute();
        verify(dialogService).notify("Undone: Change field Author");

        redoAction.execute();
        verify(dialogService).notify("Redone: Change field Author");
    }

    @Test
    void theNotificationNamesTheCommandWhenTheStepIsASet() {
        journalOfA.addEdit(Localization.lang("Replace string"), edit -> edit.addEdit(setAuthor(entryInA, "Bohr")));
        showLibrary(tabA, libraryA);

        undoAction.execute();

        verify(dialogService).notify("Undone: Replace string");
    }

    @Test
    void theNotificationSaysWhenPartOfTheStepCouldNotBeUndone() {
        BibtexString string = new BibtexString("name", "content");
        libraryA.getDatabase().addString(string);
        // Recorded without being performed, so undoing it puts back a string the library still
        // holds - the shape a change has when the library moved on underneath the journal.
        journalOfA.addEdit(new ChangeSet("Remove string", List.of(new UndoableRemoveString(libraryA.getDatabase(), string))));
        showLibrary(tabA, libraryA);

        undoAction.execute();

        verify(dialogService).notify("Undone: Remove string (some changes could not be applied)");
    }

    @Test
    void theNotificationNamesTheCommandHoldingTheLibrary() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));
        showLibrary(tabA, libraryA);

        try (UndoSuspension suspended = journalOfA.suspendUndo("Import entries")) {
            undoAction.execute();
            redoAction.execute();
        }

        verify(dialogService).notify("Cannot undo while Import entries is running");
        verify(dialogService).notify("Cannot redo while Import entries is running");
        assertEquals(Optional.of("Bohr"), entryInA.getField(StandardField.AUTHOR), "the undo ran anyway");
    }

    /// Enablement stays on while a command holds the library, so that pressing Ctrl+Z reaches the
    /// action and the user is told why nothing happened. A disabled menu item swallows its
    /// accelerator, which would make the keystroke do nothing at all.
    @Test
    void enablementStaysOnWhileACommandHoldsTheActiveLibrary() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));
        showLibrary(tabA, libraryA);

        try (UndoSuspension suspended = journalOfA.suspendUndo("Import entries")) {
            assertTrue(undoAction.executableProperty().get(), "the keystroke could not reach the action");
            undoAction.execute();
            verify(dialogService).notify("Cannot undo while Import entries is running");
            assertEquals(Optional.of("Bohr"), entryInA.getField(StandardField.AUTHOR), "the undo ran anyway");
        }
    }

    @Test
    void enablementTracksTheActiveLibraryRatherThanAnyLibrary() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));

        showLibrary(tabA, libraryA);
        assertTrue(undoAction.executableProperty().get());

        showLibrary(tabB, libraryB);
        assertFalse(undoAction.executableProperty().get(), "enabled over a library with an empty journal");
    }

    @Test
    void enablementIsOffWhileNoLibraryIsActive() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));

        assertFalse(undoAction.executableProperty().get());
        assertFalse(redoAction.executableProperty().get());
    }

    /// Enablement keeps this out of reach from the UI, but nothing about the class enforces that,
    /// and a no-op beats a NullPointerException.
    @Test
    void executingWithNoLibraryOpenDoesNothing() {
        assertDoesNotThrow(() -> undoAction.execute());
        assertDoesNotThrow(() -> redoAction.execute());
    }

    private static BibDatabaseContext libraryAt(String fileName) {
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(Path.of(fileName));
        return context;
    }

    private void showLibrary(LibraryTab tab, BibDatabaseContext context) {
        activeTab.set(Optional.of(tab));
        activeDatabase.set(Optional.of(context));
    }

    private UndoableFieldChange setAuthor(BibEntry entry, String value) {
        String before = entry.getField(StandardField.AUTHOR).orElse(null);
        entry.setField(StandardField.AUTHOR, value);
        return new UndoableFieldChange(entry, StandardField.AUTHOR, before, value);
    }
}
