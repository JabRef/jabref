package org.jabref.logic.openoffice.backend;

import java.util.regex.Pattern;

import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoCast;
import org.jabref.logic.openoffice.uno.UnoDispatch;
import org.jabref.model.openoffice.ootext.OOText;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class ReferenceMarkTextWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceMarkTextWriter.class);
    private static final Pattern FULL_SUPERSCRIPT = Pattern.compile("^(?:<span[^>]*>)?<sup>.*</sup>(?:</span>)?$", Pattern.DOTALL);
    private static final Pattern FULL_SUBSCRIPT = Pattern.compile("^(?:<span[^>]*>)?<sub>.*</sub>(?:</span>)?$", Pattern.DOTALL);

    private ReferenceMarkTextWriter() {
    }

    public static void write(XTextDocument document, XComponentContext context, WriterReferenceMark range, OOText text)
            throws NoDocumentException, CreationException, WrappedTargetException {
        XTextCursor raw = range.getRawCursor(document)
                               .orElseThrow(() -> new NoDocumentException("Missing reference mark cursor: " + range.getRangeName()));
        OOTextWriter.removeEscapementFormatting(raw);
        XTextCursor fill = range.getFillCursor(document);
        try (FillCursorCleanup ignored = new FillCursorCleanup(document, range)) {
            OOTextWriter.removeEscapementFormatting(fill);
            OOTextWriter.write(document, fill, text);
        }
        XTextCursor full = range.getRawCursor(document)
                                .orElseThrow(() -> new NoDocumentException("Reference mark disappeared while writing: " + range.getRangeName()));
        restoreEscapement(text, full);
        UnoDispatch.resetAttributesAtRangeEnd(document, context, full.getEnd());
    }

    private static void restoreEscapement(OOText text, XTextCursor cursor) throws CreationException, WrappedTargetException {
        boolean superscript = FULL_SUPERSCRIPT.matcher(text.toString()).matches();
        boolean subscript = FULL_SUBSCRIPT.matcher(text.toString()).matches();
        if (!superscript && !subscript) {
            return;
        }
        XPropertySet properties = UnoCast.cast(XPropertySet.class, cursor)
                                         .orElseThrow(() -> new CreationException("Cannot restore reference mark escapement"));
        try {
            properties.setPropertyValue("CharEscapement", superscript ? (short) 33 : (short) -10);
            properties.setPropertyValue("CharEscapementHeight", (byte) 58);
            properties.setPropertyValue("CharAutoEscapement", false);
        } catch (UnknownPropertyException | PropertyVetoException | com.sun.star.lang.IllegalArgumentException exception) {
            LOGGER.warn("Could not restore reference mark escapement", exception);
            CreationException failure = new CreationException("Could not restore reference mark escapement");
            failure.initCause(exception);
            throw failure;
        }
    }

    @NullMarked
    private record FillCursorCleanup(XTextDocument document, WriterReferenceMark range) implements AutoCloseable {
        @Override
        public void close() throws NoDocumentException, WrappedTargetException {
            range.cleanFillCursor(document);
        }
    }
}
