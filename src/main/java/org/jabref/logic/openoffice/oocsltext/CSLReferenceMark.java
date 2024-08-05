package org.jabref.logic.openoffice.oocsltext;

import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import io.github.thibaultmeyer.cuid.CUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle a reference mark. See {@link CSLReferenceMarkManager} for the management of all reference marks.
 */
public class CSLReferenceMark {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLReferenceMark.class);

    private final ReferenceMark referenceMark;
    private final XTextContent textContent;

    public CSLReferenceMark(XNamed named, ReferenceMark referenceMark) {
        this.referenceMark = referenceMark;
        textContent = UnoRuntime.queryInterface(XTextContent.class, named);
    }

    public CSLReferenceMark(XNamed named, String name, String citationKey, Integer citationNumber, String uniqueId) {
        referenceMark = new ReferenceMark(name, citationKey, citationNumber, uniqueId);
        this.textContent = UnoRuntime.queryInterface(XTextContent.class, named);
    }

    public static CSLReferenceMark of(String citationKey, Integer citationNumber, XMultiServiceFactory factory) throws Exception {
        String uniqueId = CUID.randomCUID2(8).toString();
        String name = "JABREF_" + citationKey + " CID_" + citationNumber + " " + uniqueId;
        XNamed named = UnoRuntime.queryInterface(XNamed.class, factory.createInstance("com.sun.star.text.ReferenceMark"));
        named.setName(name);
        return new CSLReferenceMark(named, name, citationKey, citationNumber, uniqueId);
    }

    public void insertReferenceIntoOO(XTextDocument doc, XTextCursor cursor, OOText ooText) throws Exception, CreationException {
        XTextRange startRange = cursor.getStart();
        OOTextIntoOO.write(doc, cursor, ooText);
        XTextRange endRange = cursor.getEnd();
        cursor.gotoRange(startRange, false);
        cursor.gotoRange(endRange, true);
        textContent.attach(cursor);
        cursor.gotoRange(endRange, false);
    }

    public XTextContent getTextContent() {
        return textContent;
    }

    public String getName() {
        return referenceMark.getName();
    }
}
