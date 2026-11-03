package org.jabref.logic.openoffice.backend;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.backend.WriterReferenceMark.BoundaryPolicy;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.DocumentAnnotation;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoCast;
import org.jabref.logic.openoffice.uno.UnoNameAccess;
import org.jabref.logic.openoffice.uno.UnoNamed;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Creates and finds Writer reference marks without interpreting citation names or formats.
@NullMarked
public final class WriterReferenceMarks {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriterReferenceMarks.class);

    public WriterReferenceMark create(XTextDocument doc, XComponentContext context, String name, XTextCursor position,
                                      boolean insertSpaceBefore, boolean insertSpaceAfter, BoundaryPolicy boundaryPolicy) throws CreationException {
        WriterReferenceMark mark = new WriterReferenceMark(name, boundaryPolicy);
        mark.createInDocument(doc, Optional.of(context), position, insertSpaceBefore, insertSpaceAfter);
        return mark;
    }

    public Optional<WriterReferenceMark> find(XTextDocument doc, String name, BoundaryPolicy boundaryPolicy)
            throws NoDocumentException, WrappedTargetException {
        return getAnchor(doc, name).map(_ -> new WriterReferenceMark(name, boundaryPolicy));
    }

    public List<String> names(XTextDocument doc) throws NoDocumentException {
        return getListOfNames(doc);
    }

    /// Also used to check whether a document connection is still working.
    public static XNameAccess getNameAccess(XTextDocument doc) throws NoDocumentException {
        XReferenceMarksSupplier supplier = UnoCast.cast(XReferenceMarksSupplier.class, doc)
                                                  .orElseThrow(() -> new NoDocumentException("Document does not supply reference marks"));
        try {
            return supplier.getReferenceMarks();
        } catch (DisposedException exception) {
            LOGGER.debug("Cannot access reference marks of a disposed document", exception);
            throw new NoDocumentException("Cannot access reference marks of a disposed document");
        }
    }

    public static List<String> getListOfNames(XTextDocument doc) throws NoDocumentException {
        return Optional.ofNullable(getNameAccess(doc).getElementNames()).map(List::of).orElseGet(List::of);
    }

    /// Removes the named reference mark and its text, if present.
    public static void removeIfExists(XTextDocument doc, String name) throws WrappedTargetException, NoDocumentException {
        Optional<XTextContent> mark = getAsTextContent(doc, name);
        if (mark.isEmpty()) {
            return;
        }
        try {
            doc.getText().removeTextContent(mark.orElseThrow());
        } catch (NoSuchElementException exception) {
            LOGGER.debug("Reference mark was already removed: {}", name, exception);
        }
    }

    public static Optional<XTextContent> getAsTextContent(XTextDocument doc, String name) throws NoDocumentException, WrappedTargetException {
        return UnoNameAccess.getTextContentByName(getNameAccess(doc), name);
    }

    public static Optional<XTextRange> getAnchor(XTextDocument doc, String name) throws NoDocumentException, WrappedTargetException {
        return getAsTextContent(doc, name).map(XTextContent::getAnchor);
    }

    /// Inserts a raw UNO reference mark; the annotation controls whether its text is absorbed.
    /// Writer permits duplicate names programmatically, so callers must ensure uniqueness.
    public static XNamed create(DocumentAnnotation documentAnnotation) throws CreationException {
        return UnoNamed.insertNamedTextContent("com.sun.star.text.ReferenceMark", documentAnnotation);
    }
}
