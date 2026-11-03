package org.jabref.logic.openoffice.backend;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.backend.WriterReferenceMark.BoundaryPolicy;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.DocumentAnnotation;
import org.jabref.logic.openoffice.uno.NoDocumentException;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@NullMarked
class WriterReferenceMarkTest {
    private static final String NAME = "uninterpreted name";
    private static final String BOUNDARY = "\u200b";

    private final XTextDocument document = mock(XTextDocument.class, withSettings().extraInterfaces(XReferenceMarksSupplier.class));
    private final XNameAccess names = mock(XNameAccess.class);
    private final XTextContent content = mock(XTextContent.class);
    private final XTextRange anchor = mock(XTextRange.class);
    private final XText text = mock(XText.class);
    private final XTextCursor full = mock(XTextCursor.class);
    private final XTextRange start = mock(XTextRange.class);
    private final XTextRange end = mock(XTextRange.class);
    private final XTextCursor left = mock(XTextCursor.class);
    private final XTextCursor right = mock(XTextCursor.class);
    private final WriterReferenceMarks marks = new WriterReferenceMarks();

    @BeforeEach
    void prepareDocument() throws Exception {
        when(((XReferenceMarksSupplier) document).getReferenceMarks()).thenReturn(names);
        when(names.getByName(NAME)).thenReturn(content);
        when(content.getAnchor()).thenReturn(anchor);
        when(anchor.getText()).thenReturn(text);
        when(text.createTextCursorByRange(anchor)).thenReturn(full);
        when(full.getText()).thenReturn(text);
        when(full.getStart()).thenReturn(start);
        when(full.getEnd()).thenReturn(end);
        when(text.createTextCursorByRange(start)).thenReturn(left);
        when(text.createTextCursorByRange(end)).thenReturn(right);
    }

    @ParameterizedTest
    @CsvSource({"PERSISTENT, '', false, false", "PERSISTENT, a, false, false", "PERSISTENT, ab, false, false",
            "TEMPORARY, '', false, false", "TEMPORARY, a, false, true", "TEMPORARY, ab, true, true"})
    void cleanupUsesExplicitPolicyAndRetainsSafeMinimumLength(BoundaryPolicy policy, String rendered, boolean removeLeft, boolean removeRight) throws Exception {
        when(full.getString()).thenReturn(BOUNDARY + rendered + BOUNDARY);
        WriterReferenceMark mark = marks.find(document, NAME, policy).orElseThrow();

        mark.cleanFillCursor(document);

        verify(left, removeLeft ? times(1) : never()).setString("");
        verify(right, removeRight ? times(1) : never()).setString("");
        verify(full, never()).setString(anyString());
    }

