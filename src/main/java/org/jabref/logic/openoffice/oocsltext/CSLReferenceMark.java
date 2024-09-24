package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

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

import static org.jabref.logic.openoffice.backend.NamedRangeReferenceMark.safeInsertSpacesBetweenReferenceMarks;

/**
 * Class to handle a reference mark. See {@link CSLReferenceMarkManager} for the management of all reference marks.
 */
public class CSLReferenceMark {
    private ReferenceMark referenceMark;
    private XTextContent textContent;
    private final List<String> citationKeys;
    private List<Integer> citationNumbers;

    public CSLReferenceMark(XNamed named, ReferenceMark referenceMark) {
        this.referenceMark = referenceMark;
        this.textContent = UnoRuntime.queryInterface(XTextContent.class, named);
        this.citationKeys = referenceMark.getCitationKeys();
        this.citationNumbers = referenceMark.getCitationNumbers();
    }

    public static CSLReferenceMark of(List<String> citationKeys, List<Integer> citationNumbers, XMultiServiceFactory factory) throws Exception {
        String uniqueId = CUID.randomCUID2(8).toString();
        String name = buildReferenceName(citationKeys, citationNumbers, uniqueId);
        XNamed named = UnoRuntime.queryInterface(XNamed.class, factory.createInstance("com.sun.star.text.ReferenceMark"));
        named.setName(name);
        ReferenceMark referenceMark = new ReferenceMark(name, citationKeys, citationNumbers, uniqueId);
        return new CSLReferenceMark(named, referenceMark);
    }

    private static String buildReferenceName(List<String> citationKeys, List<Integer> citationNumbers, String uniqueId) {
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < citationKeys.size(); i++) {
            if (i > 0) {
                nameBuilder.append(", ");
            }
            nameBuilder.append(ReferenceMark.PREFIXES[0]).append(citationKeys.get(i))
                       .append(" ").append(ReferenceMark.PREFIXES[1]).append(citationNumbers.get(i));
        }
        nameBuilder.append(" ").append(uniqueId);
        return nameBuilder.toString();
    }

    public List<String> getCitationKeys() {
        return citationKeys;
    }

    public void setCitationNumbers(List<Integer> numbers) {
        this.citationNumbers = numbers;
    }

    public XTextContent getTextContent() {
        return textContent;
    }

    public String getName() {
        return referenceMark.getName();
    }

    public void insertReferenceIntoOO(XTextDocument doc, XTextCursor position, OOText ooText, boolean insertSpaceBefore, boolean insertSpaceAfter)
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

    public void updateTextContent(XTextContent newTextContent) {
        this.textContent = newTextContent;
    }

    public void updateName(String newName) {
        this.referenceMark = new ReferenceMark(newName, this.citationKeys, this.citationNumbers, this.referenceMark.getUniqueId());
    }
}
