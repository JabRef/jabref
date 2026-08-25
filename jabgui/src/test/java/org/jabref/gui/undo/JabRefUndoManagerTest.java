package org.jabref.gui.undo;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.ChangeSet;
import org.jabref.model.undo.UndoableFieldChange;
import org.jabref.model.undo.UndoableRemoveString;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JabRefUndoManagerTest {

    private final JabRefUndoManager undoRedoManager = new JabRefUndoManager();
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        entry = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Einstein");
    }

    private UndoableFieldChange setField(Field field, String value) {
        String before = entry.getField(field).orElse(null);
        entry.setField(field, value);
        return new UndoableFieldChange(entry, field, before, value);
    }

    private UndoableFieldChange setAuthor(String value) {
        return setField(StandardField.AUTHOR, value);
    }

    /// Waits for the other thread, failing this test rather than hanging the suite.
    private static void await(CountDownLatch released, String message) {
        try {
            assertTrue(released.await(5, TimeUnit.SECONDS), message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    /// Whether the other thread got as far as finishing, which a deadlocked one never does.
    private static boolean completes(Future<?> work) {
        try {
            work.get(5, TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        } catch (ExecutionException e) {
            throw new AssertionError(e.getCause());
        }
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
    void applyPerformsTheChangeAndRecordsIt() {
        undoRedoManager.applyEdit(new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr"));
        assertEquals(Optional.of("Bohr"), entry.getField(StandardField.AUTHOR));

        undoRedoManager.undo();
        assertEquals(Optional.of("Einstein"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void applyInsideABlockJoinsIt() {
        undoRedoManager.addEdit("both", edit -> {
            edit.applyEdit(new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr"));
            edit.applyEdit(new UndoableFieldChange(entry, StandardField.TITLE, null, "On the quantum theory"));
        });
        assertEquals(Optional.of("Bohr"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("On the quantum theory"), entry.getField(StandardField.TITLE));

        undoRedoManager.undo();
        assertFalse(undoRedoManager.canUndo());
        assertEquals(Optional.of("Einstein"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
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
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void aBlockOnAnotherThreadDoesNotCaptureThisThreadsEdits() {
        CountDownLatch blockStarted = new CountDownLatch(1);
        CountDownLatch editMade = new CountDownLatch(1);

        try (ExecutorService background = Executors.newSingleThreadExecutor()) {
            Future<?> block = background.submit(() -> undoRedoManager.addEdit("background", edit -> {
                edit.addEdit(setAuthor("Bohr"));
                blockStarted.countDown();
                await(editMade, "the recording block was never released");
            }));

            await(blockStarted, "the recording block never started");
            undoRedoManager.addEdit(setAuthor("Planck"));
            editMade.countDown();
            assertTrue(completes(block), "the recording block never finished");
        }

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

    /// The defect P14 fixes: an edit balance returns to the saved value along a history that
    /// never passes through the saved position, because pushing B discarded A.
    @Test
    void editingAfterUndoingTheSavedChangeReportsChanged() {
        // [utest->req~logic.undo.saved-position-identity~1]
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.markUnchanged();

        undoRedoManager.undo();
        undoRedoManager.addEdit(setAuthor("Planck"));

        assertFalse(undoRedoManager.canRedo(), "the saved change is still reachable");
        assertTrue(undoRedoManager.hasChanged());
    }

    /// Redoing forward again does not return to the saved position either: the change that was
    /// saved is gone, and the one now on the stack was never saved.
    @Test
    void redoingAnEditMadeAfterUndoingTheSavedChangeReportsChanged() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.markUnchanged();
        undoRedoManager.undo();
        undoRedoManager.addEdit(setAuthor("Planck"));

        undoRedoManager.undo();
        undoRedoManager.redo();

        assertTrue(undoRedoManager.hasChanged());
    }

    /// Positions are identified, not counted, so a stack that returns to the depth it was saved
    /// at along a different history is not the saved position.
    @Test
    void aDifferentHistoryOfTheSameLengthIsNotTheSavedPosition() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.addEdit(setField(StandardField.TITLE, "On the constitution of atoms"));
        undoRedoManager.markUnchanged();

        undoRedoManager.undo();
        undoRedoManager.undo();
        undoRedoManager.addEdit(setAuthor("Planck"));
        undoRedoManager.addEdit(setField(StandardField.TITLE, "On the law of energy distribution"));

        assertTrue(undoRedoManager.hasChanged());
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
        // [utest->req~logic.undo.saved-position-identity~1]
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

    /// The library is already modified by whatever the block managed to do before it failed, so
    /// the half it recorded has to reach the stack — otherwise that half cannot be taken back.
    @Test
    void aBlockThatFailsPartWayHandsOverWhatItAlreadyChanged() {
        assertThrows(IllegalStateException.class, () -> undoRedoManager.addEdit("half", edit -> {
            edit.addEdit(setAuthor("Bohr"));
            throw new IllegalStateException("the command gave up here");
        }));

        assertTrue(undoRedoManager.canUndo());
        undoRedoManager.undo();
        assertEquals(Optional.of("Einstein"), entry.getField(StandardField.AUTHOR));
    }

    /// Nothing recorded means nothing to hand over, failure or not: an empty step would enable
    /// Undo and let the next Ctrl+Z consume a no-op instead of the user's previous edit.
    @Test
    void aBlockThatFailsBeforeRecordingAnythingPushesNoStep() {
        assertThrows(IllegalStateException.class, () -> undoRedoManager.addEdit("nothing", _ -> {
            throw new IllegalStateException("the command gave up immediately");
        }));

        assertFalse(undoRedoManager.canUndo());
    }

    /// A failing nested block hands its changes to the enclosing one for the same reason, and
    /// the enclosing block decides for itself whether the failure ends the whole step.
    @Test
    void aNestedBlockThatFailsHandsOverToItsEnclosingBlock() {
        undoRedoManager.addEdit("outer", outer -> {
            outer.addEdit(setAuthor("Bohr"));
            assertThrows(IllegalStateException.class, () -> undoRedoManager.addEdit("inner", inner -> {
                inner.addEdit(setField(StandardField.TITLE, "On the quantum theory"));
                throw new IllegalStateException("the nested command gave up here");
            }));
        });

        // One step for the whole thing, holding both the outer change and the nested one. The
        // nested change touches a different field, so losing it would survive undoing the outer.
        undoRedoManager.undo();
        assertFalse(undoRedoManager.canUndo());
        assertEquals(Optional.of("Einstein"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
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
    /// The listener records the outcome rather than asserting it, because it runs on whichever
    /// thread pushed the edit and an assertion failing there would never reach the test.
    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void listenersDoNotRunWhileTheStackLockIsHeld() {
        AtomicBoolean readCompleted = new AtomicBoolean();

        try (ExecutorService reader = Executors.newSingleThreadExecutor()) {
            undoRedoManager.addListener(() -> readCompleted.set(completes(reader.submit(undoRedoManager::canUndo))));

            undoRedoManager.addEdit(setAuthor("Bohr"));
        }

        assertTrue(readCompleted.get(), "reading the manager from a listener deadlocked");
        assertTrue(undoRedoManager.canUndo());
    }

    /// An entry that runs `probe` from inside `setField`, when the field is set to `value`. The
    /// write happens on the thread making the change, so the probe runs at the one moment
    /// [JabRefUndoManager#applyEdit] has written to the library and not yet recorded anything — the window
    /// this test is about.
    private static class ProbingEntry extends BibEntry {

        private final String value;
        private final Runnable probe;

        ProbingEntry(String value, Runnable probe) {
            super(StandardEntryType.Article);
            this.value = value;
            this.probe = probe;
        }

        @Override
        public Optional<FieldChange> setField(Field field, @Nullable String newValue) {
            Optional<FieldChange> change = super.setField(field, newValue);
            if (value.equals(newValue)) {
                probe.run();
            }
            return change;
        }
    }

    /// Applying and recording are one operation, holding the journal's monitor throughout. Were
    /// they two, an undo arriving in between would revert the *previous* change while this one
    /// stayed applied but unrecorded, leaving a history that describes a library state that
    /// never existed.
    ///
    /// Asked of the applying thread rather than staged between two threads. A second thread can
    /// only ever show that it did not get in *within some interval*, which makes the assertion a
    /// statement about a timeout; whether the lock is held is a fact available on the spot.
    @Test
    void applyingAChangeHappensWhileTheJournalIsLocked() {
        // [utest->req~logic.undo.apply-and-record-atomically~1]
        AtomicBoolean lockedWhileApplying = new AtomicBoolean();
        BibEntry probingEntry = new ProbingEntry("Bohr",
                () -> lockedWhileApplying.set(Thread.holdsLock(undoRedoManager)));

        undoRedoManager.applyEdit(new UndoableFieldChange(probingEntry, StandardField.AUTHOR, null, "Bohr"));

        assertTrue(lockedWhileApplying.get(),
                "the library was written before the journal was locked, so an undo could have run in between");
        assertEquals(Optional.of("Bohr"), probingEntry.getField(StandardField.AUTHOR));
        assertTrue(undoRedoManager.canUndo());
    }

    /// Inside a block there is no window to close, and taking the manager's monitor there would
    /// hold it across the whole block. The change is applied and joins the step being collected.
    @Test
    void applyingInsideABlockRecordsIntoThatStep() {
        undoRedoManager.addEdit("two fields", edit -> {
            edit.applyEdit(new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr"));
            edit.applyEdit(new UndoableFieldChange(entry, StandardField.TITLE, null, "On the constitution of atoms"));
        });

        assertEquals(Optional.of("Bohr"), entry.getField(StandardField.AUTHOR));
        undoRedoManager.undo();
        assertFalse(undoRedoManager.canUndo(), "the two changes did not become one step");
        assertEquals(Optional.of("Einstein"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
    }

    @Test
    void clearResetsTheSavedPosition() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        assertTrue(undoRedoManager.hasChanged());

        undoRedoManager.clear();
        assertFalse(undoRedoManager.hasChanged());
    }
}
