package org.jabref.model.openoffice.uno;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public class UnoNamed {

    private UnoNamed() { }

    /**
     * Insert a new instance of a service at the provided cursor position.
     *
     * @param service For example
     *                "com.sun.star.text.ReferenceMark",
     *                "com.sun.star.text.Bookmark" or
     *                "com.sun.star.text.TextSection".
     *
     *                Passed to this.asXMultiServiceFactory().createInstance(service)
     *                The result is expected to support the XNamed and XTextContent interfaces.
     *
     * @param name    For the ReferenceMark, Bookmark, TextSection.
     *                If the name is already in use, LibreOffice may change the name.
     *
     * @param range   Marks the location or range for the thing to be inserted.
     *
     * @param absorb ReferenceMark, Bookmark and TextSection can incorporate a text range.
     *               If absorb is true, the text in the range becomes part of the thing.
     *               If absorb is false, the thing is inserted at the end of the range.
     *
     * @return The XNamed interface, in case we need to check the actual name.
     *
     */
    static XNamed insertNamedTextContent(XTextDocument doc,
                                         String service,
                                         String name,
                                         XTextRange range,
                                         boolean absorb)
        throws
        CreationException {

        XMultiServiceFactory msf = UnoCast.cast(XMultiServiceFactory.class, doc).get();

        Object xObject;
        try {
            xObject = msf.createInstance(service);
        } catch (com.sun.star.uno.Exception e) {
            throw new CreationException(e.getMessage());
        }

        XNamed xNamed = (UnoCast.cast(XNamed.class, xObject)
                         .orElseThrow(() -> new IllegalArgumentException("Service is not an XNamed")));
        xNamed.setName(name);

        // get XTextContent interface
        XTextContent xTextContent = (UnoCast.cast(XTextContent.class, xObject)
                                     .orElseThrow(() -> new IllegalArgumentException("Service is not an XTextContent")));
        range.getText().insertTextContent(range, xTextContent, absorb);
        return xNamed;
    }

}
