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
    private String uniqueId;

    public CSLReferenceMark(XNamed named, String name) {
        this.name = name;
        this.textContent = UnoRuntime.queryInterface(XTextContent.class, named);

        // Format: JABREF_{citationKey} CID_{citationNumber} {uniqueId}
        String[] parts = name.split(" ");

        if (parts.length >= 3 && parts[0].startsWith("JABREF_") && parts[1].startsWith("CID_")) {
            this.citationKey = parts[0].substring(7);
            this.citationNumber = parts[1].substring(4);
            this.uniqueId = parts[2];
            System.out.println(citationKey);
            System.out.println(citationNumber);
            System.out.println(uniqueId);
        }
    }

    public void insertReferenceIntoOO(XTextDocument doc, XTextCursor cursor, OOText ooText) throws Exception {
        XTextRange startRange = cursor.getStart();
        OOTextIntoOO.write(doc, cursor, ooText);
        XTextRange endRange = cursor.getEnd();
        cursor.gotoRange(startRange, false);
        cursor.gotoRange(endRange, true);
        textContent.attach(cursor);
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

    public String getCitationNumber() {
        return citationNumber;
    }

    public String getUniqueId() {
        return uniqueId;
    }
}
