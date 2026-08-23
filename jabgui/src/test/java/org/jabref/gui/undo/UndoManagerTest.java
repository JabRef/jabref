package org.jabref.gui.undo;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.ChangeSet;
import org.jabref.model.undo.UndoableFieldChange;
import org.jabref.model.undo.UndoableRemoveString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoManagerTest {

    private final UndoManager undoRedoManager = new UndoManager();
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        entry = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Einstein");
    }

    private UndoableFieldChange setAuthor(String value) {
        String before = entry.getField(StandardField.AUTHOR).orElse(null);
        entry.setField(StandardField.AUTHOR, value);
        return new UndoableFieldChange(entry, StandardField.AUTHOR, before, value);
    }

    @Test
    void aPushedChangeCanBeUndoneAndRedone() {
        undoRedoManager.addEdit(setAuthor("Bohr"));

        assertTrue(undoRedoManager.canUndo());
        undoRedoManager.undo();
        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());

        assertTrue(undoRedoManager.canRedo());
        undoRedoManager.redo();
        assertEquals("Bohr", entry.getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void undoingAnEmptyStackDoesNothing() {
        undoRedoManager.undo();

        assertFalse(undoRedoManager.canUndo());
        assertFalse(undoRedoManager.canRedo());
    }

    @Test
    void aNewChangeDiscardsTheRedoBranch() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.undo();
        assertTrue(undoRedoManager.canRedo());

        undoRedoManager.addEdit(setAuthor("Planck"));

        assertFalse(undoRedoManager.canRedo());
    }

    @Test
    void aRecordedBlockUndoesAsOneStep() {
        undoRedoManager.addEdit("edit", edit -> {
            edit.addEdit(entry.setField(StandardField.AUTHOR, "Bohr"));
            edit.addEdit(entry.setField(StandardField.TITLE, "Relativity"));
        });

        undoRedoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void aBlockThatChangesNothingIsNotPushed() {
        undoRedoManager.addEdit("no-op", edit ->
                // Setting the value it already has reports no change.
                edit.addEdit(entry.setField(StandardField.AUTHOR, "Einstein")));

        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void nestedBlocksProduceASingleStep() {
        undoRedoManager.addEdit("outer", outer -> {
            outer.addEdit(entry.setField(StandardField.AUTHOR, "Bohr"));
            undoRedoManager.addEdit("inner", inner -> inner.addEdit(entry.setField(StandardField.TITLE, "Relativity")));
        });

        undoRedoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void pushInsideABlockJoinsIt() {
        undoRedoManager.addEdit("outer", outer -> {
            outer.addEdit(entry.setField(StandardField.AUTHOR, "Bohr"));
            undoRedoManager.addEdit(setAuthor("Planck"));
        });

        undoRedoManager.undo();

        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void anEmptySetIsNotAnUndoStep() {
        undoRedoManager.addEdit(new ChangeSet("nothing", List.of()));

        assertFalse(undoRedoManager.canUndo());
    }

    /// A change that throws while being reverted must stay undoable rather than disappear from
    /// both stacks. Re-inserting a removed string collides once the name is taken again.
    @Test
    void aFailingUndoLeavesTheChangeOnTheStack() {
        BibDatabase database = new BibDatabase();
        BibtexString removed = new BibtexString("label", "content");
        database.addString(removed);

        UndoableRemoveString removal = new UndoableRemoveString(database, removed);
        removal.apply();
        database.addString(new BibtexString("label", "something else"));
        undoRedoManager.addEdit(removal);

        assertThrows(KeyCollisionException.class, undoRedoManager::undo);
        assertTrue(undoRedoManager.canUndo());
        assertFalse(undoRedoManager.canRedo());
    }

    /// The stack keeps the BibEntry objects of removed entries alive, so it is bounded.
    @Test
    void theStackIsBounded() {
        for (int i = 0; i < 150; i++) {
            undoRedoManager.addEdit(setAuthor("Author " + i));
        }

        for (int i = 0; i < 100; i++) {
            assertTrue(undoRedoManager.canUndo(), "expected 100 undoable steps, ran out at " + i);
            undoRedoManager.undo();
        }
        assertFalse(undoRedoManager.canUndo());
    }

    /// One manager serves the whole application and long commands record from background tasks.
    /// An edit made meanwhile must become its own step, not join the background command's.
    @Test
    void aBlockOnAnotherThreadDoesNotCaptureThisThreadsEdits() throws Exception {
        CountDownLatch blockStarted = new CountDownLatch(1);
        CountDownLatch editMade = new CountDownLatch(1);

        Thread background = new Thread(() -> undoRedoManager.addEdit("background", edit -> {
            edit.addEdit(setAuthor("Bohr"));
            blockStarted.countDown();
            try {
                editMade.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        background.start();

        blockStarted.await();
        undoRedoManager.addEdit(setAuthor("Planck"));
        editMade.countDown();
        background.join();

        // One step for the foreground edit, one for the block.
        undoRedoManager.undo();
        assertTrue(undoRedoManager.canUndo());
        undoRedoManager.undo();
        assertFalse(undoRedoManager.canUndo());
    }

    @Test
    void undoingBackToTheSavedPositionReportsUnchanged() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.markUnchanged();
        assertFalse(undoRedoManager.hasChanged());

        undoRedoManager.addEdit(setAuthor("Planck"));
        assertTrue(undoRedoManager.hasChanged());

        undoRedoManager.undo();
        assertFalse(undoRedoManager.hasChanged());
    }

    @Test
    void editingPastTheStackLimitStillReportsChanged() {
        // Fill the stack to its limit, then save. Every further edit trims one edit off the
        // bottom, so the stack depth no longer moves — but the library has still changed.
        for (int i = 0; i < 200; i++) {
            undoRedoManager.addEdit(setAuthor("Author " + i));
        }
        undoRedoManager.markUnchanged();

        undoRedoManager.addEdit(setAuthor("Planck"));
        assertTrue(undoRedoManager.hasChanged());
    }

    @Test
    void undoingEverythingAfterTrimmingReportsChanged() {
        undoRedoManager.markUnchanged();
        for (int i = 0; i < 200; i++) {
            undoRedoManager.addEdit(setAuthor("Author " + i));
        }

        // The stack empties, but the edits it discarded to stay within its limit are still
        // applied and can no longer be undone, so the library differs from the saved position.
        while (undoRedoManager.canUndo()) {
            undoRedoManager.undo();
        }
        assertTrue(undoRedoManager.hasChanged());
    }

    @Test
    void redoingBackToTheSavedPositionReportsUnchanged() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.markUnchanged();
        undoRedoManager.undo();
        assertTrue(undoRedoManager.hasChanged());

        undoRedoManager.redo();
        assertFalse(undoRedoManager.hasChanged());
    }

    @Test
    void aListenerRegisteredWhileNotifyingDoesNotDisturbTheNotification() {
        AtomicInteger registeredLater = new AtomicInteger();
        undoRedoManager.addListener(() -> undoRedoManager.addListener(registeredLater::incrementAndGet));

        // Iterating the listeners directly would throw ConcurrentModificationException here.
        undoRedoManager.addEdit(setAuthor("Bohr"));

        assertTrue(undoRedoManager.canUndo());
        // The snapshot this notification iterates predates the new listener, so it runs from
        // the next notification onwards, not from this one.
        assertEquals(0, registeredLater.get());

        undoRedoManager.undo();
        assertEquals(1, registeredLater.get());
    }

    @Test
    void aThrowingListenerNeitherFailsTheEditNorHidesTheOthers() {
        AtomicInteger reached = new AtomicInteger();
        undoRedoManager.addListener(() -> {
            throw new IllegalStateException("this listener is broken");
        });
        undoRedoManager.addListener(reached::incrementAndGet);

        undoRedoManager.addEdit(setAuthor("Bohr"));

        assertTrue(undoRedoManager.canUndo());
        assertEquals(1, reached.get());
    }

    /// A listener that waits for another thread to read the manager. Were listeners still run
    /// while the stack monitor is held, that read would block until the listener returns and
    /// the listener would block until the read completes.
    ///
    /// The listener records the outcome instead of asserting it, because it runs on whichever
    /// thread pushed the edit and an assertion failing there would never reach the test.
    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void listenersDoNotRunWhileTheStackLockIsHeld() {
        AtomicBoolean readCompleted = new AtomicBoolean();
        undoRedoManager.addListener(() -> {
            Thread reader = new Thread(undoRedoManager::canUndo, "undo-state-reader");
            reader.start();
            try {
                reader.join(Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            readCompleted.set(!reader.isAlive());
        });

        undoRedoManager.addEdit(setAuthor("Bohr"));

        assertTrue(readCompleted.get(), "reading the manager from a listener deadlocked");
        assertTrue(undoRedoManager.canUndo());
    }

    @Test
    void clearResetsTheSavedPosition() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        assertTrue(undoRedoManager.hasChanged());

        undoRedoManager.clear();
        assertFalse(undoRedoManager.hasChanged());
    }
}
