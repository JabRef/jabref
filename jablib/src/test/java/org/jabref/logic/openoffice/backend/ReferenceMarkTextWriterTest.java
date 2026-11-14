package org.jabref.logic.openoffice.backend;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoDispatch;
import org.jabref.model.openoffice.ootext.OOText;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@NullMarked
class ReferenceMarkTextWriterTest {

    private final XTextDocument document = mock(XTextDocument.class);
    private final XComponentContext context = mock(XComponentContext.class);
    private final WriterReferenceMark range = mock(WriterReferenceMark.class);
    private final XTextCursor raw = mock(XTextCursor.class);
    private final XTextCursor fill = mock(XTextCursor.class);
    private final XTextCursor full = mock(XTextCursor.class, withSettings().extraInterfaces(XPropertySet.class));
    private final XTextRange end = mock(XTextRange.class);
    private MockedStatic<OOTextWriter> textWriter;
    private MockedStatic<UnoDispatch> dispatch;

    @BeforeEach
    void prepareRange() throws Exception {
        when(range.getRawCursor(document)).thenReturn(Optional.of(raw)).thenReturn(Optional.of(full));
        when(range.getFillCursor(document)).thenReturn(fill);
        when(range.getRangeName()).thenReturn("uninterpreted name");
        when(full.getEnd()).thenReturn(end);
        textWriter = mockStatic(OOTextWriter.class);
        dispatch = mockStatic(UnoDispatch.class);
    }

    @AfterEach
    void closeStaticMocks() {
        dispatch.close();
        textWriter.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A", "<i>A</i>", "<b>A &amp; B</b>", "prefix <sup>1</sup>", "<sub>1</sub> suffix"})
    void writesOnlySuppliedFormattedTextAndResetsAtRefreshedRangeEnd(String markup) throws Exception {
        OOText text = OOText.fromString(markup);
        textWriter.when(() -> OOTextWriter.write(document, fill, text)).thenAnswer(_ -> {
            textWriter.verify(() -> OOTextWriter.removeEscapementFormatting(raw));
            textWriter.verify(() -> OOTextWriter.removeEscapementFormatting(fill));
            verify(range, never()).cleanFillCursor(document);
            return null;
        });

        ReferenceMarkTextWriter.write(document, context, range, text);

        InOrder order = inOrder(range);
        order.verify(range).getRawCursor(document);
        order.verify(range).getFillCursor(document);
        order.verify(range).cleanFillCursor(document);
        order.verify(range).getRawCursor(document);
        textWriter.verify(() -> OOTextWriter.write(document, fill, text));
        verify((XPropertySet) full, never()).setPropertyValue(anyString(), any());
        dispatch.verify(() -> UnoDispatch.resetAttributesAtRangeEnd(document, context, end));
    }

    @ParameterizedTest
    @CsvSource({"<sup>1</sup>, 33", "<sub>1</sub>, -10", "<span lang=\"en\"><sup>12</sup></span>, 33",
            "<span oo:CharStyleName=\"Citation\"><sub>1</sub></span>, -10"})
    void restoresWholeMarkEscapementWithoutChangingFontHeight(String markup, short escapement) throws Exception {
        ReferenceMarkTextWriter.write(document, context, range, OOText.fromString(markup));

        XPropertySet properties = (XPropertySet) full;
        verify(properties).setPropertyValue("CharEscapement", escapement);
        verify(properties).setPropertyValue("CharEscapementHeight", (byte) 58);
        verify(properties).setPropertyValue("CharAutoEscapement", false);
        verify(properties, never()).setPropertyValue(eq("CharHeight"), any());
        dispatch.verify(() -> UnoDispatch.resetAttributesAtRangeEnd(document, context, end));
    }

    @Test
    void cleansBoundariesWhenWritingFailsWithoutResettingEndAttributes() throws Exception {
        OOText text = OOText.fromString("<i>A</i>");
        CreationException failure = new CreationException("Write failed");
        textWriter.when(() -> OOTextWriter.write(document, fill, text)).thenThrow(failure);

        assertSame(failure, assertThrows(CreationException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, text)));
        verify(range).cleanFillCursor(document);
        verify(range).getRawCursor(document);
        dispatch.verifyNoInteractions();
    }

