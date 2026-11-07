package org.jabref.logic.openoffice.uno;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnoNamed {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnoNamed.class);

    private UnoNamed() {
    }

    /// Insert a new instance of a service at the provided cursor position.
    ///
    /// Passed to this.asXMultiServiceFactory().createInstance(service) The result is expected to support the XNamed and XTextContent interfaces.
    ///
    /// @param service For example "com.sun.star.text.ReferenceMark", "com.sun.star.text.Bookmark" or "com.sun.star.text.TextSection".
    /// @return The XNamed interface, in case we need to check the actual name.
    public static XNamed insertNamedTextContent(String service, DocumentAnnotation documentAnnotation)
            throws
            CreationException {

        XMultiServiceFactory msf = UnoCast.cast(XMultiServiceFactory.class, documentAnnotation.doc())
                                          .orElseThrow(() -> new CreationException("Document does not supply a service factory"));

        Object xObject;
        try {
            xObject = msf.createInstance(service);
        } catch (Exception e) {
            LOGGER.debug("Cannot create document service: {}", service, e);
            throw new CreationException("Cannot create document service: " + service);
        }

        XNamed xNamed = UnoCast.cast(XNamed.class, xObject)
                               .orElseThrow(() -> new CreationException("Service is not an XNamed: " + service));
        xNamed.setName(documentAnnotation.name());

        // get XTextContent interface
        XTextContent xTextContent = UnoCast.cast(XTextContent.class, xObject)
                                           .orElseThrow(() -> new CreationException("Service is not an XTextContent: " + service));
        documentAnnotation.range().getText().insertTextContent(documentAnnotation.range(), xTextContent, documentAnnotation.absorb());
        return xNamed;
    }
}
