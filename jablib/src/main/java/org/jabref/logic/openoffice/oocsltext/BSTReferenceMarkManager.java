package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.openoffice.JabRefReferenceMark;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.DocumentAnnotation;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.rangesort.RangeSort;
import org.jabref.model.openoffice.rangesort.RangeSortEntry;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.UnoReferenceMark;
import org.jabref.model.openoffice.uno.UnoTextRange;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.logic.openoffice.backend.NamedRangeReferenceMark.safeInsertSpacesBetweenReferenceMarks;

/// Reference mark manager for BST-based citations.
///
/// Mirrors the behavior of [CSLReferenceMarkManager] but uses identifier terminology
/// and supports applying a precomputed numbering map (e.g., from BST style order).
public class BSTReferenceMarkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BSTReferenceMarkManager.class);
    private static final Pattern CITATION_NUMBER_PATTERN = Pattern.compile("(\\D*)(\\d+)(\\D*)");

    private final XTextDocument document;
    private final XMultiServiceFactory factory;
    private final Map<String, BSTReferenceMark> marksByName = new HashMap<>();
    private final List<BSTReferenceMark> marksInOrder = new ArrayList<>();
    private Map<String, Integer> identifierToNumber = new HashMap<>();
    private final XTextRangeCompare textRangeCompare;
    private int highestCitationNumber = 0;
    private boolean isNumberUpdateRequired;
    private CSLCitationType citationType;

    public BSTReferenceMarkManager(XTextDocument document) {
        this.document = document;
        this.factory = UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        this.textRangeCompare = UnoRuntime.queryInterface(XTextRangeCompare.class, document.getText());
        this.isNumberUpdateRequired = false;
        this.citationType = CSLCitationType.NORMAL;
    }

    public BSTReferenceMark createReferenceMark(List<BibEntry> entries, CSLCitationType citationType) throws Exception {
        List<String> identifiers = entries.stream()
                                          .map(entry -> entry.getCitationKey().orElse(entry.getId()))
                                          .collect(Collectors.toList());

        List<Integer> citationNumbers = identifiers.stream()
                                                   .map(this::getCitationNumber)
                                                   .collect(Collectors.toList());

        BSTReferenceMark referenceMark = BSTReferenceMark.of(identifiers, citationNumbers, citationType, factory);
        marksByName.put(referenceMark.getName(), referenceMark);
        marksInOrder.add(referenceMark);
        this.citationType = citationType;
        return referenceMark;
    }

    public void insertReferenceIntoOO(List<BibEntry> entries, XTextDocument doc, XTextCursor position, OOText ooText, boolean insertSpaceBefore, boolean insertSpaceAfter, CSLCitationType citationType)
            throws CreationException, Exception {
        BSTReferenceMark mark = createReferenceMark(entries, citationType);
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
        identifierToNumber.clear();
        citationType = CSLCitationType.NORMAL;

        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();

        for (String name : marks.getElementNames()) {
            if (!JabRefReferenceMark.isJabRefReferenceMarkName(name)) {
                continue;
            }

            XNamed named = UnoRuntime.queryInterface(XNamed.class, marks.getByName(name));
            Optional<JabRefReferenceMark> referenceMark = JabRefReferenceMark.parse(name);
            if (referenceMark.isEmpty()) {
                LOGGER.warn("Cannot parse reference mark - invalid format: {}", name);
                continue;
            }

            List<String> identifiers = referenceMark.orElseThrow().getCitationKeys();
            List<Integer> citationNumbers = referenceMark.orElseThrow().getCitationNumbers();

            if (!identifiers.isEmpty() && !citationNumbers.isEmpty()) {
                BSTReferenceMark mark = new BSTReferenceMark(named, referenceMark.orElseThrow());
                marksByName.put(name, mark);
                marksInOrder.add(mark);
                citationType = referenceMark.orElseThrow().getCitationType();

                for (int i = 0; i < identifiers.size(); i++) {
                    String id = identifiers.get(i);
                    int number = citationNumbers.get(i);
                    identifierToNumber.put(id, number);
                    highestCitationNumber = Math.max(highestCitationNumber, number);
                }
            } else {
                LOGGER.warn("Cannot parse reference mark - invalid format: {}", name);
            }
        }

        LOGGER.debug("Read {} existing marks", marksByName.size());

        if (isNumberUpdateRequired) {
            try {
                updateAllCitationNumbers();
            } catch (Exception | CreationException e) {
                LOGGER.warn("Error updating citation numbers", e);
            }
        }
    }

    private String getUpdatedReferenceMarkNameWithNewNumbers(String oldName, List<Integer> newNumbers) {
        return JabRefReferenceMark.parse(oldName)
                                  .map(referenceMark -> JabRefReferenceMark.buildReferenceMarkName(
                                          referenceMark.getCitationKeys(),
                                          newNumbers,
                                          referenceMark.getUniqueId(),
                                          referenceMark.getCitationType()))
                                  .orElse(oldName);
    }

    private void updateAllCitationNumbers() throws Exception, CreationException {
        sortMarksInOrder();
        Map<String, Integer> newIdentifierToNumber = new HashMap<>();
        int currentNumber = 1;

        for (BSTReferenceMark mark : marksInOrder) {
            List<String> identifiers = mark.getCitationKeys();
            List<Integer> assignedNumbers = new ArrayList<>();

            for (String id : identifiers) {
                int assignedNumber;
                if (newIdentifierToNumber.containsKey(id)) {
                    assignedNumber = newIdentifierToNumber.get(id);
                } else {
                    assignedNumber = currentNumber;
                    newIdentifierToNumber.put(id, assignedNumber);
                    currentNumber++;
                }
                assignedNumbers.add(assignedNumber);
            }

            mark.setCitationNumbers(assignedNumbers);
            updateMarkAndTextWithNewNumbers(mark, assignedNumbers);
        }

        identifierToNumber = newIdentifierToNumber;
        highestCitationNumber = newIdentifierToNumber.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private String getUpdatedCitationTextWithNewNumbers(String currentText, List<Integer> newNumbers) {
        Matcher matcher = CITATION_NUMBER_PATTERN.matcher(currentText);
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

    private void updateMarkAndTextWithNewNumbers(BSTReferenceMark mark, List<Integer> newNumbers) throws Exception, CreationException {
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

    private void updateMarkAndText(BSTReferenceMark mark, String newText, String markName) throws Exception, CreationException {
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

    public int getCitationNumber(String identifier) {
        Integer override = identifierToNumber.get(identifier);
        if (override != null) {
            return override;
        }
        return identifierToNumber.computeIfAbsent(identifier, _ -> ++highestCitationNumber);
    }

    public boolean hasCitationForIdentifier(String identifier) {
        return identifierToNumber.containsKey(identifier);
    }

    public CSLCitationType getCitationType() {
        return citationType;
    }

    public void setRealTimeNumberUpdateRequired(boolean isNumeric) {
        this.isNumberUpdateRequired = isNumeric;
    }

    private void sortMarksInOrder() {
        // Reverse document order (bottom to top)
        // NOTE: We must traverse bottom→top when rewriting reference marks.
        // Editing a mark removes and recreates the content, which shifts subsequent
        // anchors. Operating from the bottom avoids temporarily inverted sequences
        // like [3] above [2] above [1] during refresh.
        List<RangeSortEntry<BSTReferenceMark>> sortEntries = new ArrayList<>();

        for (BSTReferenceMark mark : marksInOrder) {
            XTextRange range = mark.getTextContent().getAnchor();
            if (range == null) {
                LOGGER.debug("Skipping dangling BST reference mark without anchor: {}", mark.getName());
                continue;
            }
            sortEntries.add(new RangeSortEntry<>(range, 0, mark));
        }

        RangeSort.RangePartitions<RangeSortEntry<BSTReferenceMark>> partitions =
                RangeSort.partitionAndSortRanges(sortEntries);

        for (List<RangeSortEntry<BSTReferenceMark>> partition : partitions.getPartitions()) {
            int indexInPartition = 0;
            for (RangeSortEntry<BSTReferenceMark> sortEntry : partition) {
                sortEntry.setIndexInPosition(indexInPartition++);

                Optional<XTextRange> footnoteMarkRange =
                        UnoTextRange.getFootnoteMarkRange(sortEntry.getRange());
                footnoteMarkRange.ifPresent(sortEntry::setRange);
            }
        }

        sortEntries.sort(this::compareTextRanges);

        marksInOrder.clear();
        sortEntries.stream()
                   .map(RangeSortEntry::getContent)
                   .forEach(marksInOrder::add);
    }

    private int compareTextRanges(RangeSortEntry<BSTReferenceMark> first, RangeSortEntry<BSTReferenceMark> second) {
        int rangeComparison;
        try {
            rangeComparison = textRangeCompare.compareRegionStarts(second.getRange(), first.getRange());
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Error comparing text ranges: {}", exception.getMessage(), exception);
            rangeComparison = 0;
        }

        if (rangeComparison != 0) {
            return rangeComparison;
        }

        return Integer.compare(first.getIndexInPosition(), second.getIndexInPosition());
    }
}
