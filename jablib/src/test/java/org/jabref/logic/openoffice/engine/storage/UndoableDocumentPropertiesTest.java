package org.jabref.logic.openoffice.engine.storage;

import java.util.Optional;

import org.jabref.logic.openoffice.uno.UnoUndo;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.document.UndoFailedException;
import com.sun.star.document.XUndoAction;
import com.sun.star.document.XUndoManager;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class UndoableDocumentPropertiesTest {
    private static final String PROPERTY = "JabRef_page_info:uid:0";
    private final XTextDocument document = mock(XTextDocument.class);
    private final XUndoManager manager = mock(XUndoManager.class);

    @Test
    void undoAndRedoRestoreTheBeforeAndAfterValuesWithoutRegisteringMoreActions() throws Exception {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY)).thenReturn(Optional.of("p. 5"));
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            UndoableDocumentProperties.set(document, PROPERTY, Optional.of("p. 7"));
            ArgumentCaptor<XUndoAction> action = ArgumentCaptor.forClass(XUndoAction.class);
            verify(manager).addUndoAction(action.capture());
            action.getValue().undo();
            action.getValue().redo();

            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, PROPERTY, "p. 5"));
            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, PROPERTY, "p. 7"), times(2));
            verify(manager).addUndoAction(action.getValue());
        }
    }

    @Test
    void removingAPropertyRecordsItsValueAndRedoRemovesItAgain() throws Exception {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY)).thenReturn(Optional.of("p. 5"));
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            UndoableDocumentProperties.remove(document, PROPERTY);
            ArgumentCaptor<XUndoAction> action = ArgumentCaptor.forClass(XUndoAction.class);
            verify(manager).addUndoAction(action.capture());
            action.getValue().undo();
            action.getValue().redo();

            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, PROPERTY, "p. 5"));
            properties.verify(() -> UnoUserDefinedProperty.removeIfExists(document, PROPERTY), times(2));
            verify(manager).addUndoAction(action.getValue());
        }
    }

    @Test
    void unchangedAndAbsentValuesDoNotCreateUndoActions() throws Exception {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY)).thenReturn(Optional.of("p. 5"), Optional.empty());
            UndoableDocumentProperties.set(document, PROPERTY, Optional.of("p. 5"));
            UndoableDocumentProperties.remove(document, PROPERTY);

            verifyNoInteractions(manager, document);
            properties.verify(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY), times(2));
            properties.verifyNoMoreInteractions();
        }
    }

    @Test
    void lockedUndoManagerDoesNotReceiveAnotherAction() throws Exception {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY)).thenReturn(Optional.empty());
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            when(manager.isLocked()).thenReturn(true);
            UndoableDocumentProperties.set(document, PROPERTY, Optional.of("p. 5"));

            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, PROPERTY, "p. 5"));
            verify(manager).isLocked();
            verifyNoMoreInteractions(manager);
        }
    }

    @Test
    void registrationFailureImmediatelyRestoresTheOriginalPropertyValue() throws Exception {
        com.sun.star.lang.IllegalArgumentException cause = new com.sun.star.lang.IllegalArgumentException("Cannot register undo");
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY)).thenReturn(Optional.of("p. 5"));
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            doThrow(cause).when(manager).addUndoAction(any());
            WrappedTargetException failure = assertThrows(WrappedTargetException.class,
                    () -> UndoableDocumentProperties.set(document, PROPERTY, Optional.of("p. 7")));
            assertSame(cause, failure.getCause());
            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, PROPERTY, "p. 5"));
        }
    }

    @Test
    void registrationFailureAfterRemovalImmediatelyRestoresTheOriginalProperty() throws Exception {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY)).thenReturn(Optional.of("p. 5"));
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            doThrow(new com.sun.star.lang.IllegalArgumentException("Cannot register undo")).when(manager).addUndoAction(any());
            assertThrows(WrappedTargetException.class, () -> UndoableDocumentProperties.remove(document, PROPERTY));
            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, PROPERTY, "p. 5"));
        }
    }

    @Test
    void failedImmediateCompensationIsSuppressedOnTheCheckedRegistrationFailure() throws Exception {
        com.sun.star.lang.IllegalArgumentException cause = new com.sun.star.lang.IllegalArgumentException("Cannot register undo");
        PropertyVetoException compensation = new PropertyVetoException("Cannot restore property");
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY)).thenReturn(Optional.of("p. 5"));
            properties.when(() -> UnoUserDefinedProperty.setStringProperty(document, PROPERTY, "p. 5")).thenThrow(compensation);
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            doThrow(cause).when(manager).addUndoAction(any());
            WrappedTargetException failure = assertThrows(WrappedTargetException.class,
                    () -> UndoableDocumentProperties.set(document, PROPERTY, Optional.of("p. 7")));
            assertSame(cause, failure.getCause());
            assertSame(compensation, failure.getSuppressed()[0]);
        }
    }

    @Test
    void replayFailureKeepsTheCheckedCauseInTheUnoUndoFailure() throws Exception {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, PROPERTY)).thenReturn(Optional.of("p. 5"));
            undo.when(() -> UnoUndo.getXUndoManager(document)).thenReturn(Optional.of(manager));
            UndoableDocumentProperties.set(document, PROPERTY, Optional.of("p. 7"));
            ArgumentCaptor<XUndoAction> action = ArgumentCaptor.forClass(XUndoAction.class);
            verify(manager).addUndoAction(action.capture());
            PropertyVetoException cause = new PropertyVetoException("Property is protected");
            properties.when(() -> UnoUserDefinedProperty.setStringProperty(document, PROPERTY, "p. 5")).thenThrow(cause);

            UndoFailedException failure = assertThrows(UndoFailedException.class, action.getValue()::undo);
            assertSame(cause, failure.getCause());
            assertSame(cause, failure.Reason);
        }
    }
}
