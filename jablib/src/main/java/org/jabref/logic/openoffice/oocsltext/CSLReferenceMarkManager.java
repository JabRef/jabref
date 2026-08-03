package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.openoffice.JabRefReferenceMark;
import org.jabref.logic.openoffice.OpenOfficeReferenceMarkFormat;
import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.DocumentAnnotation;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.rangesort.RangeSort;
import org.jabref.model.openoffice.rangesort.RangeSortEntry;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.UnoReferenceMark;
import org.jabref.model.openoffice.uno.UnoTextRange;
import org.jabref.model.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.beans.NotRemoveableException;
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

/// Class for generation, insertion and management of all reference marks in the document.
public class CSLReferenceMarkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSLReferenceMarkManager.class);
    private static final String FORMATTED_CITATION_TEXT_PROPERTY_PREFIX = "JabRef_formatted_citation_text:";
    private static final Pattern CITATION_NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private final XTextDocument document;
    private final XMultiServiceFactory factory;
    private final Map<String, CSLReferenceMark> marksByName = new HashMap<>();
    private final List<CSLReferenceMark> marksInOrder = new ArrayList<>();
    private Map<String, Integer> citationKeyToNumber = new HashMap<>();
    private final XTextRangeCompare textRangeCompare;
    private int highestCitationNumber = 0;
    private int nextZoteroItemId = 1;
    private boolean isNumberUpdateRequired;
    private CSLCitationType citationType;

    public CSLReferenceMarkManager(XTextDocument document) {
        this.document = document;
        this.factory = UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        this.textRangeCompare = UnoRuntime.queryInterface(XTextRangeCompare.class, document.getText());
        this.isNumberUpdateRequired = false;
        this.citationType = CSLCitationType.NORMAL;
    }

    public CSLReferenceMark createReferenceMark(List<BibEntry> entries,
                                                CSLCitationType citationType,
                                                BibDatabaseContext bibDatabaseContext,
                                                BibEntryTypesManager entryTypesManager,
                                                OpenOfficeReferenceMarkFormat referenceMarkFormat) throws Exception {
        List<String> citationKeys = entries.stream()
                                           .map(entry -> entry.getCitationKey().orElse(CUID.randomCUID2(8).toString()))
                                           .collect(Collectors.toList());

        List<Integer> citationNumbers = citationKeys.stream()
                                                    .map(this::getCitationNumber)
                                                    .collect(Collectors.toList());

        CSLReferenceMark referenceMark = CSLReferenceMark.of(
                entries,
                citationKeys,
                citationNumbers,
                citationType,
                nextZoteroItemId,
                factory,
                bibDatabaseContext,
                entryTypesManager,
                referenceMarkFormat,
                getZoteroUriByCitationKey());
        marksByName.put(referenceMark.getName(), referenceMark);
        marksInOrder.add(referenceMark);
        nextZoteroItemId += entries.size();
        this.citationType = citationType;
        return referenceMark;
    }

    private Map<String, String> getZoteroUriByCitationKey() {
        Map<String, String> zoteroUriByCitationKey = new HashMap<>();
        for (CSLReferenceMark mark : marksInOrder) {
            String referenceMarkName = mark.getName();
            if (!ReferenceMark.isZoteroReferenceMarkName(referenceMarkName)) {
                continue;
            }

            ZoteroReferenceMark.extractZoteroUriByCitationKey(referenceMarkName)
                               .forEach(zoteroUriByCitationKey::putIfAbsent);
        }
        return zoteroUriByCitationKey;
    }

    public boolean isConversionNeededForFirstReferenceMark(OpenOfficeReferenceMarkFormat referenceMarkFormat) {
        sortMarksInOrder();
        return marksInOrder.stream()
                           .findFirst()
                           .map(mark -> !referenceMarkFormat.matchesReferenceMarkName(mark.getName()))
                           .orElse(false);
    }

    public int convertReferenceMarks(OpenOfficeReferenceMarkFormat referenceMarkFormat,
                                     List<BibDatabaseContext> bibDatabaseContexts,
                                     BibEntryTypesManager entryTypesManager) throws Exception, CreationException {
        sortMarksInOrder();
        Map<String, String> zoteroUriByCitationKey = getZoteroUriByCitationKey();
        int convertedMarks = 0;

        for (CSLReferenceMark mark : List.copyOf(marksInOrder)) {
            if (referenceMarkFormat.matchesReferenceMarkName(mark.getName())) {
                continue;
            }

            Optional<String> convertedName = buildConvertedReferenceMarkName(
                    mark,
                    referenceMarkFormat,
                    bibDatabaseContexts,
                    entryTypesManager,
                    zoteroUriByCitationKey);
            if (convertedName.isEmpty()) {
                LOGGER.warn("Could not convert reference mark: {}", mark.getName());
                continue;
            }

            if (updateReferenceMarkName(mark, convertedName.get())) {
                convertedMarks++;
            }
        }

        readAndUpdateExistingMarks();
        return convertedMarks;
    }

    private boolean updateReferenceMarkName(CSLReferenceMark mark, String markName) throws Exception, CreationException {
        Optional<XTextRange> range = Optional.ofNullable(mark.getTextContent().getAnchor());
        if (range.isEmpty()) {
            return false;
        }
        updateMarkAndText(mark, getCurrentCitationText(mark), markName);
        return true;
    }

    private Optional<String> buildConvertedReferenceMarkName(CSLReferenceMark mark,
                                                             OpenOfficeReferenceMarkFormat referenceMarkFormat,
                                                             List<BibDatabaseContext> bibDatabaseContexts,
                                                             BibEntryTypesManager entryTypesManager,
                                                             Map<String, String> zoteroUriByCitationKey) {
        return switch (referenceMarkFormat) {
            case JABREF_ONLY ->
                    Optional.of(JabRefReferenceMark.buildReferenceMarkName(
                            mark.getCitationKeys(),
                            mark.getCitationNumbers(),
                            mark.getUniqueId(),
                            mark.getCitationType()));
            case ZOTERO_COMPATIBLE ->
                    buildZoteroReferenceMarkName(mark, bibDatabaseContexts, entryTypesManager, zoteroUriByCitationKey);
        };
    }

    private Optional<String> buildZoteroReferenceMarkName(CSLReferenceMark mark,
                                                          List<BibDatabaseContext> bibDatabaseContexts,
                                                          BibEntryTypesManager entryTypesManager,
                                                          Map<String, String> zoteroUriByCitationKey) {
        Optional<List<BibEntry>> entries = findEntriesByCitationKeys(mark.getCitationKeys(), bibDatabaseContexts);
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        List<BibEntry> bibEntries = entries.get();
        ReferenceMark referenceMark = ZoteroReferenceMark.buildReferenceMark(
                bibEntries,
                mark.getCitationKeys(),
                mark.getCitationNumbers(),
                nextZoteroItemId,
                mark.getCitationType(),
                new BibDatabaseContext(new BibDatabase(bibEntries)),
                entryTypesManager,
                zoteroUriByCitationKey);
        nextZoteroItemId += bibEntries.size();
        return Optional.of(referenceMark.getName());
    }

    private Optional<List<BibEntry>> findEntriesByCitationKeys(List<String> citationKeys,
                                                               List<BibDatabaseContext> bibDatabaseContexts) {
        List<BibEntry> entries = new ArrayList<>();
        for (String citationKey : citationKeys) {
            Optional<BibEntry> entry = findEntryByCitationKey(citationKey, bibDatabaseContexts);
            if (entry.isEmpty()) {
                return Optional.empty();
            }
            entries.add(entry.orElseThrow());
        }
        return Optional.of(entries);
    }

    private Optional<BibEntry> findEntryByCitationKey(String citationKey,
                                                      List<BibDatabaseContext> bibDatabaseContexts) {
        return bibDatabaseContexts.stream()
                                  .map(BibDatabaseContext::getDatabase)
                                  .map(database -> database.getEntryByCitationKey(citationKey))
                                  .flatMap(Optional::stream)
                                  .findFirst();
    }

    public void insertReferenceIntoOO(List<BibEntry> entries,
                                      XTextDocument doc,
                                      XTextCursor position,
                                      OOText ooText,
                                      boolean insertSpaceBefore,
                                      boolean insertSpaceAfter,
                                      CSLCitationType citationType,
                                      BibDatabaseContext bibDatabaseContext,
                                      BibEntryTypesManager entryTypesManager,
                                      OpenOfficeReferenceMarkFormat referenceMarkFormat)
            throws CreationException, Exception {
        CSLReferenceMark mark = createReferenceMark(entries, citationType, bibDatabaseContext, entryTypesManager, referenceMarkFormat);
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
        UnoUserDefinedProperty.setStringProperty(document, FORMATTED_CITATION_TEXT_PROPERTY_PREFIX + mark.getUniqueId(), ooText.toString());
        mark.setFormattedCitationText(ooText);

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
        highestCitationNumber = 0;
        nextZoteroItemId = 1;
        citationType = CSLCitationType.NORMAL;

        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();
        Set<String> existingReferenceMarkUniqueIds = new HashSet<>();

        for (String name : marks.getElementNames()) {
            if (ReferenceMark.isReferenceMarkName(name)) {
                XNamed named = UnoRuntime.queryInterface(XNamed.class, marks.getByName(name));

                Optional<ReferenceMark> parsedReferenceMark;
                parsedReferenceMark = ReferenceMark.parse(name);
                if (parsedReferenceMark.isEmpty()) {
                    continue;
                }
                ReferenceMark referenceMark = parsedReferenceMark.get();
                List<String> citationKeys = referenceMark.getCitationKeys();
                List<Integer> citationNumbers = referenceMark.getCitationNumbers();

                if (!citationKeys.isEmpty() && !citationNumbers.isEmpty()) {
                    CSLReferenceMark mark = new CSLReferenceMark(named, referenceMark);
                    existingReferenceMarkUniqueIds.add(referenceMark.getUniqueId());
                    String storageKey = FORMATTED_CITATION_TEXT_PROPERTY_PREFIX + referenceMark.getUniqueId();
                    UnoUserDefinedProperty.getStringValue(document, storageKey)
                                          .map(OOText::fromString)
                                          .ifPresent(mark::setFormattedCitationText);
                    marksByName.put(name, mark);
                    marksInOrder.add(mark);
                    citationType = referenceMark.getCitationType();
                    if (ReferenceMark.isZoteroReferenceMarkName(name)) {
                        nextZoteroItemId = Math.max(nextZoteroItemId, ZoteroReferenceMark.getMaxItemId(name) + 1);
                    }
                } else {
                    LOGGER.warn("Cannot parse reference mark - invalid format: {}", name);
                }
            }
        }

        removeUnusedFormattedCitationTextProperties(existingReferenceMarkUniqueIds);
        rebuildCitationNumberState();

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

    private void removeUnusedFormattedCitationTextProperties(Set<String> existingReferenceMarkUniqueIds) {
        for (String propertyName : UnoUserDefinedProperty.getListOfNames(document)
                                                         .stream()
                                                         .filter(name -> name.startsWith(FORMATTED_CITATION_TEXT_PROPERTY_PREFIX))
                                                         .filter(name -> !existingReferenceMarkUniqueIds.contains(
                                                                 name.substring(FORMATTED_CITATION_TEXT_PROPERTY_PREFIX.length())))
                                                         .toList()) {
            try {
                UnoUserDefinedProperty.removeIfExists(document, propertyName);
            } catch (NotRemoveableException ex) {
                LOGGER.warn("Could not remove unused formatted CSL citation text property: {}", propertyName, ex);
            }
        }
    }

    private String getUpdatedReferenceMarkNameWithNewNumbers(String oldName, List<Integer> newNumbers) {
        if (ReferenceMark.isZoteroReferenceMarkName(oldName)) {
            return oldName;
        }

        String[] parts = oldName.split(" ");

        /*
         * e.g. "JABREF_Smith_2020 CID_1 abcd1234 EMPTY" is separated into 4 parts
         * The last part is the citation type
         * The second to last part is the uniqueId
         */
        String citationType = parts[parts.length - 1];
        int uniqueIdIndex = parts.length - 2;

        if (parts[0].startsWith(JabRefReferenceMark.PREFIXES[0]) && parts[1].startsWith(JabRefReferenceMark.PREFIXES[1]) && uniqueIdIndex >= 2) {
            StringBuilder newName = new StringBuilder();
            for (int i = 0; i < uniqueIdIndex; i += 2) {
                // Each iteration of the loop (incrementing by 2) represents one full citation (key + number)
                if (i > 0) {
                    newName.append(", ");
                }
                newName.append(parts[i]).append(" ");
                newName.append(JabRefReferenceMark.PREFIXES[1]).append(newNumbers.get(i / 2));
            }
            newName.append(" ").append(parts[uniqueIdIndex]).append(" ").append(citationType);
            return newName.toString();
        }
        return oldName;
    }

    private void rebuildCitationNumberState() {
        sortMarksInOrder();
        citationKeyToNumber.clear();
        highestCitationNumber = 0;

        for (CSLReferenceMark mark : marksInOrder) {
            List<String> citationKeys = mark.getCitationKeys();
            List<Integer> citationNumbers = new ArrayList<>(mark.getCitationNumbers());
            for (int i = 0; i < citationKeys.size(); i++) {
                String citationKey = citationKeys.get(i);
                int citationNumber = i < citationNumbers.size() ? citationNumbers.get(i) : 0;
                if (citationNumber <= 0) {
                    citationNumber = citationKeyToNumber.computeIfAbsent(citationKey, _ -> ++highestCitationNumber);
                    if (i < citationNumbers.size()) {
                        citationNumbers.set(i, citationNumber);
                    } else {
                        citationNumbers.add(citationNumber);
                    }
                } else {
                    citationKeyToNumber.putIfAbsent(citationKey, citationNumber);
                    highestCitationNumber = Math.max(highestCitationNumber, citationNumber);
                }
            }
            mark.setCitationNumbers(citationNumbers);
        }
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

            String currentCitationText = getCurrentCitationText(mark);
            mark.setCitationNumbers(assignedNumbers);
            updateMarkAndTextWithNewNumbers(mark, assignedNumbers, currentCitationText);
        }

        citationKeyToNumber = newCitationKeyToNumber;
    }

    static String getUpdatedCitationTextWithNewNumbers(String currentText, List<Integer> newNumbers) {
        StringBuilder result = new StringBuilder();
        int numberIndex = 0;
        int lastEnd = 0;

        Matcher tagMatcher = HTML_TAG_PATTERN.matcher(currentText);
        while (tagMatcher.find()) {
            numberIndex = appendUpdatedCitationTextSegment(
                    result,
                    currentText.substring(lastEnd, tagMatcher.start()),
                    newNumbers,
                    numberIndex);
            result.append(currentText, tagMatcher.start(), tagMatcher.end());
            lastEnd = tagMatcher.end();
        }

        appendUpdatedCitationTextSegment(
                result,
                currentText.substring(lastEnd),
                newNumbers,
                numberIndex);

        return result.toString();
    }

    private static int appendUpdatedCitationTextSegment(StringBuilder result, String textSegment, List<Integer> newNumbers, int numberIndex) {
        Matcher matcher = CITATION_NUMBER_PATTERN.matcher(textSegment);
        int lastEnd = 0;

        while (numberIndex < newNumbers.size() && matcher.find()) {
            result.append(textSegment, lastEnd, matcher.start());
            result.append(newNumbers.get(numberIndex));
            numberIndex++;
            lastEnd = matcher.end();
        }

        result.append(textSegment.substring(lastEnd));
        return numberIndex;
    }

    private void updateMarkAndTextWithNewNumbers(CSLReferenceMark mark, List<Integer> newNumbers, String currentText) throws Exception, CreationException {
        String updatedName = getUpdatedReferenceMarkNameWithNewNumbers(mark.getName(), newNumbers);
        String updatedText = getUpdatedCitationTextWithNewNumbers(currentText, newNumbers);

        updateMarkAndText(mark, updatedText, updatedName);

        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, document);
        XNameAccess marks = supplier.getReferenceMarks();
        XTextContent newContent = UnoRuntime.queryInterface(XTextContent.class, marks.getByName(updatedName));

        mark.updateTextContent(newContent);
        mark.updateName(updatedName);
        mark.setCitationNumbers(newNumbers);
    }

    public void updateMarkAndTextWithNewStyle(CSLReferenceMark mark, String newText, CSLCitationType citationType) throws Exception, CreationException {
        String updatedName = mark.getName();
        if (ReferenceMark.isZoteroReferenceMarkName(updatedName)) {
            updateMarkAndText(mark, newText, ZoteroReferenceMark.updateCitationType(updatedName, citationType));
            return;
        }

        // Remove citation marker first
        if (updatedName.endsWith(JabRefReferenceMark.IN_TEXT_MARKER)) {
            updatedName = updatedName.substring(0, updatedName.length() - JabRefReferenceMark.IN_TEXT_MARKER.length() - 1);
        } else if (updatedName.endsWith(JabRefReferenceMark.EMPTY_MARKER)) {
            updatedName = updatedName.substring(0, updatedName.length() - JabRefReferenceMark.EMPTY_MARKER.length() - 1);
        } else if (updatedName.endsWith(JabRefReferenceMark.NORMAL_MARKER)) {
            updatedName = updatedName.substring(0, updatedName.length() - JabRefReferenceMark.NORMAL_MARKER.length() - 1);
        }

        // Then add the new marker
        String marker = switch (citationType) {
            case IN_TEXT ->
                    JabRefReferenceMark.IN_TEXT_MARKER;
            case EMPTY ->
                    JabRefReferenceMark.EMPTY_MARKER;
            case NORMAL ->
                    JabRefReferenceMark.NORMAL_MARKER;
        };

        updateMarkAndText(mark, newText, updatedName + " " + marker);
    }

    private void updateMarkAndText(CSLReferenceMark mark, String newText, String markName) throws Exception, CreationException {
        XTextContent oldContent = mark.getTextContent();
        XTextRange range = oldContent.getAnchor();
        String oldUniqueId = mark.getUniqueId();

        if (range != null) {
            XText text = range.getText();
            XTextCursor cursor = text.createTextCursorByRange(range);
            OOText ooText = OOText.fromString(newText);

            // The only way to edit a reference mark is to remove it and add a new one
            // Remove old reference mark but keep cursor position
            text.removeTextContent(oldContent);

            // Store the start position before writing
            XTextRange startRange = cursor.getStart();
            XTextCursor writeCursor = text.createTextCursorByRange(startRange);
            OOTextIntoOO.removeEscapementFormatting(writeCursor);

            // Update the text using OOTextIntoOO
            OOTextIntoOO.write(document, writeCursor, ooText);

            // Store the end position after writing
            XTextRange endRange = writeCursor.getEnd();

            // Move cursor to wrap the entire inserted content
            cursor.gotoRange(startRange, false);
            cursor.gotoRange(endRange, true);

            // Create and attach DocumentAnnotation
            DocumentAnnotation documentAnnotation = new DocumentAnnotation(document, markName, cursor, true);
            UnoReferenceMark.create(documentAnnotation);
            Optional<ReferenceMark> newReferenceMark = ReferenceMark.parse(markName);
            if (newReferenceMark.isEmpty()) {
                LOGGER.warn("Could not store citation format for reference mark: {}", markName);
            } else {
                ReferenceMark referenceMark = newReferenceMark.orElseThrow();
                String newUniqueId = referenceMark.getUniqueId();
                UnoUserDefinedProperty.setStringProperty(document, FORMATTED_CITATION_TEXT_PROPERTY_PREFIX + newUniqueId, ooText.toString());
                mark.setFormattedCitationText(ooText);
                if (!oldUniqueId.equals(newUniqueId)) {
                    UnoUserDefinedProperty.removeIfExists(document, FORMATTED_CITATION_TEXT_PROPERTY_PREFIX + oldUniqueId);
                }
            }

            // Move cursor to the end
            cursor.gotoRange(endRange, false);
        }
    }

    private String getCurrentCitationText(CSLReferenceMark mark) throws WrappedTargetException {
        Optional<OOText> formattedCitationText = mark.getFormattedCitationText();
        if (formattedCitationText.isEmpty()) {
            String storageKey = FORMATTED_CITATION_TEXT_PROPERTY_PREFIX + mark.getUniqueId();
            formattedCitationText = UnoUserDefinedProperty.getStringValue(document, storageKey)
                                                          .map(OOText::fromString);
            formattedCitationText.ifPresent(mark::setFormattedCitationText);
        }

        if (formattedCitationText.isPresent()) {
            return formattedCitationText.get().toString();
        }

        XTextRange range = mark.getTextContent().getAnchor();
        if (range == null) {
            return "";
        }

        LOGGER.debug("Formatted CSL citation text is missing for reference mark {}", mark.getName());
        return range.getString();
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

    public CSLCitationType getCitationType() {
        return citationType;
    }

    public void setRealTimeNumberUpdateRequired(boolean isNumeric) {
        this.isNumberUpdateRequired = isNumeric;
    }

    private void sortMarksInOrder() {
        List<RangeSortEntry<CSLReferenceMark>> sortEntries = new ArrayList<>();

        for (CSLReferenceMark mark : marksInOrder) {
            XTextRange range = mark.getTextContent().getAnchor();
            if (range == null) {
                LOGGER.debug("Skipping dangling reference mark without anchor: {}", mark.getName());
                continue;
            }
            sortEntries.add(new RangeSortEntry<>(range, 0, mark));
        }

        RangeSort.RangePartitions<RangeSortEntry<CSLReferenceMark>> partitions =
                RangeSort.partitionAndSortRanges(sortEntries);

        for (List<RangeSortEntry<CSLReferenceMark>> partition : partitions.getPartitions()) {
            int indexInPartition = 0;
            for (RangeSortEntry<CSLReferenceMark> sortEntry : partition) {
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

    private int compareTextRanges(RangeSortEntry<CSLReferenceMark> first, RangeSortEntry<CSLReferenceMark> second) {
        int rangeComparison;
        try {
            rangeComparison = textRangeCompare.compareRegionStarts(second.getRange(), first.getRange());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Error comparing text ranges: {}", e.getMessage(), e);
            rangeComparison = 0;
        }

        if (rangeComparison != 0) {
            return rangeComparison;
        }

        return Integer.compare(first.getIndexInPosition(), second.getIndexInPosition());
    }
}
