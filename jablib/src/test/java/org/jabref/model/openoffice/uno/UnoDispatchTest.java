package org.jabref.model.openoffice.uno;

import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class UnoDispatchTest {

    @Test
    void executeSwallowsDispatchCreationFailures() throws com.sun.star.uno.Exception {
        XComponentContext context = mock(XComponentContext.class);
        XMultiComponentFactory serviceManager = mock(XMultiComponentFactory.class);
        XTextDocument document = mock(XTextDocument.class);

        when(context.getServiceManager()).thenReturn(serviceManager);
        when(serviceManager.createInstanceWithContext(eq("com.sun.star.frame.DispatchHelper"), eq(context)))
                .thenThrow(new com.sun.star.uno.Exception("boom"));

        assertDoesNotThrow(() -> UnoDispatch.execute(context, document, ".uno:ResetAttributes", new PropertyValue[0]));
    }

    @Test
    void resetAttributesAtRangeEndSwallowsMissingViewCursor() {
        XComponentContext context = mock(XComponentContext.class);
        XTextDocument document = mock(XTextDocument.class);
        XTextRange endRange = mock(XTextRange.class);

        assertDoesNotThrow(() -> UnoDispatch.resetAttributesAtRangeEnd(context, document, endRange));
    }

    @Test
    void resetAttributesAtRangeEndSwallowsDisposedCursorComparison() {
        XComponentContext context = mock(XComponentContext.class);
        XTextDocument document = mock(XTextDocument.class);
        XTextCursor cursor = mock(XTextCursor.class);
        XText text = mock(XText.class);
        XTextRange endRange = mock(XTextRange.class);

        when(cursor.getText()).thenReturn(text);
        when(cursor.isCollapsed()).thenThrow(new com.sun.star.uno.RuntimeException("disposed?"));

        assertDoesNotThrow(() -> UnoDispatch.resetAttributesAtRangeEnd(context, document, cursor, endRange));
    }
}
