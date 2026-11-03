package org.jabref.logic.openoffice.engine.storage;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.backend.ReferenceMarkTextWriter;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;

import com.sun.star.document.UndoManagerEvent;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerListener;
import com.sun.star.document.XUndoManagerSupplier;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifyBroadcaster;
import com.sun.star.util.XModifyListener;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@NullMarked
class MarkWriterTest {
    private final XTextDocument document = mock(XTextDocument.class,
            withSettings().extraInterfaces(XModifyBroadcaster.class, XUndoManagerSupplier.class));
    private final XUndoManager undoManager = mock(XUndoManager.class);
    private final XComponentContext context = mock(XComponentContext.class);
    private final WriterReferenceMark range = mock(WriterReferenceMark.class);
    private final MarkStore store = mock(MarkStore.class);
    private final CitationGroup group = new CitationGroup(new CitationGroupId("uid"), CitationType.AUTHORYEAR_PAR,
            List.of(new Citation("alpha")), Optional.of("JABREF_alpha NORMAL uid"));
    private final OOText marker = OOText.fromString("A");
    private MarkWriter writer;
    private XModifyListener modifications;
    private XUndoManagerListener undo;

    @BeforeEach
    void createWriter() throws Exception {
        when(((XUndoManagerSupplier) document).getUndoManager()).thenReturn(undoManager);
        when(store.getMarkData(group)).thenReturn(Optional.of(new JabRefMarkData(List.of("alpha"), "uid", CitationType.AUTHORYEAR_PAR)));
        when(store.referenceMark(group)).thenReturn(range);
        writer = new MarkWriter(document, context);
        ArgumentCaptor<XModifyListener> modifyListener = ArgumentCaptor.forClass(XModifyListener.class);
        verify((XModifyBroadcaster) document).addModifyListener(modifyListener.capture());
        modifications = modifyListener.getValue();
        ArgumentCaptor<XUndoManagerListener> undoListener = ArgumentCaptor.forClass(XUndoManagerListener.class);
        verify(undoManager).addUndoManagerListener(undoListener.capture());
        undo = undoListener.getValue();
    }

    @AfterEach
    void closeWriter() {
        writer.close();
    }

    @Test
    void externalModificationInvalidatesAnOtherwiseReusableFormattedBaseline() throws Exception {
        assertTrue(write());
        assertFalse(write());
        modifications.modified(new EventObject(document));
        assertTrue(write());
        assertFalse(write());
        verify(range, never()).getRawCursor(document);
    }

    @Test
    void nestedFullPassScopesSuppressOwnModifyAndUndoActionAddedEvents() throws Exception {
        writer.beginUpdate();
        writer.beginUpdate();
        try {
            assertTrue(write());
            modifications.modified(new EventObject(document));
            undo.undoActionAdded(new UndoManagerEvent());
        } finally {
            writer.endUpdate();
        }
        try {
            modifications.modified(new EventObject(document));
            assertFalse(write());
        } finally {
            writer.endUpdate();
        }
        assertFalse(write());
        modifications.modified(new EventObject(document));
        assertTrue(write());
    }

    @Test
    void undoAndRedoInvalidateEvenWhileAnUpdateScopeIsOpen() throws Exception {
        assertTrue(write());
        writer.beginUpdate();
        try {
            undo.actionUndone(new UndoManagerEvent());
            assertTrue(write());
            undo.actionRedone(new UndoManagerEvent());
            assertTrue(write());
        } finally {
            writer.endUpdate();
        }
        assertFalse(write());
    }

    @Test
    void clearInvalidatesWithoutDetachingAndCloseDetachesExactlyOnce() throws Exception {
        assertTrue(write());
        writer.clear();
        verify((XModifyBroadcaster) document, never()).removeModifyListener(modifications);
        verify(undoManager, never()).removeUndoManagerListener(undo);
        assertTrue(write());
        writer.close();
        writer.close();
        verify((XModifyBroadcaster) document).removeModifyListener(modifications);
        verify(undoManager).removeUndoManagerListener(undo);
        assertThrows(CreationException.class, this::write);
    }

    @Test
    void disposingTheDocumentMakesTheWriterUnusable() throws Exception {
        assertTrue(write());
        modifications.disposing(new EventObject(document));
        assertThrows(CreationException.class, this::write);
    }

    @Test
    void delegatesUnmodifiedFormattedTextAndCachesOnlySuccessfulWrites() throws Exception {
        OOText formatted = OOText.fromString("<span lang=\"en\"><sup>1</sup></span>");
        CreationException failure = new CreationException("Writing failed");
        try (MockedStatic<ReferenceMarkTextWriter> textWriter = mockStatic(ReferenceMarkTextWriter.class)) {
            textWriter.when(() -> ReferenceMarkTextWriter.write(document, context, range, formatted)).thenThrow(failure);
            assertSame(failure, assertThrows(CreationException.class, () -> writer.write(store, group, formatted)));
            textWriter.when(() -> ReferenceMarkTextWriter.write(document, context, range, formatted)).thenAnswer(_ -> null);

            assertTrue(writer.write(store, group, formatted));
            assertFalse(writer.write(store, group, formatted));
            textWriter.verify(() -> ReferenceMarkTextWriter.write(document, context, range, formatted), times(2));
        }
    }

    @Test
    void writerKeepsStoredInvisibleCitationsEmptyEvenWhenRendererReturnsText() throws Exception {
        CitationGroup invisible = new CitationGroup(new CitationGroupId("hidden"), CitationType.INVISIBLE_CIT,
                List.of(new Citation("alpha")), Optional.empty());
        when(store.getMarkData(invisible)).thenReturn(Optional.of(new JabRefMarkData(List.of("alpha"), "hidden", CitationType.INVISIBLE_CIT)));
        when(store.referenceMark(invisible)).thenReturn(range);
        try (MockedStatic<ReferenceMarkTextWriter> textWriter = mockStatic(ReferenceMarkTextWriter.class)) {
            assertTrue(writer.write(store, invisible, marker));
            assertFalse(writer.write(store, invisible, OOText.fromString("Different rendered text")));
            textWriter.verify(() -> ReferenceMarkTextWriter.write(document, context, range, OOText.fromString("")));
            textWriter.verifyNoMoreInteractions();
        }
    }

    private boolean write() throws Exception {
        try (MockedStatic<ReferenceMarkTextWriter> ignored = mockStatic(ReferenceMarkTextWriter.class)) {
            return writer.write(store, group, marker);
        }
    }
}
