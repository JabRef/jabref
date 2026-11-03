package org.jabref.logic.openoffice.engine;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jabref.logic.JabRefException;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoUndo;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.document.UndoFailedException;
import com.sun.star.document.UndoManagerEvent;
import com.sun.star.document.XUndoAction;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerListener;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class DocumentSessionTest {
    private static final String TITLE = "Insert citation";
    private final XTextDocument document = mock(XTextDocument.class);
    private final XUndoManager manager = mock(XUndoManager.class);
    private final AtomicReference<XUndoManagerListener> listener = new AtomicReference<>();
    private final AtomicReference<XUndoAction> sentinel = new AtomicReference<>();

    private void rootContext(int depth) throws Exception {
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(0));
            return null;
        }).when(manager).addUndoManagerListener(any());
        doAnswer(_ -> {
            Optional.ofNullable(listener.get()).orElseThrow().enteredContext(new UndoManagerEvent(manager, TITLE, depth));
            return null;
        }).when(manager).enterUndoContext(TITLE);
        doAnswer(_ -> {
            Optional.ofNullable(listener.get()).orElseThrow().leftContext(new UndoManagerEvent(manager, TITLE, depth - 1));
            return null;
        }).when(manager).leaveUndoContext();
        doAnswer(invocation -> {
            sentinel.set(invocation.getArgument(0));
            return null;
        }).when(manager).addUndoAction(any());
        when(manager.isUndoPossible()).thenReturn(true);
        when(manager.getCurrentUndoActionTitle()).thenReturn(TITLE);
    }

    private MockedStatic<UnoRedlines> recording() throws Exception {
        MockedStatic<UnoRedlines> recording = mockStatic(UnoRedlines.class);
        recording.when(() -> UnoRedlines.withRecordChangesSuspended(eq(document), any())).thenAnswer(invocation -> {
            invocation.getArgument(1, UnoRedlines.RedlineAction.class).run();
            return null;
        });
        return recording;
    }

    // [utest->req~openoffice.next-citation-format-conversion~1]
    @Test
    void failureBeforeAnyRealWriteUndoesOnlyItsOwnRegisteredSentinel() throws Exception {
        rootContext(1);
        doAnswer(_ -> {
            Optional.ofNullable(sentinel.get()).orElseThrow().undo();
            return null;
        }).when(manager).undo();
        JabRefException original = new JabRefException("Injected failure");
        try (MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class);
             MockedStatic<UnoRedlines> recording = recording()) {
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            OOResult<?, JabRefException> result = new DocumentSession(document).writeWithRollback(TITLE, () -> OOResult.error(original));
            assertSame(original, result.getError());
            verify(manager).undo();
            verify(manager).clearRedo();
            verify(document, times(2)).unlockControllers();
        }
    }

    @Test
    void sentinelRegistrationFailureNeverUndoesAnEarlierUserAction() throws Exception {
        rootContext(1);
        doThrow(new com.sun.star.lang.IllegalArgumentException("Injected registration failure")).when(manager).addUndoAction(any());
        try (MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            assertTrue(new DocumentSession(document).writeWithRollback(TITLE, () -> OOResult.ok("must not run")).isError());
            verify(manager, never()).undo();
            verify(document, never()).lockControllers();
        }
    }

    @Test
    void nestedContextIsRejectedBeforeTheSentinelOrDocumentWrites() throws Exception {
        rootContext(2);
        try (MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            assertTrue(new DocumentSession(document).writeWithRollback(TITLE, () -> OOResult.ok("must not run")).isError());
            verify(manager, never()).addUndoAction(any());
            verify(manager, never()).undo();
            verify(document, never()).lockControllers();
        }
    }

    @Test
    void rollbackFailureIsDistinctAndRetainsTheOriginalWriteFailure() throws Exception {
        rootContext(1);
        UndoFailedException rollback = new UndoFailedException("Injected rollback failure");
        doThrow(rollback).when(manager).undo();
        JabRefException original = new JabRefException("Injected write failure");
        try (MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class);
             MockedStatic<UnoRedlines> recording = recording()) {
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            OOResult<?, JabRefException> result = new DocumentSession(document).writeWithRollback(TITLE, () -> OOResult.error(original));
            assertTrue(result.isError());
            assertTrue(Optional.ofNullable(result.getError().getLocalizedMessage()).orElseThrow().contains("could not be rolled back completely"));
            assertSame(rollback, Optional.ofNullable(result.getError().getCause()).orElseThrow().getCause());
            assertEquals(original, Optional.ofNullable(result.getError().getCause()).orElseThrow().getSuppressed()[0]);
            verify(manager, never()).clearRedo();
        }
    }

    @Test
    void failedPropertyCompensationIsReportedEvenIfTheUndoContextReplaysSuccessfully() throws Exception {
        rootContext(1);
        doAnswer(_ -> {
            Optional.ofNullable(sentinel.get()).orElseThrow().undo();
            return null;
        }).when(manager).undo();
        JabRefException original = new JabRefException("Property registration failure");
        original.addSuppressed(new com.sun.star.beans.PropertyVetoException("Immediate compensation also failed"));
        try (MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class);
             MockedStatic<UnoRedlines> recording = recording()) {
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            OOResult<?, JabRefException> result = new DocumentSession(document).writeWithRollback(TITLE, () -> OOResult.error(original));
            assertTrue(Optional.ofNullable(result.getError().getLocalizedMessage()).orElseThrow().contains("could not be rolled back completely"));
            assertSame(original, result.getError().getCause());
        }
    }

    @Test
    void lostUndoOwnershipNeverUndoesAnEarlierUserAction() throws Exception {
        rootContext(1);
        try (MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class);
             MockedStatic<UnoRedlines> recording = recording()) {
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            OOResult<?, JabRefException> result = new DocumentSession(document).writeWithRollback(TITLE, () -> {
                Optional.ofNullable(listener.get()).orElseThrow().allActionsCleared(new EventObject(manager));
                return OOResult.error(new JabRefException("Write failed after external undo reset"));
            });
            assertTrue(Optional.ofNullable(result.getError().getLocalizedMessage()).orElseThrow().contains("could not be rolled back completely"));
            verify(manager, never()).undo();
        }
    }

    @Test
    void missingUndoManagerRejectsConversionBeforeCallingTheAction() {
        try (MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.empty());
            Supplier<OOResult<String, JabRefException>> action = mock();
            assertTrue(new DocumentSession(document).writeWithRollback(TITLE, action).isError());
            verify(action, never()).get();
        }
    }

    @Test
    void ordinaryWriteStillDoesNotRollBackAnErrorResult() throws Exception {
        try (MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class);
             MockedStatic<UnoRedlines> recording = recording()) {
            JabRefException original = new JabRefException("Ordinary write failure");
            OOResult<?, JabRefException> result = new DocumentSession(document).write(TITLE, () -> OOResult.error(original));
            assertSame(original, result.getError());
            verify(manager, never()).undo();
            undo.verify(() -> UnoUndo.enterUndoContext(document, TITLE));
            undo.verify(() -> UnoUndo.leaveUndoContext(document));
        }
    }
}
