package org.jabref.logic.openoffice.oocsltext;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;

import com.sun.star.container.XNamed;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;

public class CSLReferenceMark {
    private final String name;
    private final XTextContent textContent;
    private String citationKey;
    private String citationNumber;

    public CSLReferenceMark(XNamed named, String name) {
        this.name = name;
        this.textContent = UnoRuntime.queryInterface(XTextContent.class, named);

        // Format: JABREF_{citationKey} RND{citationNumber}
        String[] parts = name.split(" ");

        if (parts.length >= 2 && parts[0].startsWith("JABREF_") && parts[1].startsWith("RND")) {
            this.citationKey = parts[0].substring(7);
            this.citationNumber = parts[1].substring(3);
        }
    }

    public void insertReferenceIntoOO(XTextDocument doc, XTextCursor cursor, OOText ooText) throws Exception {
        // Create a text range for the start position
        XTextRange startRange = cursor.getStart();

        // Insert the text content at the cursor position
        OOTextIntoOO.write(doc, cursor, ooText);

        // Create a text range for the end position
        XTextRange endRange = cursor.getEnd();

        // Move the cursor back to the start position
        cursor.gotoRange(startRange, false);

        // Select the text by moving to the end position
        cursor.gotoRange(endRange, true);

        // Attach the reference mark to the selected range
        textContent.attach(cursor);

        // Move the cursor to the end of the inserted text
        cursor.gotoRange(endRange, false);
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
