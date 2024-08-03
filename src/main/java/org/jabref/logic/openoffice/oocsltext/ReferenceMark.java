package org.jabref.logic.openoffice.oocsltext;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

public class ReferenceMark {
    private final XTextDocument document;
    private final XNamed named;
    private final String name;
    private XTextContent textContent;
    private String citationKey;

    public ReferenceMark(XTextDocument document, XNamed named, String name) {
        this.document = document;
        this.named = named;
        this.name = name;
        this.textContent = UnoRuntime.queryInterface(XTextContent.class, named);
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            this.citationKey = parts[1];
        }
    }

    public void insertInText(XTextDocument doc, XTextCursor cursor, OOText ooText) throws WrappedTargetException, CreationException {
        // First, insert the text content (ReferenceMark) at the cursor position
        cursor.getText().insertTextContent(cursor, textContent, true);

        // Then, insert the formatted text inside the ReferenceMark
        XTextCursor markCursor = textContent.getAnchor().getText().createTextCursorByRange(textContent.getAnchor());
        OOTextIntoOO.write(doc, markCursor, ooText);
    }

    public String getName() {
        return name;
    }

    public XTextContent getTextContent() {
        return textContent;
    }

    public String getCitationKey() {
        return citationKey;
    }
}