    @Test
    void cleansBoundariesWhenFillFormattingResetFails() throws Exception {
        com.sun.star.uno.RuntimeException failure = new com.sun.star.uno.RuntimeException("Reset failed");
        textWriter.when(() -> OOTextWriter.removeEscapementFormatting(fill)).thenThrow(failure);

        assertSame(failure, assertThrows(com.sun.star.uno.RuntimeException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, OOText.fromString("A"))));
        verify(range).cleanFillCursor(document);
        dispatch.verifyNoInteractions();
    }

    @Test
    void preservesWriteFailureWhenBoundaryCleanupAlsoFails() throws Exception {
        OOText text = OOText.fromString("A");
        CreationException writeFailure = new CreationException("Write failed");
        NoDocumentException cleanupFailure = new NoDocumentException("Cleanup failed");
        textWriter.when(() -> OOTextWriter.write(document, fill, text)).thenThrow(writeFailure);
        doThrow(cleanupFailure).when(range).cleanFillCursor(document);

        CreationException actual = assertThrows(CreationException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, text));

        assertSame(writeFailure, actual);
        assertEquals(List.of(cleanupFailure), List.of(actual.getSuppressed()));
        dispatch.verifyNoInteractions();
    }

    @Test
    void reportsBoundaryCleanupFailureAfterSuccessfulWrite() throws Exception {
        NoDocumentException failure = new NoDocumentException("Cleanup failed");
        doThrow(failure).when(range).cleanFillCursor(document);

        assertSame(failure, assertThrows(NoDocumentException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, OOText.fromString("A"))));
        dispatch.verifyNoInteractions();
    }

    @Test
    void missingMarkFailsBeforeWriting() throws Exception {
        when(range.getRawCursor(document)).thenReturn(Optional.empty());

        NoDocumentException failure = assertThrows(NoDocumentException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, OOText.fromString("A")));

        assertEquals("Missing reference mark cursor: uninterpreted name", failure.getMessage());
        textWriter.verifyNoInteractions();
        dispatch.verifyNoInteractions();
        verify(range, never()).getFillCursor(document);
        verify(range, never()).cleanFillCursor(document);
    }

    @Test
    void failedFillPreparationDoesNotAttemptBoundaryCleanup() throws Exception {
        CreationException failure = new CreationException("Damaged boundaries");
        when(range.getFillCursor(document)).thenThrow(failure);

        assertSame(failure, assertThrows(CreationException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, OOText.fromString("A"))));
        verify(range, never()).cleanFillCursor(document);
        dispatch.verifyNoInteractions();
    }

    @Test
    void disappearingMarkAfterWritingIsReported() throws Exception {
        when(range.getRawCursor(document)).thenReturn(Optional.of(raw)).thenReturn(Optional.empty());

        NoDocumentException failure = assertThrows(NoDocumentException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, OOText.fromString("A")));

        assertEquals("Reference mark disappeared while writing: uninterpreted name", failure.getMessage());
        verify(range).cleanFillCursor(document);
        dispatch.verifyNoInteractions();
    }

    @Test
    void missingEscapementPropertiesAreReported() throws Exception {
        when(range.getRawCursor(document)).thenReturn(Optional.of(raw));

        CreationException failure = assertThrows(CreationException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, OOText.fromString("<sup>1</sup>")));

        assertEquals("Cannot restore reference mark escapement", failure.getMessage());
        verify(range).cleanFillCursor(document);
        verify(range, times(2)).getRawCursor(document);
        verifyNoInteractions((XPropertySet) full);
        dispatch.verifyNoInteractions();
    }

    @Test
    void escapementPropertyFailuresRetainTheirCause() throws Exception {
        PropertyVetoException cause = new PropertyVetoException("Read-only escapement");
        doThrow(cause).when((XPropertySet) full).setPropertyValue("CharEscapement", (short) 33);

        CreationException failure = assertThrows(CreationException.class,
                () -> ReferenceMarkTextWriter.write(document, context, range, OOText.fromString("<sup>1</sup>")));

        assertSame(cause, failure.getCause());
        verify(range).cleanFillCursor(document);
        dispatch.verifyNoInteractions();
    }
}
