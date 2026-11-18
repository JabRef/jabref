package org.jabref.logic.openoffice.uno;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Creates named Writer services and inserts them at their specified text ranges.
public class NamedTextContentInserter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamedTextContentInserter.class);

    private NamedTextContentInserter() {
    }

    /// Insert a new instance of a service at the provided cursor position.
    ///
    /// Passed to this.asXMultiServiceFactory().createInstance(service) The result is expected to support the XNamed and XTextContent interfaces.
    ///
    /// @param service For example "com.sun.star.text.ReferenceMark", "com.sun.star.text.Bookmark" or "com.sun.star.text.TextSection".
    /// @return The XNamed interface, in case we need to check the actual name.
    public static XNamed insertNamedTextContent(String service, NamedTextContentPlacement placement)
            throws
            WriterOperationException {

        XMultiServiceFactory msf = UnoInterfaces.query(XMultiServiceFactory.class, placement.doc())
                                          .orElseThrow(() -> new WriterOperationException("Document does not supply a service factory"));

        Object xObject;
        try {
            xObject = msf.createInstance(service);
        } catch (Exception e) {
            LOGGER.debug("Cannot create document service: {}", service, e);
            throw new WriterOperationException("Cannot create document service: " + service);
        }

        XNamed xNamed = UnoInterfaces.query(XNamed.class, xObject)
                               .orElseThrow(() -> new WriterOperationException("Service is not an XNamed: " + service));
        xNamed.setName(placement.name());

        // get XTextContent interface
        XTextContent xTextContent = UnoInterfaces.query(XTextContent.class, xObject)
                                           .orElseThrow(() -> new WriterOperationException("Service is not an XTextContent: " + service));
        placement.range().getText().insertTextContent(placement.range(), xTextContent, placement.absorb());
        return xNamed;
    }
}
