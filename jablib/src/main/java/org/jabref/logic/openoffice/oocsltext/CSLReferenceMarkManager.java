package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.DocumentAnnotation;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.UnoReferenceMark;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
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

import static org.jabref.logic.openoffice.backend.NamedRangeReferenceMark.safeInsertSpacesBetweenReferenceMarks;

/**
 * Class for generation, insertion and management of all reference marks in the document.
 */
public class CSLReferenceMarkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSLReferenceMarkManager.class);

    private final XTextDocument document;
    private final XMultiServiceFactory factory;
    private final Map<String, CSLReferenceMark> marksByName = new HashMap<>();
    private final List<CSLReferenceMark> marksInOrder = new ArrayList<>();
    private Map<String, Integer> citationKeyToNumber = new HashMap<>();
    private final XTextRangeCompare textRangeCompare;
    private int highestCitationNumber = 0;
    private boolean isNumberUpdateRequired;

    public CSLReferenceMarkManager(XTextDocument document) {
        this.document = document;
        this.factory = UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        this.textRangeCompare = UnoRuntime.queryInterface(XTextRangeCompare.class, document.getText());
        this.isNumberUpdateRequired = false;
    }

    public CSLReferenceMark createReferenceMark(List<BibEntry> entries) throws Exception {
        List<String> citationKeys = entries.stream()
                                           .map(entry -> entry.getCitationKey().orElse(CUID.randomCUID2(8).toString()))
                                           .collect(Collectors.toList());

        List<Integer> citationNumbers = citationKeys.stream()
                                                    .map(this::getCitationNumber)
                                                    .collect(Collectors.toList());

        CSLReferenceMark referenceMark = CSLReferenceMark.of(citationKeys, citationNumbers, factory);
        marksByName.put(referenceMark.getName(), referenceMark);
        marksInOrder.add(referenceMark);
        return referenceMark;
    }

    public void insertReferenceIntoOO(List<BibEntry> entries, XTextDocument doc, XTextCursor position, OOText ooText, boolean insertSpaceBefore, boolean insertSpaceAfter)
            throws CreationException, Exception {
        CSLReferenceMark mark = createReferenceMark(entries);
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
        DocumentAnnotation documentAnnotation = new DocumentAnnotation(doc, mark.getName(), cursor, true);
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

    public void readAndUpdateExistingMarks() throws WrappedTargetException, NoSuchElementException {
        marksByName.clear();
        marksInOrder.clear();
        citationKeyToNumber.clear();

        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();

        for (String name : marks.getElementNames()) {
            String[] parts = name.split(" ");
            if (parts[0].startsWith(ReferenceMark.PREFIXES[0]) && parts[1].startsWith(ReferenceMark.PREFIXES[1]) && parts.length >= 3) {
                XNamed named = UnoRuntime.queryInterface(XNamed.class, marks.getByName(name));

                ReferenceMark referenceMark = new ReferenceMark(name);
                List<String> citationKeys = referenceMark.getCitationKeys();
                List<Integer> citationNumbers = referenceMark.getCitationNumbers();

                if (!citationKeys.isEmpty() && !citationNumbers.isEmpty()) {
                    CSLReferenceMark mark = new CSLReferenceMark(named, referenceMark);
                    marksByName.put(name, mark);
                    marksInOrder.add(mark);

                    for (int i = 0; i < citationKeys.size(); i++) {
                        String key = citationKeys.get(i);
                        int number = citationNumbers.get(i);
                        citationKeyToNumber.put(key, number);
                        highestCitationNumber = Math.max(highestCitationNumber, number);
                    }
                } else {
                    LOGGER.warn("Cannot parse reference mark - invalid format: {}", name);
                }
            }
        }

        LOGGER.debug("Read {} existing marks", marksByName.size());

        if (isNumberUpdateRequired) {
            try {
                updateAllCitationNumbers();
            } catch (Exception
                     | CreationException e) {
                LOGGER.warn("Error updating citation numbers", e);
            }
        }
    }

    private String getUpdatedReferenceMarkNameWithNewNumbers(String oldName, List<Integer> newNumbers) {
        String[] parts = oldName.split(" ");
        if (parts[0].startsWith(ReferenceMark.PREFIXES[0]) && parts[1].startsWith(ReferenceMark.PREFIXES[1]) && parts.length >= 3) {
            StringBuilder newName = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i += 2) {
                // Each iteration of the loop (incrementing by 2) represents one full citation (key + number)
                if (i > 0) {
                    newName.append(", ");
                }
                newName.append(parts[i]).append(" ");
                newName.append(ReferenceMark.PREFIXES[1]).append(newNumbers.get(i / 2));
            }
            newName.append(" ").append(parts[parts.length - 1]);
            return newName.toString();
        }
        return oldName;
    }

    private void updateAllCitationNumbers() throws Exception, CreationException {
        sortMarksInOrder();
        Map<String, Integer> newCitationKeyToNumber = new HashMap<>();
        int currentNumber = 1;

        for (CSLReferenceMark mark : marksInOrder) {
            List<String> citationKeys = mark.getCitationKeys();
            List<Integer> assignedNumbers = new ArrayList<>();

            for (String citationKey : citationKeys) {
                int assignedNumber;
                if (newCitationKeyToNumber.containsKey(citationKey)) {
                    assignedNumber = newCitationKeyToNumber.get(citationKey);
                } else {
                    assignedNumber = currentNumber;
                    newCitationKeyToNumber.put(citationKey, assignedNumber);
                    currentNumber++;
                }
                assignedNumbers.add(assignedNumber);
            }

            mark.setCitationNumbers(assignedNumbers);
            updateMarkAndTextWithNewNumbers(mark, assignedNumbers);
        }

        citationKeyToNumber = newCitationKeyToNumber;
    }

    private String getUpdatedCitationTextWithNewNumbers(String currentText, List<Integer> newNumbers) {
        Pattern pattern = Pattern.compile("(\\D*)(\\d+)(\\D*)");
        Matcher matcher = pattern.matcher(currentText);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        int numberIndex = 0;

        while (matcher.find()) {
            result.append(currentText, lastEnd, matcher.start(2));
            if (numberIndex < newNumbers.size()) {
                result.append(newNumbers.get(numberIndex));
            } else {
                // If we've run out of new numbers, increment the last used number
                result.append(newNumbers.getLast() + (numberIndex - newNumbers.size() + 1));
            }
            numberIndex++;
            lastEnd = matcher.end(2);
        }
        result.append(currentText.substring(lastEnd));

        return result.toString();
    }

    private void updateMarkAndTextWithNewNumbers(CSLReferenceMark mark, List<Integer> newNumbers) throws Exception, CreationException {
        String updatedName = getUpdatedReferenceMarkNameWithNewNumbers(mark.getName(), newNumbers);
        String currentText = mark.getTextContent().getAnchor().getString();
        String updatedText = getUpdatedCitationTextWithNewNumbers(currentText, newNumbers);

        updateMarkAndText(mark, updatedText, updatedName);

        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();
        XTextContent newContent = UnoRuntime.queryInterface(XTextContent.class, marks.getByName(updatedName));

        mark.updateTextContent(newContent);
        mark.updateName(updatedName);
        mark.setCitationNumbers(newNumbers);
    }

    public void updateMarkAndTextWithNewStyle(CSLReferenceMark mark, String newText) throws Exception, CreationException {
        String unchangedName = mark.getName();

        updateMarkAndText(mark, newText, unchangedName);
    }

    private void updateMarkAndText(CSLReferenceMark mark, String newText, String markName) throws Exception, CreationException {
        XTextContent oldContent = mark.getTextContent();
        XTextRange range = oldContent.getAnchor();

        if (range != null) {
            XText text = range.getText();
            XTextCursor cursor = text.createTextCursorByRange(range);
            OOText ooText = OOText.fromString(newText);

            // The only way to edit a reference mark is to remove it and add a new one
            // Remove old reference mark but keep cursor position
            text.removeTextContent(oldContent);

            // Store the start position before writing
            XTextRange startRange = cursor.getStart();

            // Update the text using OOTextIntoOO
            OOTextIntoOO.write(document, cursor, ooText);

            // Store the end position after writing
            XTextRange endRange = cursor.getEnd();

            // Move cursor to wrap the entire inserted content
            cursor.gotoRange(startRange, false);
            cursor.gotoRange(endRange, true);

            // Create and attach DocumentAnnotation
            DocumentAnnotation documentAnnotation = new DocumentAnnotation(document, markName, cursor, true);
            UnoReferenceMark.create(documentAnnotation);

            // Move cursor to the end
            cursor.gotoRange(endRange, false);
        }
    }

    public int getCitationNumber(String citationKey) {
        return citationKeyToNumber.computeIfAbsent(citationKey, _ -> ++highestCitationNumber);
    }

    public List<CSLReferenceMark> getMarksInOrder() {
        sortMarksInOrder();
        return marksInOrder;
    }

    public boolean hasCitationForKey(String citationKey) {
        return citationKeyToNumber.containsKey(citationKey);
    }

    public void setRealTimeNumberUpdateRequired(boolean isNumeric) {
        this.isNumberUpdateRequired = isNumeric;
    }

    private void sortMarksInOrder() {
        marksInOrder.sort((m1, m2) -> compareTextRanges(m2.getTextContent().getAnchor(), m1.getTextContent().getAnchor()));
    }

    private int compareTextRanges(XTextRange r1, XTextRange r2) {
        try {
            return r1 != null && r2 != null ? textRangeCompare.compareRegionStarts(r1, r2) : 0;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Error comparing text ranges: {}", e.getMessage(), e);
            return 0;
        }
    }
}
