package org.jabref.model.openoffice.uno;

import org.jabref.model.openoffice.DocumentAnnotation;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.Exception;

public class UnoNamed {

    private UnoNamed() {
    }

    /**
     * Insert a new instance of a service at the provided cursor position.
     *
     * @param service For example "com.sun.star.text.ReferenceMark", "com.sun.star.text.Bookmark" or "com.sun.star.text.TextSection".
     *                <p>
     *                Passed to this.asXMultiServiceFactory().createInstance(service) The result is expected to support the XNamed and XTextContent interfaces.
     * @return The XNamed interface, in case we need to check the actual name.
     */
    static XNamed insertNamedTextContent(String service, DocumentAnnotation documentAnnotation)
            throws
            CreationException {

        XMultiServiceFactory msf = UnoCast.cast(XMultiServiceFactory.class, documentAnnotation.doc()).get();

        Object xObject;
        try {
            xObject = msf.createInstance(service);
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }

        XNamed xNamed = UnoCast.cast(XNamed.class, xObject)
                               .orElseThrow(() -> new IllegalArgumentException("Service is not an XNamed"));
        xNamed.setName(documentAnnotation.name());

        // get XTextContent interface
        XTextContent xTextContent = UnoCast.cast(XTextContent.class, xObject)
                                           .orElseThrow(() -> new IllegalArgumentException("Service is not an XTextContent"));
        documentAnnotation.range().getText().insertTextContent(documentAnnotation.range(), xTextContent, documentAnnotation.absorb());
        return xNamed;
    }
}
