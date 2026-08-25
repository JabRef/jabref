package org.jabref.gui.undo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.UndoableFieldChange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Unlike [UndoManagerTest], this needs a live toolkit: the point of this class is the hop onto
/// the JavaFX thread, and `Platform.runLater` is what requires one. The properties themselves
/// would not.
@ExtendWith(ApplicationExtension.class)
class JabRefGuiUndoManagerTest {

    private final JabRefGuiUndoManager undoManager = new JabRefGuiUndoManager();
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
    void propertiesStartOutMatchingAnEmptyManager() {
        assertFalse(undoManager.undoableProperty().get());
        assertFalse(undoManager.redoableProperty().get());
    }

    /// Recorded from the test thread, so the update is posted to the JavaFX thread rather than
    /// applied inline and each assertion has to let that queue drain first.
    @Test
    void propertiesFollowTheStacks() {
        undoManager.addEdit(setAuthor("Bohr"));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(undoManager.undoableProperty().get());
        assertFalse(undoManager.redoableProperty().get());

        undoManager.undo();
        WaitForAsyncUtils.waitForFxEvents();
        assertFalse(undoManager.undoableProperty().get());
        assertTrue(undoManager.redoableProperty().get());

        undoManager.redo();
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(undoManager.undoableProperty().get());
        assertFalse(undoManager.redoableProperty().get());
    }

    /// Recorded from the JavaFX thread, where there is nothing to wait for: the property is
    /// already current when the edit returns, rather than a pulse behind it.
    @Test
    void propertiesFollowTheStacksWithoutADelayOnTheJavaFxThread() {
        AtomicBoolean undoableImmediatelyAfterTheEdit = new AtomicBoolean();

        WaitForAsyncUtils.asyncFx(() -> {
            undoManager.addEdit(setAuthor("Bohr"));
            undoableImmediatelyAfterTheEdit.set(undoManager.undoableProperty().get());
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(undoableImmediatelyAfterTheEdit.get());
    }

    /// Each queued update reads the stacks when it runs, not when it was requested, so a value
    /// the manager has already moved past never reaches a property. Both mutations below land
    /// while the JavaFX thread is blocked, so both updates run after the stack is empty again
    /// and the menu is never told about an undo that is already gone.
    @Test
    void aQueuedUpdateAppliesTheStateItFindsWhenItRuns() throws InterruptedException {
        List<Boolean> undoableValues = new CopyOnWriteArrayList<>();
        WaitForAsyncUtils.asyncFx(() -> undoManager.undoableProperty()
                                                  .addListener((observable, was, is) -> undoableValues.add(is)));
        WaitForAsyncUtils.waitForFxEvents();

        CountDownLatch release = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                // Bounded, so that a failure here fails this test rather than leaving the
                // JavaFX thread blocked for every test that follows.
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        undoManager.addEdit(setAuthor("Bohr"));
        undoManager.undo();
        release.countDown();
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(undoManager.undoableProperty().get());
        assertEquals(List.of(), undoableValues);
    }
}
