package org.jabref.logic.openoffice.uno;

import java.util.Optional;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextContent;

/// Finds Writer text content in a UNO name-indexed collection.
public class NamedTextContentLookup {

    private NamedTextContentLookup() {
    }

    /// @return An empty optional if the name is absent or its object does not support `XTextContent`.
    public static Optional<XTextContent> getTextContentByName(XNameAccess nameAccess, String name)
            throws
            WrappedTargetException {
        try {
            return UnoInterfaces.query(XTextContent.class, nameAccess.getByName(name));
        } catch (NoSuchElementException _) {
            return Optional.empty();
        }
    }
}
