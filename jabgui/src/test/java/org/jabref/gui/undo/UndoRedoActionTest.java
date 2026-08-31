package org.jabref.gui.undo;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.UndoableFieldChange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Pins that the two actions act on the journal of the library the user is looking at, and read
/// it when they run rather than when they were built. One instance of each serves every library
/// the session opens, so holding a journal would tie the menu to whichever library happened to
/// exist first.
class UndoRedoActionTest {

    private final HeadlessGuiUndoManager journalOfA = new HeadlessGuiUndoManager();
    private final HeadlessGuiUndoManager journalOfB = new HeadlessGuiUndoManager();
    private final OptionalObjectProperty<LibraryTab> activeTab = OptionalObjectProperty.empty();

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
        when(tabA.getUndoManager()).thenReturn(journalOfA);
        tabB = mock(LibraryTab.class);
        when(tabB.getUndoManager()).thenReturn(journalOfB);

        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeTabProperty()).thenReturn(activeTab);
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.ofNullable(new BibDatabaseContext()));

        undoAction = new UndoAction(() -> activeTab.get().orElseThrow(), mock(DialogService.class), stateManager);
        redoAction = new RedoAction(() -> activeTab.get().orElseThrow(), mock(DialogService.class), stateManager);
    }

    private UndoableFieldChange setAuthor(BibEntry entry, String value) {
        String before = entry.getField(StandardField.AUTHOR).orElse(null);
        entry.setField(StandardField.AUTHOR, value);
        return new UndoableFieldChange(entry, StandardField.AUTHOR, before, value);
    }

    @Test
    void undoReversesTheChangeInTheActiveLibraryOnly() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));
        journalOfB.addEdit(setAuthor(entryInB, "Meitner"));
        activeTab.set(Optional.of(tabA));

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

        activeTab.set(Optional.of(tabA));
        undoAction.execute();
        activeTab.set(Optional.of(tabB));
        undoAction.execute();

        assertEquals(Optional.of("Einstein"), entryInA.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Curie"), entryInB.getField(StandardField.AUTHOR));
    }

    @Test
    void redoReappliesTheChangeInTheActiveLibraryOnly() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));
        journalOfB.addEdit(setAuthor(entryInB, "Meitner"));
        journalOfA.undo();
        journalOfB.undo();
        activeTab.set(Optional.of(tabA));

        redoAction.execute();

        assertEquals(Optional.of("Bohr"), entryInA.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Curie"), entryInB.getField(StandardField.AUTHOR), "the other library was redone as well");
    }

    @Test
    void enablementTracksTheActiveLibraryRatherThanAnyLibrary() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));

        activeTab.set(Optional.of(tabA));
        assertTrue(undoAction.executableProperty().get());

        activeTab.set(Optional.of(tabB));
        assertFalse(undoAction.executableProperty().get(), "enabled over a library with an empty journal");
    }

    @Test
    void enablementIsOffWhileNoLibraryIsActive() {
        journalOfA.addEdit(setAuthor(entryInA, "Bohr"));

        assertFalse(undoAction.executableProperty().get());
        assertFalse(redoAction.executableProperty().get());
    }
}
