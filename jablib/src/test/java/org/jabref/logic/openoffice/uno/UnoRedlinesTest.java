package org.jabref.logic.openoffice.uno;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.document.XRedlinesSupplier;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@NullMarked
class UnoRedlinesTest {
    private final XTextDocument document = mock(XTextDocument.class,
            withSettings().extraInterfaces(XRedlinesSupplier.class, XPropertySet.class));
    private final XEnumeration changes = mock(XEnumeration.class);
    private final XText text = mock(XText.class, withSettings().extraInterfaces(XTextRangeCompare.class));
    private final Map<XTextRange, Integer> positions = new IdentityHashMap<>();
    private final XTextRange start = point(2);
    private final XTextRange end = point(4);
    private final XTextCursor anchoredDeletion = mock(XTextCursor.class);

    @BeforeEach
    void prepareDocumentAnchors() throws Exception {
        XEnumerationAccess access = mock(XEnumerationAccess.class);
        when(((XRedlinesSupplier) document).getRedlines()).thenReturn(access);
        when(access.createEnumeration()).thenReturn(changes);
        when(text.createTextCursorByRange(start)).thenReturn(anchoredDeletion);
        when(anchoredDeletion.getText()).thenReturn(text);
        when(anchoredDeletion.getEnd()).thenReturn(end);

        positions.put(anchoredDeletion, 2);
        when(((XTextRangeCompare) text).compareRegionStarts(any(XTextRange.class), any(XTextRange.class)))
                .thenAnswer(invocation -> (short) Integer.compare(position(invocation.getArgument(1)), position(invocation.getArgument(0))));
    }

    @Test
    void deletedRangesUseDocumentAnchorsInsteadOfDetachedDeletedText() throws Exception {
        XPropertySet change = deletion();
        XTextRange detached = (XTextRange) change;
        when(detached.getText()).thenReturn(mock(XText.class));
        enumerate(change);

        assertEquals(List.of(anchoredDeletion), UnoRedlines.getDeletedRanges(document));
        verify(anchoredDeletion).gotoRange(end, true);
        verify(detached, never()).getText();
        verify(detached, never()).getStart();
        verify(detached, never()).getEnd();
        verify((XPropertySet) document, never()).getPropertyValue("RecordChanges");
    }

    @ParameterizedTest
    @CsvSource({"0, 1, 0", "0, 2, 1", "1, 3, 1", "2, 4, 1", "3, 3, 1", "4, 6, 1", "5, 6, 0"})
    void deletionCountUsesAnchoredRangesAndIncludesTouchingBoundaries(int citationStart, int citationEnd, int expected) throws Exception {
        enumerate(deletion());
        XTextRange citation = point(citationStart);
        XTextRange citationEndRange = point(citationEnd);
        when(citation.getEnd()).thenReturn(citationEndRange);

        assertEquals(expected, UnoRedlines.countDeletedRangesTouching(document, List.of(citation)));
        verify((XPropertySet) document, never()).getPropertyValue("RecordChanges");
    }

    @Test
    void deletionIsCountedOnceWhenItTouchesMultipleCitations() throws Exception {
        enumerate(deletion());

        assertEquals(1, UnoRedlines.countDeletedRangesTouching(document, List.of(anchoredDeletion, anchoredDeletion)));
    }

