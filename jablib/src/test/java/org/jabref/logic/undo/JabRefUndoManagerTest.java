package org.jabref.logic.undo;

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

    /// The twin of [#pushInsideABlockJoinsIt] for the applying entry point: called on the
    /// manager rather than on the recorder, it still joins the step being collected instead of
    /// becoming one of its own — and takes no lock on the way, since nothing reaches the stacks
    /// until the block ends.
    @Test
    void applyingThroughTheManagerInsideABlockJoinsIt() {
        undoRedoManager.addEdit("two fields", _ -> {
            undoRedoManager.applyEdit(new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr"));
            undoRedoManager.applyEdit(new UndoableFieldChange(entry, StandardField.TITLE, null, "On the quantum theory"));
        });

        assertEquals(Optional.of("Bohr"), entry.getField(StandardField.AUTHOR));

        undoRedoManager.undo();
        assertFalse(undoRedoManager.canUndo(), "the two changes did not become one step");
        assertEquals(Optional.of("Einstein"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
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
    /// both stacks. Re-inserting a removed string collides when its id has been taken since - a
    /// name collision is refused rather than thrown, which the test below covers.
    @Test
    void aFailingUndoLeavesTheChangeOnTheStack() {
        BibDatabase database = new BibDatabase();
        BibtexString removed = new BibtexString("label", "content");
        database.addString(removed);

        UndoableRemoveString removal = new UndoableRemoveString(database, removed);
        removal.apply();
        BibtexString sameId = new BibtexString("other label", "something else");
        sameId.setId(removed.getId());
        database.addString(sameId);
        undoRedoManager.addEdit(removal);

        assertThrows(KeyCollisionException.class, undoRedoManager::undo);
        assertTrue(undoRedoManager.canUndo());
        assertFalse(undoRedoManager.canRedo());
    }

    /// The same situation the library can actually get into: the name is taken again, so putting
    /// the string back would overwrite someone else's. That is reported, and the step is spent -
    /// leaving it on the stack would make the next Ctrl+Z look broken.
    @Test
    void anUndoThatCannotBeAppliedIsReportedAndSpent() {
        BibDatabase database = new BibDatabase();
        BibtexString removed = new BibtexString("label", "content");
        database.addString(removed);

        UndoableRemoveString removal = new UndoableRemoveString(database, removed);
        removal.apply();
        database.addString(new BibtexString("label", "something else"));
        undoRedoManager.addEdit(removal);

        UndoStep step = undoRedoManager.undo().orElseThrow();

        assertFalse(step.complete());
        assertEquals("something else", database.getStringByName("label").orElseThrow().getContent(),
                "the undo overwrote the string that took the name");
        assertFalse(undoRedoManager.canUndo());
        assertTrue(undoRedoManager.canRedo());
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

    /// An edit balance returns to the saved value along a history that never passes through the
    /// saved position, because pushing B discarded A. Counting edits cannot tell the two apart.
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

    /// The boundary of the two trim tests around it: the saved change is the one the limit drops.
    /// It stays applied, so undoing everything still on the stack leaves the library at exactly
    /// the state that was saved, and it has to say so.
    @Test
    void undoingBackToASavedPositionTheLimitDroppedReportsUnchanged() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.markUnchanged();

        // One more than the stack holds, so the trim drops the saved change and nothing else.
        for (int i = 0; i < 100; i++) {
            undoRedoManager.addEdit(setAuthor("Author " + i));
        }
        while (undoRedoManager.canUndo()) {
            undoRedoManager.undo();
        }

        assertEquals(Optional.of("Bohr"), entry.getField(StandardField.AUTHOR));
        assertFalse(undoRedoManager.hasChanged());
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

    /// Severity does not make the library less modified: an Error ends the block like anything
    /// else, and what it wrote before dying still has to be takeable back.
    @Test
    void aBlockKilledByAnErrorHandsOverWhatItAlreadyChanged() {
        assertThrows(AssertionError.class, () -> undoRedoManager.addEdit("half", edit -> {
            edit.addEdit(setAuthor("Bohr"));
            throw new AssertionError("a model invariant gave way here");
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

    /// The defect suspending guards against: a background command applies its changes long before it
    /// pushes them, and an undo arriving in that window takes back a change *underneath* those
    /// writes - after which the command's push discards the undone change with the redo stack.
    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    // [utest->req~logic.undo.writes-reserved-against-undo~1]
    void anUndoCannotLandBetweenACommandsWritesAndItsPush() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        CountDownLatch blockStarted = new CountDownLatch(1);
        CountDownLatch undoAttempted = new CountDownLatch(1);

        try (ExecutorService background = Executors.newSingleThreadExecutor()) {
            Future<?> block = background.submit(() -> undoRedoManager.addEdit("Import entries", edit -> {
                edit.addEdit(setAuthor("Planck"));
                blockStarted.countDown();
                await(undoAttempted, "the recording block was never released");
            }));

            await(blockStarted, "the recording block never started");
            assertEquals(Optional.empty(), undoRedoManager.undo(), "undo ran while the library was being written");
            assertEquals(Optional.of("Planck"), entry.getField(StandardField.AUTHOR),
                    "the undo reverted a change underneath the command's writes");

            undoAttempted.countDown();
            assertTrue(completes(block), "the recording block never finished");
        }

        // Both steps survived: the command's, and the one it would have discarded.
        undoRedoManager.undo();
        assertEquals(Optional.of("Bohr"), entry.getField(StandardField.AUTHOR));
        undoRedoManager.undo();
        assertEquals(Optional.of("Einstein"), entry.getField(StandardField.AUTHOR));
    }

    /// The library moved on under a recorded step - a background command wrote the same field - so
    /// undoing it takes back what it can and says the rest did not apply.
    @Test
    void undoingAStepTheLibraryMovedOnFromReportsIt() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        entry.setField(StandardField.AUTHOR, "Planck");

        UndoStep step = undoRedoManager.undo().orElseThrow();

        assertFalse(step.complete(), "the undo claimed to have taken the step back");
        assertEquals(Optional.of("Planck"), entry.getField(StandardField.AUTHOR), "undo wrote over the newer value");
        assertFalse(undoRedoManager.canUndo(), "the step was not consumed");
        assertTrue(undoRedoManager.canRedo());
    }

    /// A change the journal cannot take back is a saved position no position can reach: undoing
    /// everything does not mean "back to what was saved", because that change is still there.
    @Test
    void aChangeTheJournalCannotTakeBackKeepsTheLibraryChanged() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.markUnchanged();
        assertFalse(undoRedoManager.hasChanged());

        undoRedoManager.markChanged();
        assertTrue(undoRedoManager.hasChanged());

        undoRedoManager.undo();
        assertTrue(undoRedoManager.hasChanged(), "undoing cleared a marker the journal cannot clear");
        assertTrue(undoRedoManager.canRedo(), "the stack was discarded rather than left alone");

        undoRedoManager.markUnchanged();
        assertFalse(undoRedoManager.hasChanged(), "saving did not clear it");
    }

    @Test
    void markingTheLibraryChangedNotifiesListeners() {
        AtomicInteger notifications = new AtomicInteger();
        undoRedoManager.addListener(notifications::incrementAndGet);

        undoRedoManager.markChanged();

        assertEquals(1, notifications.get());
    }

    /// A marker derived from the journal has to hear about the one moment the answer turns false.
    @Test
    void markingTheCurrentPositionSavedNotifiesListeners() {
        undoRedoManager.addEdit(setAuthor("Bohr"));
        AtomicInteger notifications = new AtomicInteger();
        undoRedoManager.addListener(notifications::incrementAndGet);

        undoRedoManager.markUnchanged();

        assertFalse(undoRedoManager.hasChanged());
        assertEquals(1, notifications.get(), "the saved position moved without telling anyone");
    }

    @Test
    void aReservationMakesUndoAndRedoDecline() {
        // One step on each stack, so neither answer can be right for the wrong reason.
        undoRedoManager.addEdit(setAuthor("Bohr"));
        undoRedoManager.addEdit(setAuthor("Planck"));
        undoRedoManager.undo();
        assertTrue(undoRedoManager.canUndo());
        assertTrue(undoRedoManager.canRedo());

        try (UndoSuspension suspended = undoRedoManager.suspendUndo("Import entries")) {
            // The steps are still there, and the menu item stays enabled on purpose: a disabled
            // item swallows its accelerator, and Ctrl+Z doing nothing silently is what this is
            // meant to avoid. What declines is the operation.
            assertTrue(undoRedoManager.canUndo());
            assertTrue(undoRedoManager.canRedo());
            assertEquals(Optional.empty(), undoRedoManager.undo());
            assertEquals(Optional.empty(), undoRedoManager.redo());
            assertEquals(Optional.of("Import entries"), undoRedoManager.suspendedBy());
        }

        assertTrue(undoRedoManager.canUndo());
        assertTrue(undoRedoManager.canRedo());
        assertEquals(Optional.empty(), undoRedoManager.suspendedBy());
    }

    /// Enablement has to fall when a command takes the library and rise when it gives it back, so
    /// both ends are stack changes as far as an observer is concerned.
    @Test
    void takingAndReleasingAReservationNotifiesListeners() {
        AtomicInteger notifications = new AtomicInteger();
        undoRedoManager.addListener(notifications::incrementAndGet);

        UndoSuspension suspended = undoRedoManager.suspendUndo("Import entries");
        assertEquals(1, notifications.get());

        suspended.close();
        assertEquals(2, notifications.get());

        // Idempotent, so a task closing on more than one of its outcomes says nothing twice.
        suspended.close();
        assertEquals(2, notifications.get());
        assertEquals(Optional.empty(), undoRedoManager.suspendedBy());
    }

    @Test
    void twoCommandsHoldTheLibraryUntilBothHaveHandedOver() {
        undoRedoManager.addEdit(setAuthor("Bohr"));

        UndoSuspension first = undoRedoManager.suspendUndo("Import entries");
        UndoSuspension second = undoRedoManager.suspendUndo("Look up DOI");
        first.close();

        assertEquals(Optional.empty(), undoRedoManager.undo(), "undo ran while a command was still writing");
        assertEquals(Optional.of("Look up DOI"), undoRedoManager.suspendedBy(),
                "named a command that had already finished");

        second.close();
        assertTrue(undoRedoManager.undo().isPresent(), "undo still declined after both had handed over");
    }

    /// Of several commands writing at once, the message names the one still running that the user
    /// has been waiting on longest.
    @Test
    void theCommandNamedIsTheOldestStillWriting() {
        UndoSuspension first = undoRedoManager.suspendUndo("Import entries");
        UndoSuspension second = undoRedoManager.suspendUndo("Look up DOI");

        assertEquals(Optional.of("Import entries"), undoRedoManager.suspendedBy());

        second.close();
        assertEquals(Optional.of("Import entries"), undoRedoManager.suspendedBy());

        first.close();
        assertEquals(Optional.empty(), undoRedoManager.suspendedBy());
    }

    @Test
    void aBlockHoldsTheLibraryForItsWholeDurationAndReleasesItAfterThePush() {
        undoRedoManager.addEdit(setAuthor("Bohr"));

        undoRedoManager.addEdit("Import entries", edit -> {
            assertEquals(Optional.empty(), undoRedoManager.undo(), "the block did not hold the library");
            assertEquals(Optional.of("Import entries"), undoRedoManager.suspendedBy());
            edit.addEdit(setAuthor("Planck"));
        });

        assertTrue(undoRedoManager.undo().isPresent(), "the block did not release the library");
        assertEquals(Optional.empty(), undoRedoManager.suspendedBy());
    }

    /// A nested block is inside its caller's window already; releasing at its end would reopen the
    /// window while the outer block is still writing.
    @Test
    void aNestedBlockDoesNotReleaseTheLibraryWhenItEnds() {
        undoRedoManager.addEdit("Import entries", edit -> {
            undoRedoManager.addEdit("Merge entries", nested -> nested.addEdit(setAuthor("Planck")));
            assertEquals(Optional.of("Import entries"), undoRedoManager.suspendedBy(),
                    "the nested block released the library its caller was holding");
        });

        assertEquals(Optional.empty(), undoRedoManager.suspendedBy());
    }

    @Test
    void aBlockThatFailsDoesNotKeepHoldingTheLibrary() {
        assertThrows(IllegalStateException.class, () -> undoRedoManager.addEdit("Import entries", edit -> {
            edit.addEdit(setAuthor("Planck"));
            throw new IllegalStateException("import failed");
        }));

        assertEquals(Optional.empty(), undoRedoManager.suspendedBy());
        assertTrue(undoRedoManager.canUndo(), "what the failed block managed to change stayed undoable");
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
