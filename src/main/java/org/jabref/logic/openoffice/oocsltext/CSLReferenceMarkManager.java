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

public class CSLReferenceMarkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSLReferenceMarkManager.class);

    private final XTextDocument document;
    private final XMultiServiceFactory factory;
    private final Map<String, CSLReferenceMark> marksByName = new HashMap<>();
    private final List<CSLReferenceMark> marksInOrder = new ArrayList<>();
    private Map<String, Integer> citationKeyToNumber = new HashMap<>();
    private final XTextRangeCompare textRangeCompare;
    private int highestCitationNumber = 0;
    private boolean isUpdateRequired;

    public CSLReferenceMarkManager(XTextDocument document) {
        this.document = document;
        this.factory = UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        this.textRangeCompare = UnoRuntime.queryInterface(XTextRangeCompare.class, document.getText());
        this.isUpdateRequired = false;
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

    public void setUpdateRequired(boolean isNumeric) {
        this.isUpdateRequired = isNumeric;
    }

    public void updateAllCitationNumbers() throws Exception {
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
            updateMarkAndText(mark, assignedNumbers);
        }

        citationKeyToNumber = newCitationKeyToNumber;
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

    private void updateMarkAndText(CSLReferenceMark mark, List<Integer> newNumbers) throws Exception {
        XTextContent oldContent = mark.getTextContent();
        XTextRange range = oldContent.getAnchor();

        if (range != null) {
            XText text = range.getText();

            // Store the position of the mark
            XTextCursor cursor = text.createTextCursorByRange(range);

            // Get the current text content
            String currentText = range.getString();

            // Update the citation numbers in the text
            String updatedText = updateCitationText(currentText, newNumbers);

            // Remove the old reference mark without removing the text (The only way to edit a reference mark is to remove it and add a new one)
            text.removeTextContent(oldContent);

            // Update the text
            cursor.setString(updatedText);

            // Create a new reference mark with updated name
            String updatedName = updateReferenceName(mark.getName(), newNumbers);
            XNamed newNamed = UnoRuntime.queryInterface(XNamed.class,
                    factory.createInstance("com.sun.star.text.ReferenceMark"));
            newNamed.setName(updatedName);
            XTextContent newContent = UnoRuntime.queryInterface(XTextContent.class, newNamed);

            // Attach the new reference mark to the cursor range
            newContent.attach(cursor);

            // Update our internal reference to the new text content and name
            mark.updateTextContent(newContent);
            mark.updateName(updatedName);
            mark.setCitationNumbers(newNumbers);
        }
    }

    private String updateReferenceName(String oldName, List<Integer> newNumbers) {
        String[] parts = oldName.split(" ");
        if (oldName.startsWith("JABREF_") && oldName.contains("CID") && parts.length >= 3) {
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

    private String updateCitationText(String currentText, List<Integer> newNumbers) {
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

    public int getCitationNumber(String citationKey) {
        return citationKeyToNumber.computeIfAbsent(citationKey, k -> ++highestCitationNumber);
    }

    public void readAndUpdateExistingMarks() throws WrappedTargetException, NoSuchElementException {
        marksByName.clear();
        marksInOrder.clear();
        citationKeyToNumber.clear();

        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();

        for (String name : marks.getElementNames()) {
            if (name.startsWith(ReferenceMark.PREFIXES[0]) && name.contains(ReferenceMark.PREFIXES[1]) && name.split(" ").length >= 3) {
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

        if (isUpdateRequired) {
            try {
                updateAllCitationNumbers();
            } catch (Exception e) {
                LOGGER.warn("Error updating citation numbers", e);
            }
        }
    }

    public boolean hasCitationForKey(String citationKey) {
        return citationKeyToNumber.containsKey(citationKey);
    }
}
