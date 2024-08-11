package org.jabref.logic.openoffice.oocsltext;

import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.model.openoffice.DocumentAnnotation;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.UnoReferenceMark;

import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
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

import static org.jabref.logic.openoffice.backend.NamedRangeReferenceMark.safeInsertSpacesBetweenReferenceMarks;

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

    public void insertReferenceIntoOO(XTextDocument doc, XTextCursor position, OOText ooText, boolean insertSpaceBefore, boolean insertSpaceAfter, boolean withoutBrackets)
            throws CreationException, WrappedTargetException {
        // Ensure the cursor is at the end of its range
        position.collapseToEnd();

        // Insert spaces safely
        XTextCursor cursor = safeInsertSpacesBetweenReferenceMarks(position.getEnd(), 2);

        // Cursors before the first and after the last space
        XTextCursor cursorBefore = cursor.getText().createTextCursorByRange(cursor.getStart());
        XTextCursor cursorAfter = cursor.getText().createTextCursorByRange(cursor.getEnd());

        cursor.collapseToStart();
        cursor.goRight((short) 1, false);
        // Now we are between two spaces

        // Store the start position
        XTextRange startRange = cursor.getStart();

        // Insert the OOText content
        OOTextIntoOO.write(doc, cursor, ooText);

        // Store the end position
        XTextRange endRange = cursor.getEnd();

        // Move cursor to wrap the entire inserted content
        cursor.gotoRange(startRange, false);
        cursor.gotoRange(endRange, true);

        // Create DocumentAnnotation and attach it
        DocumentAnnotation documentAnnotation = new DocumentAnnotation(doc, referenceMark.getName(), cursor, true);
        UnoReferenceMark.create(documentAnnotation);

        // Move cursor to the end of the inserted content
        cursor.gotoRange(endRange, false);

        // Remove extra spaces
        if (!insertSpaceBefore) {
            cursorBefore.goRight((short) 1, true);
            cursorBefore.setString("");
        }
        if (!insertSpaceAfter) {
            cursorAfter.goLeft((short) 1, true);
            cursorAfter.setString("");
        }

        // Move the original position cursor to the end of the inserted content
        position.gotoRange(cursorAfter.getEnd(), false);
    }

    public XTextContent getTextContent() {
        return textContent;
    }

    public String getName() {
        return referenceMark.getName();
    }
}
