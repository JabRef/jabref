package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.model.entry.BibEntry;

import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import io.github.thibaultmeyer.cuid.CUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSLReferenceMarkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSLReferenceMarkManager.class);

    private final XTextDocument document;
    private final XMultiServiceFactory factory;
    private final Map<String, CSLReferenceMark> marksByName = new HashMap<>();
    private final List<CSLReferenceMark> marksInOrder = new ArrayList<>();
    private Map<String, Integer> citationKeyToNumber = new HashMap<>();
    private XTextRangeCompare textRangeCompare;

    public CSLReferenceMarkManager(XTextDocument document) {
        this.document = document;
        this.factory = UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        this.textRangeCompare = UnoRuntime.queryInterface(XTextRangeCompare.class, document.getText());
    }

    public CSLReferenceMark createReferenceMark(BibEntry entry) throws Exception {
        String citationKey = entry.getCitationKey().orElse(CUID.randomCUID2(8).toString());
        int citationNumber = getCitationNumber(citationKey);
        CSLReferenceMark referenceMark = CSLReferenceMark.of(citationKey, citationNumber, factory);
        marksByName.put(referenceMark.getName(), referenceMark);
        marksInOrder.add(referenceMark);
        updateAllCitationNumbers();
        return referenceMark;
    }

    public void updateAllCitationNumbers() throws Exception {
        sortMarksInOrder();
        Map<String, Integer> newCitationKeyToNumber = new HashMap<>();
        int currentNumber = 1;

        for (CSLReferenceMark mark : marksInOrder) {
            String citationKey = mark.getCitationKey();
            int assignedNumber;
            if (newCitationKeyToNumber.containsKey(citationKey)) {
                assignedNumber = newCitationKeyToNumber.get(citationKey);
            } else {
                assignedNumber = currentNumber;
                newCitationKeyToNumber.put(citationKey, assignedNumber);
                currentNumber++;
            }
            mark.setCitationNumber(assignedNumber);
            updateMarkText(mark, assignedNumber);
        }

        citationKeyToNumber = newCitationKeyToNumber;
    }

    private void sortMarksInOrder() {
        marksInOrder.sort((m1, m2) -> compareTextRanges(m2.getTextContent().getAnchor(), m1.getTextContent().getAnchor()));
    }

    private int compareTextRanges(XTextRange r1, XTextRange r2) {
        try {
            return textRangeCompare.compareRegionStarts(r1, r2);
        } catch (com.sun.star.lang.IllegalArgumentException e) {
            System.err.println("Error comparing text ranges: " + e.getMessage());
            return 0;
        }
    }

    private void updateMarkText(CSLReferenceMark mark, int newNumber) throws Exception {
        XTextContent oldContent = mark.getTextContent();
        XTextRange range = oldContent.getAnchor();

        if (range != null) {
            XText text = range.getText();

            // Store the position of the mark
            XTextCursor cursor = text.createTextCursorByRange(range);

            // Get the current text content
            String currentText = range.getString();

            // Update the citation number in the text
            String updatedText = updateCitationText(currentText, newNumber);

            // Remove the old reference mark without removing the text
            text.removeTextContent(oldContent);

            // Update the text
            cursor.setString(updatedText);

            // Create a new reference mark with updated name
            String updatedName = updateReferenceName(mark.getName(), newNumber);
            XNamed newNamed = UnoRuntime.queryInterface(XNamed.class,
                    factory.createInstance("com.sun.star.text.ReferenceMark"));
            newNamed.setName(updatedName);
            XTextContent newContent = UnoRuntime.queryInterface(XTextContent.class, newNamed);

            // Attach the new reference mark to the cursor range
            newContent.attach(cursor);

            // Update our internal reference to the new text content and name
            mark.updateTextContent(newContent);
            mark.updateName(updatedName);
            mark.setCitationNumber(newNumber);
        }
    }

    private String updateReferenceName(String oldName, int newNumber) {
        String[] parts = oldName.split(" ");
        if (parts.length >= 2) {
            parts[1] = "CID_" + newNumber;
            return String.join(" ", parts);
        }
        return oldName;
    }

    private String updateCitationText(String currentText, int newNumber) {
        Pattern pattern = Pattern.compile("(\\D*)(\\d+)(\\D*)");
        Matcher matcher = pattern.matcher(currentText);
        if (matcher.find()) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(3);
            return prefix + newNumber + suffix;
        }
        return currentText;
    }

    public int getCitationNumber(String citationKey) {
        return citationKeyToNumber.getOrDefault(citationKey, 1);
    }

    public void readExistingMarks() throws WrappedTargetException, com.sun.star.container.NoSuchElementException {
        marksByName.clear();
        marksInOrder.clear();
        citationKeyToNumber.clear();

        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();

        for (String name : marks.getElementNames()) {
            if (name.startsWith("JABREF_")) {
                XNamed named = UnoRuntime.queryInterface(XNamed.class, marks.getByName(name));
                String[] parts = name.split(" ");
                String citationKey = parts[0].substring(7);
                int citationNumber = Integer.parseInt(parts[1].substring(4));
                String uniqueId = parts[2];
                CSLReferenceMark mark = new CSLReferenceMark(named, new ReferenceMark(name, citationKey, citationNumber, uniqueId));
                marksByName.put(name, mark);
                marksInOrder.add(mark);
            }
        }

        try {
            updateAllCitationNumbers();
        } catch (Exception e) {
            LOGGER.warn("Error updating citation numbers: {}", e.getMessage(), e);
        }
    }

    public boolean hasCitationForKey(String citationKey) {
        return citationKeyToNumber.containsKey(citationKey);
    }
}
