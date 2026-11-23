package org.jabref.logic.openoffice.uno;

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

    private UnoBookmark() {
    }

    /// Provides access to bookmarks by name.
    public static XNameAccess getNameAccess(XTextDocument doc)
            throws
            WriterDocumentAccessException {

        XBookmarksSupplier supplier = UnoInterfaces.query(XBookmarksSupplier.class, doc).get();
        try {
            return supplier.getBookmarks();
        } catch (DisposedException ex) {
            throw new WriterDocumentAccessException("UnoBookmark.getNameAccess failed with" + ex);
        }
    }

    /// Get the XTextRange corresponding to the named bookmark.
    ///
    /// @param name The name of the bookmark to find.
    /// @return The XTextRange for the bookmark, or Optional.empty().
    public static Optional<XTextRange> getAnchor(XTextDocument doc, String name)
            throws
            WrappedTargetException,
            WriterDocumentAccessException {

        XNameAccess nameAccess = getNameAccess(doc);
        return NamedTextContentLookup.getTextContentByName(nameAccess, name).map(XTextContent::getAnchor);
    }

    /// Insert a bookmark with the given name at the cursor provided, or with another name if the one we asked for is already in use.
    ///
    /// In LibreOffice the another name is in "{name}{number}" format.
    ///
    /// @return The XNamed interface of the bookmark.
    /// result.getName() should be checked by the caller, because its name may differ from the one requested.
    public static XNamed create(NamedTextContentPlacement placement)
            throws
            WriterOperationException {
        return NamedTextContentInserter.insertNamedTextContent("com.sun.star.text.Bookmark", placement);
    }

    /// Remove the named bookmark if it exists.
    public static void removeIfExists(XTextDocument doc, String name)
            throws
            WriterDocumentAccessException,
            WrappedTargetException {

        XNameAccess marks = UnoBookmark.getNameAccess(doc);

        if (marks.hasByName(name)) {
            Optional<XTextContent> mark = NamedTextContentLookup.getTextContentByName(marks, name);
            if (mark.isEmpty()) {
                return;
            }
            try {
                doc.getText().removeTextContent(mark.get());
            } catch (NoSuchElementException _) {
                // The caller gets what it expects.
            }
        }
    }
}
