package org.jabref.logic.openoffice.oocsltext;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;

import com.sun.star.container.XNamed;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
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

    public void insertInText(XTextDocument doc, XTextCursor cursor, OOText ooText) throws Exception {
        // To debug: mark doesn't wrap around text!??

        // Insert the text content at the cursor position
        OOTextIntoOO.write(doc, cursor, ooText);

        // Create a text range covering the just-inserted text
        XTextRange start = cursor.getStart();
        XTextRange end = cursor.getEnd();
        cursor.gotoRange(start, false);
        cursor.gotoRange(end, true);

        // Attach the reference mark to this range
        textContent.attach(cursor);
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
