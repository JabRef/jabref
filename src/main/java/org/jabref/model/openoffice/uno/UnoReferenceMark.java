package org.jabref.model.openoffice.uno;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public class UnoReferenceMark {

    private UnoReferenceMark() { }

    /**
     * @throws NoDocumentException If cannot get reference marks
     *
     * Note: also used by `isDocumentConnectionMissing` to test if we have a working connection.
     *
     */
    public static XNameAccess getNameAccess(XTextDocument doc)
        throws
        NoDocumentException {

        XReferenceMarksSupplier supplier = UnoCast.cast(XReferenceMarksSupplier.class, doc).get();

        try {
            return supplier.getReferenceMarks();
        } catch (DisposedException ex) {
            throw new NoDocumentException("UnoReferenceMarks.getNameAccess failed with" + ex);
        }
    }

    /**
     * Names of all reference marks.
     *
     * Empty list for nothing.
     */
    public static List<String> getListOfNames(XTextDocument doc)
        throws NoDocumentException {

        XNameAccess nameAccess = UnoReferenceMark.getNameAccess(doc);
        String[] names = nameAccess.getElementNames();
        if (names == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(names);
    }

    /**
     * Remove the named reference mark.
     *
     * Removes both the text and the mark itself.
     */
    public static void removeIfExists(XTextDocument doc, String name)
        throws
        WrappedTargetException,
        NoDocumentException {

        XNameAccess xReferenceMarks = UnoReferenceMark.getNameAccess(doc);

        if (xReferenceMarks.hasByName(name)) {
            Optional<XTextContent> mark = UnoNameAccess.getTextContentByName(xReferenceMarks, name);
            if (mark.isEmpty()) {
                return;
            }
            try {
                doc.getText().removeTextContent(mark.get());
            } catch (NoSuchElementException ex) {
                // The caller gets what it expects.
            }
        }
    }

    /**
     *  @return reference mark as XTextContent, Optional.empty if not found.
     */
    public static Optional<XTextContent> getAsTextContent(XTextDocument doc, String name)
        throws
        NoDocumentException,
        WrappedTargetException {

        XNameAccess nameAccess = UnoReferenceMark.getNameAccess(doc);
        return UnoNameAccess.getTextContentByName(nameAccess, name);
    }

    /**
     *  XTextRange for the named reference mark, Optional.empty if not found.
     */
    public static Optional<XTextRange> getAnchor(XTextDocument doc, String name)
        throws
        NoDocumentException,
        WrappedTargetException {
        return (UnoReferenceMark.getAsTextContent(doc, name)
                .map(XTextContent::getAnchor));
    }

    /**
     * Insert a new reference mark at the provided cursor position.
     *
     * If {@code absorb} is true, the text in the cursor range will become the text with gray
     * background.
     *
     * Note: LibreOffice 6.4.6.2 will create multiple reference marks with the same name without
     *       error or renaming.
     *       Its GUI does not allow this, but we can create them programmatically.
     *       In the GUI, clicking on any of those identical names will move the cursor to the same
     *       mark.
     *
     * @param name     For the reference mark.
     * @param range Cursor marking the location or range for the reference mark.
     */
    public static XNamed create(XTextDocument doc, String name, XTextRange range, boolean absorb)
        throws
        CreationException {
        return UnoNamed.insertNamedTextContent(doc, "com.sun.star.text.ReferenceMark", name, range, absorb);
    }
}