    @Test
    void deletionInAnotherTextContainerDoesNotTouchCitation() throws Exception {
        enumerate(deletion());
        XTextRange citation = mock(XTextRange.class);
        when(citation.getText()).thenReturn(mock(XText.class));

        assertEquals(0, UnoRedlines.countDeletedRangesTouching(document, List.of(citation)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Insert", "Format"})
    void nonDeletionRedlinesDoNotRequireAnchors(String type) throws Exception {
        XPropertySet change = mock(XPropertySet.class);
        when(change.getPropertyValue("RedlineType")).thenReturn(type);
        enumerate(change);

        assertEquals(List.of(), UnoRedlines.getDeletedRanges(document));
        verify(change, never()).getPropertyValue("RedlineStart");
        verify(change, never()).getPropertyValue("RedlineEnd");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RedlineStart", "RedlineEnd"})
    void invalidDeletionAnchorFailsClosed(String property) throws Exception {
        XPropertySet change = deletion();
        when(change.getPropertyValue(property)).thenReturn(new Object());
        enumerate(change);

        assertThrows(NoDocumentException.class, () -> UnoRedlines.getDeletedRanges(document));
    }

    @Test
    void anchorsInDifferentTextContainersFailClosed() throws Exception {
        XPropertySet change = deletion();
        when(end.getText()).thenReturn(mock(XText.class));
        enumerate(change);

        assertThrows(NoDocumentException.class, () -> UnoRedlines.getDeletedRanges(document));
        verify(anchoredDeletion, never()).gotoRange(end, true);
    }

    @Test
    void unreadableAnchorFailsClosedAndRetainsCause() throws Exception {
        XPropertySet change = deletion();
        UnknownPropertyException cause = new UnknownPropertyException("RedlineEnd");
        when(change.getPropertyValue("RedlineEnd")).thenThrow(cause);
        enumerate(change);

        NoDocumentException error = assertThrows(NoDocumentException.class, () -> UnoRedlines.getDeletedRanges(document));

        assertSame(cause, error.getCause());
    }

    @Test
    void enumerationFailureDoesNotReturnPartialDeletionList() throws Exception {
        XPropertySet change = deletion();
        WrappedTargetException cause = new WrappedTargetException("Cannot read redline");
        when(changes.hasMoreElements()).thenReturn(true);
        when(changes.nextElement()).thenReturn(change).thenThrow(cause);

        NoDocumentException error = assertThrows(NoDocumentException.class, () -> UnoRedlines.getDeletedRanges(document));

        assertSame(cause, error.getCause());
    }

    @Test
    void inaccessibleRedlineCollectionFailsClosedAndRetainsCause() {
        com.sun.star.uno.RuntimeException cause = new com.sun.star.uno.RuntimeException("Cannot enumerate redlines");
        when(changes.hasMoreElements()).thenThrow(cause);

        NoDocumentException error = assertThrows(NoDocumentException.class, () -> UnoRedlines.getDeletedRanges(document));

        assertSame(cause, error.getCause());
    }

    @Test
    void missingRedlineCollectionFailsClosed() {
        XTextDocument missingCollection = mock(XTextDocument.class, withSettings().extraInterfaces(XRedlinesSupplier.class));

        assertThrows(NoDocumentException.class, () -> UnoRedlines.getDeletedRanges(missingCollection));
    }

    @Test
    void missingRedlinePropertiesFailClosed() throws Exception {
        enumerate(new Object());

        assertThrows(NoDocumentException.class, () -> UnoRedlines.getDeletedRanges(document));
    }

    @Test
    void documentWithoutRedlineSupplierHasNoDeletions() throws Exception {
        assertEquals(List.of(), UnoRedlines.getDeletedRanges(mock(XTextDocument.class)));
    }

    private XPropertySet deletion() throws Exception {
        XPropertySet change = mock(XPropertySet.class, withSettings().extraInterfaces(XTextRange.class));
        when(change.getPropertyValue("RedlineType")).thenReturn("Delete");
        when(change.getPropertyValue("RedlineStart")).thenReturn(start);
        when(change.getPropertyValue("RedlineEnd")).thenReturn(end);
        return change;
    }

    private void enumerate(Object change) throws Exception {
        when(changes.hasMoreElements()).thenReturn(true, false);
        when(changes.nextElement()).thenReturn(change);
    }

    private XTextRange point(int position) {
        XTextRange point = mock(XTextRange.class);
        when(point.getText()).thenReturn(text);
        positions.put(point, position);
        return point;
    }

    private int position(XTextRange range) {
        return Optional.ofNullable(positions.get(range)).orElseThrow();
    }
}
