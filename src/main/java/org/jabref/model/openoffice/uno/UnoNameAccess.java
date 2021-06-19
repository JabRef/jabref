package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextContent;

public class UnoNameAccess {

    private UnoNameAccess() { }

    /**
     * @return null if name not found, or if the result does not support the XTextContent interface.
     */
    public static Optional<XTextContent> getTextContentByName(XNameAccess nameAccess, String name)
        throws
        WrappedTargetException {
        try {
            return UnoCast.cast(XTextContent.class, nameAccess.getByName(name));
        } catch (NoSuchElementException ex) {
            return Optional.empty();
        }
    }
}
