package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.document.XRedlinesSupplier;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;

/**
 *  Change tracking and Redlines
 */
public class UnoRedlines {

    public static boolean getRecordChanges(XTextDocument doc)
        throws
        WrappedTargetException {

        // https://wiki.openoffice.org/wiki/Documentation/DevGuide/Text/Settings
        // "Properties of com.sun.star.text.TextDocument"

        XPropertySet propertySet = UnoCast.cast(XPropertySet.class, doc).get();

        try {
            return (boolean) propertySet.getPropertyValue("RecordChanges");
        } catch (UnknownPropertyException ex) {
            throw new java.lang.IllegalStateException("Caught UnknownPropertyException on 'RecordChanges'");
        }
    }

    private static Optional<XRedlinesSupplier> getRedlinesSupplier(XTextDocument doc) {
        return UnoCast.cast(XRedlinesSupplier.class, doc);
    }

    public static int countRedlines(XTextDocument doc) {
        Optional<XRedlinesSupplier> supplier = getRedlinesSupplier(doc);
        if (supplier.isEmpty()) {
            return 0;
        }
        XEnumerationAccess enumerationAccess = supplier.get().getRedlines();
        XEnumeration enumeration = enumerationAccess.createEnumeration();
        if (enumeration == null) {
            return 0;
        } else {
            int count = 0;
            while (enumeration.hasMoreElements()) {
                try {
                    enumeration.nextElement();
                    count++;
                } catch (NoSuchElementException | WrappedTargetException ex) {
                    break;
                }
            }
            return count;
        }
    }
}
