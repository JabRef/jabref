package org.jabref.model.openoffice.uno;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.ReferenceFieldPart;
import com.sun.star.text.ReferenceFieldSource;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.util.XRefreshable;

public class UnoCrossRef {

    private UnoCrossRef() { }

    /**
     * Update TextFields, etc. We use it to refresh cross-references in the document.
     */
    public static void refresh(XTextDocument doc) {
        // Refresh the document
        XRefreshable xRefresh = UnoCast.cast(XRefreshable.class, doc).get();
        xRefresh.refresh();
    }

    /**
     * Insert a clickable cross-reference to a reference mark, with a label containing the target's
     * page number.
     *
     * May need a documentConnection.refresh() after, to update the text shown.
     */
    public static void insertReferenceToPageNumberOfReferenceMark(XTextDocument doc,
                                                                  String referenceMarkName,
                                                                  XTextRange cursor)
        throws
        CreationException,
        WrappedTargetException {

        // based on: https://wiki.openoffice.org/wiki/Documentation/DevGuide/Text/Reference_Marks
        XMultiServiceFactory msf = UnoCast.cast(XMultiServiceFactory.class, doc).get();
        // Create a 'GetReference' text field to refer to the reference mark we just inserted,
        // and get it's XPropertySet interface
        XPropertySet xFieldProps;
        try {
            String name = "com.sun.star.text.textfield.GetReference";
            xFieldProps = UnoCast.cast(XPropertySet.class, msf.createInstance(name)).get();
        } catch (com.sun.star.uno.Exception e) {
            throw new CreationException(e.getMessage());
        }

        try {
            // Set the SourceName of the GetReference text field to the referenceMarkName
            xFieldProps.setPropertyValue("SourceName", referenceMarkName);
        } catch (UnknownPropertyException ex) {
            throw new java.lang.IllegalStateException("The created GetReference does not have property 'SourceName'");
        } catch (PropertyVetoException ex) {
            throw new java.lang.IllegalStateException("Caught PropertyVetoException on 'SourceName'");
        }

        try {
            // specify that the source is a reference mark (could also be a footnote,
            // bookmark or sequence field)
            xFieldProps.setPropertyValue("ReferenceFieldSource", Short.valueOf(ReferenceFieldSource.REFERENCE_MARK));
        } catch (UnknownPropertyException ex) {
            throw new java.lang.IllegalStateException("The created GetReference does not have property"
                                                      + " 'ReferenceFieldSource'");
        } catch (PropertyVetoException ex) {
            throw new java.lang.IllegalStateException("Caught PropertyVetoException on 'ReferenceFieldSource'");
        }

        try {
            // We want the reference displayed as page number
            xFieldProps.setPropertyValue("ReferenceFieldPart", Short.valueOf(ReferenceFieldPart.PAGE));
        } catch (UnknownPropertyException ex) {
            throw new java.lang.IllegalStateException("The created GetReference does not have property"
                                                      + " 'ReferenceFieldPart'");
        } catch (PropertyVetoException ex) {
            throw new java.lang.IllegalStateException("Caught PropertyVetoException on 'ReferenceFieldPart'");
        }

        // Get the XTextContent interface of the GetReference text field
        XTextContent xRefContent = UnoCast.cast(XTextContent.class, xFieldProps).get();

        // Insert the text field
        cursor.getText().insertTextContent(cursor.getEnd(), xRefContent, false);
    }
}
