package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public class UnoBookmark {

    private UnoBookmark() { }

    /**
     * Provides access to bookmarks by name.
     */
    public static XNameAccess getNameAccess(XTextDocument doc)
        throws
        NoDocumentException {

        XBookmarksSupplier supplier = UnoCast.cast(XBookmarksSupplier.class, doc).get();
        try {
            return supplier.getBookmarks();
        } catch (DisposedException ex) {
            throw new NoDocumentException("UnoBookmark.getNameAccess failed with" + ex);
        }
    }

    /**
     * Get the XTextRange corresponding to the named bookmark.
     *
     * @param name The name of the bookmark to find.
     * @return The XTextRange for the bookmark, or Optional.empty().
     */
    public static Optional<XTextRange> getAnchor(XTextDocument doc, String name)
        throws
        WrappedTargetException,
        NoDocumentException {

        XNameAccess nameAccess = getNameAccess(doc);
        return (UnoNameAccess.getTextContentByName(nameAccess, name).map(XTextContent::getAnchor));
    }

    /**
     * Insert a bookmark with the given name at the cursor provided, or with another name if the one
     * we asked for is already in use.
     *
     * In LibreOffice the another name is in "{name}{number}" format.
     *
     * @param name     For the bookmark.
     * @param range    Cursor marking the location or range for the bookmark.
     * @param absorb   Shall we incorporate range?
     *
     * @return The XNamed interface of the bookmark.
     *
     *         result.getName() should be checked by the caller, because its name may differ from
     *         the one requested.
     */
    public static XNamed create(XTextDocument doc, String name, XTextRange range, boolean absorb)
        throws
        CreationException {
        return UnoNamed.insertNamedTextContent(doc, "com.sun.star.text.Bookmark", name, range, absorb);
    }

    /**
     * Remove the named bookmark if it exists.
     */
    public static void removeIfExists(XTextDocument doc, String name)
        throws
        NoDocumentException,
        WrappedTargetException {

        XNameAccess marks = UnoBookmark.getNameAccess(doc);

        if (marks.hasByName(name)) {
            Optional<XTextContent> mark = UnoNameAccess.getTextContentByName(marks, name);
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
}