    @ParameterizedTest
    @EnumSource(BoundaryPolicy.class)
    void boundedFillSelectsOnlyInterior(BoundaryPolicy policy) throws Exception {
        when(full.getString()).thenReturn(BOUNDARY + "old" + BOUNDARY);
        WriterReferenceMark mark = marks.find(document, NAME, policy).orElseThrow();

        assertEquals(left, mark.getFillCursor(document));
        verify(right).goLeft((short) 1, false);
        verify(left).goRight((short) 1, false);
        verify(left).gotoRange(right, true);
        verify(left).setString("");
        verify(full, never()).setString(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "ab", "\u200b"})
    void persistentFillRejectsMissingBoundariesWithoutMutation(String raw) throws Exception {
        when(full.getString()).thenReturn(raw);
        WriterReferenceMark mark = marks.find(document, NAME, BoundaryPolicy.PERSISTENT).orElseThrow();

        CreationException exception = assertThrows(CreationException.class, () -> mark.getFillCursor(document));
        assertEquals("Reference mark has lost its boundary characters: " + NAME, exception.getMessage());
        verify(full, never()).setString(anyString());
        verifyNoInteractions(left, right);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "ab", "\u200b"})
    void temporaryCleanupReportsDamagedBoundariesAsCheckedFailure(String raw) throws Exception {
        when(full.getString()).thenReturn(raw);
        WriterReferenceMark mark = marks.find(document, NAME, BoundaryPolicy.TEMPORARY).orElseThrow();

        NoDocumentException exception = assertThrows(NoDocumentException.class, () -> mark.cleanFillCursor(document));
        assertEquals("Reference mark has lost its boundary characters: " + NAME, exception.getMessage());
        verifyNoInteractions(left, right);
    }

    @ParameterizedTest
    @EnumSource(BoundaryPolicy.class)
    void missingMarkHasNoRawCursorAndFillReportsCheckedFailure(BoundaryPolicy policy) throws Exception {
        WriterReferenceMark mark = marks.find(document, NAME, policy).orElseThrow();
        when(names.getByName(NAME)).thenThrow(new NoSuchElementException());

        assertEquals(Optional.empty(), mark.getRawCursor(document));
        assertEquals(Optional.empty(), marks.find(document, NAME, policy));
        NoDocumentException exception = assertThrows(NoDocumentException.class, () -> mark.getFillCursor(document));
        assertEquals("Missing reference mark cursor: " + NAME, exception.getMessage());
    }

    @Test
    void namesAndLookupDoNotInterpretCitationFamilies() throws Exception {
        List<String> rawNames = List.of("JABREF_key NORMAL uid", "ZOTERO_ITEM CSL_CITATION native", NAME);
        when(names.getElementNames()).thenReturn(rawNames.toArray(String[]::new));
        assertEquals(rawNames, marks.names(document));
        assertEquals(NAME, marks.find(document, NAME, BoundaryPolicy.PERSISTENT).orElseThrow().getRangeName());
    }

    @Test
    void absentNameArrayProducesEmptyList() throws Exception {
        assertEquals(List.of(), marks.names(document));
    }

    @Test
    void missingSupplierProducesCheckedFailure() {
        assertThrows(NoDocumentException.class, () -> WriterReferenceMarks.getNameAccess(mock(XTextDocument.class)));
    }

    @Test
    void rawCreationWithoutServiceFactoryProducesCheckedFailure() {
        XTextDocument unavailable = mock(XTextDocument.class);
        CreationException exception = assertThrows(CreationException.class,
                () -> WriterReferenceMarks.create(new DocumentAnnotation(unavailable, NAME, anchor, true)));
        assertEquals("Document does not supply a service factory", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rawCreationWithMissingServiceInterfaceProducesCheckedFailure(boolean named) throws Exception {
        XTextDocument factory = mock(XTextDocument.class, withSettings().extraInterfaces(XMultiServiceFactory.class));
        when(((XMultiServiceFactory) factory).createInstance("com.sun.star.text.ReferenceMark"))
                .thenReturn(named ? mock(XNamed.class) : new Object());
        CreationException exception = assertThrows(CreationException.class,
                () -> WriterReferenceMarks.create(new DocumentAnnotation(factory, NAME, anchor, true)));
        assertEquals("Service is not an " + (named ? "XTextContent" : "XNamed") + ": com.sun.star.text.ReferenceMark", exception.getMessage());
    }

    @Test
    void rawCreationWithUnavailableServiceProducesCheckedFailure() throws Exception {
        XTextDocument factory = mock(XTextDocument.class, withSettings().extraInterfaces(XMultiServiceFactory.class));
        when(((XMultiServiceFactory) factory).createInstance("com.sun.star.text.ReferenceMark"))
                .thenThrow(new com.sun.star.uno.Exception("Unavailable service"));
        CreationException exception = assertThrows(CreationException.class,
                () -> WriterReferenceMarks.create(new DocumentAnnotation(factory, NAME, anchor, true)));
        assertEquals("Cannot create document service: com.sun.star.text.ReferenceMark", exception.getMessage());
    }

    @Test
    void disposedDocumentProducesCheckedFailure() {
        when(((XReferenceMarksSupplier) document).getReferenceMarks()).thenThrow(new DisposedException());
        assertThrows(NoDocumentException.class, () -> WriterReferenceMarks.getNameAccess(document));
    }
}
