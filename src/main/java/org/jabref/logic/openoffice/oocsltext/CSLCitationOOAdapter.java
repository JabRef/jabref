package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.gui.StateManager;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

import com.airhacks.afterburner.injection.Injector;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Exception;

/**
 * This class processes CSL citations in JabRef and interacts directly with LibreOffice using an XTextDocument instance.
 * It is tightly coupled with {@link CSLReferenceMarkManager} for management of reference marks tied to the CSL citations.
 * Any method in this class is NOT supposed to be moved.
 */
public class CSLCitationOOAdapter {

    private final XTextDocument document;
    private final CSLReferenceMarkManager markManager;


    private CitationStyle currentStyle;
    private boolean styleChanged;

    public CSLCitationOOAdapter(XTextDocument doc) {
        this.document = doc;
        this.markManager = new CSLReferenceMarkManager(doc);
    }

    public void setStyle(CitationStyle newStyle) {
        if (currentStyle == null || !currentStyle.getName().equals(newStyle.getName())) {
            styleChanged = true;
            currentStyle = newStyle;
        } else {
            styleChanged = false;
        }
    }

    public void readAndUpdateExistingMarks() throws WrappedTargetException, NoSuchElementException {
        markManager.readAndUpdateExistingMarks();
    }

    /**
     * Inserts a citation for a group of entries.
     * Comparable to LaTeX's \cite command.
     */
    public void insertCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws CreationException, IOException, Exception {
        setStyle(selectedStyle);

        String style = selectedStyle.getSource();
        boolean isAlphanumericStyle = selectedStyle.isAlphanumericStyle();

        String citation;

        if (isAlphanumericStyle) {
            citation = CSLFormatUtils.generateAlphanumericCitation(entries, bibDatabaseContext);
        } else {
            citation = CitationStyleGenerator.generateCitation(entries, style, CSLFormatUtils.OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager).getText();
        }

        String formattedCitation = CSLFormatUtils.transformHTML(citation);

        if (selectedStyle.isNumericStyle()) {
            formattedCitation = updateSingleOrMultipleCitationNumbers(formattedCitation, entries);
        }

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));
        insertReferences(cursor, entries, ooText, selectedStyle.isNumericStyle());

        if (styleChanged) {
            updateAllCitationsWithNewStyle(currentStyle);
            styleChanged = false;
        }
    }

    /**
     * Inserts in-text citations for a group of entries.
     * Comparable to LaTeX's \citet command.
     *
     * @implNote Very similar to the {@link #insertCitation(XTextCursor, CitationStyle, List, BibDatabaseContext, BibEntryTypesManager) insertCitation} method.
     */
    public void insertInTextCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws IOException, CreationException, Exception {
        String style = selectedStyle.getSource();
        boolean isNumericStyle = selectedStyle.isNumericStyle();
        boolean isAlphanumericStyle = selectedStyle.isAlphanumericStyle();

        Iterator<BibEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            BibEntry currentEntry = iterator.next();

            String inTextCitation;

            if (isAlphanumericStyle) {
                inTextCitation = CSLFormatUtils.generateAlphanumericInTextCitation(currentEntry, bibDatabaseContext);
            } else {
                inTextCitation = CitationStyleGenerator.generateCitation(List.of(currentEntry), style, CSLFormatUtils.OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager).getText();
            }

            String formattedCitation = CSLFormatUtils.transformHTML(inTextCitation);
            String finalText;

            if (isNumericStyle) {
                formattedCitation = updateSingleOrMultipleCitationNumbers(formattedCitation, List.of(currentEntry));
                String prefix = CSLFormatUtils.generateAuthorPrefix(currentEntry, bibDatabaseContext);
                finalText = prefix + formattedCitation;
            } else if (isAlphanumericStyle) {
                finalText = formattedCitation;
            } else {
                finalText = CSLFormatUtils.changeToInText(formattedCitation);
            }

            if (iterator.hasNext()) {
                finalText += ",";
            }

            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(finalText));
            insertReferences(cursor, List.of(currentEntry), ooText, selectedStyle.isNumericStyle());
        }
    }

    /**
     * Inserts "empty" citations for a list of entries at the cursor to the document.
     * Adds the entries to the list for which bibliography is to be generated.
     */
    public void insertEmptyCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries)
            throws CreationException, Exception {
        OOText emptyOOText = OOFormat.setLocaleNone(OOText.fromString(""));
        insertReferences(cursor, entries, emptyOOText, selectedStyle.isNumericStyle());
    }

    /**
     * Creates a "Bibliography" section in the document and inserts a list of references.
     * The list is generated based on the existing citations, in-text citations and empty citations in the document.
     */
    public void insertBibliography(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws WrappedTargetException, CreationException, NoSuchElementException {
        markManager.setRealTimeNumberUpdateRequired(selectedStyle.isNumericStyle());
        markManager.readAndUpdateExistingMarks();

        OOText title = OOFormat.paragraph(OOText.fromString(CSLFormatUtils.DEFAULT_BIBLIOGRAPHY_TITLE), CSLFormatUtils.DEFAULT_BIBLIOGRAPHY_HEADER_PARAGRAPH_FORMAT);
        OOTextIntoOO.write(document, cursor, OOText.fromString(title.toString()));
        OOText ooBreak = OOFormat.paragraph(OOText.fromString(""), CSLFormatUtils.DEFAULT_BIBLIOGRAPHY_BODY_PARAGRAPH_FORMAT);
        OOTextIntoOO.write(document, cursor, ooBreak);

        String style = selectedStyle.getSource();

        if (selectedStyle.isNumericStyle()) {
            // Sort entries based on their order of appearance in the document
            entries.sort(Comparator.comparingInt(entry -> markManager.getCitationNumber(entry.getCitationKey().orElse(""))));

            for (BibEntry entry : entries) {
                String bibliographyEntry = CitationStyleGenerator.generateBibliography(List.of(entry), style, CSLFormatUtils.OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager).getFirst();
                String citationKey = entry.getCitationKey().orElse("");
                int currentNumber = markManager.getCitationNumber(citationKey);

                String formattedBibliographyEntry = CSLFormatUtils.updateSingleBibliographyNumber(CSLFormatUtils.transformHTML(bibliographyEntry), currentNumber);
                OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedBibliographyEntry));

                OOTextIntoOO.write(document, cursor, ooText);
            }
        } else {
            // Ordering will be according to citeproc item data provider (default)
            List<String> bibliographyEntries = CitationStyleGenerator.generateBibliography(entries, style, CSLFormatUtils.OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager);

            for (String bibliographyEntry : bibliographyEntries) {
                String formattedBibliographyEntry = CSLFormatUtils.transformHTML(bibliographyEntry);
                OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedBibliographyEntry));
                OOTextIntoOO.write(document, cursor, ooText);
            }
        }
    }

    /**
     * Inserts references and also adds a space before the citation if not already present ("smart space").
     */
    private void insertReferences(XTextCursor cursor, List<BibEntry> entries, OOText ooText, boolean isNumericStyle)
            throws CreationException, Exception {
        boolean preceedingSpaceExists;
        XTextCursor checkCursor = cursor.getText().createTextCursorByRange(cursor.getStart());

        // Check if we're at the start of the document - if yes we set the flag and don't insert a space
        if (!checkCursor.goLeft((short) 1, true)) {
            // We're at the start of the document
            preceedingSpaceExists = true;
        } else {
            // If not at the start of document, check if there is a space before
            preceedingSpaceExists = " ".equals(checkCursor.getString());
            // If not a space, check if it's a paragraph break
            if (!preceedingSpaceExists) {
                preceedingSpaceExists = checkCursor.getString().matches("\\R");
            }
        }

        markManager.insertReferenceIntoOO(entries, document, cursor, ooText, !preceedingSpaceExists, false);
        markManager.setRealTimeNumberUpdateRequired(isNumericStyle);
        markManager.readAndUpdateExistingMarks();
    }

    /**
     * Transforms the numbers in the citation to globally-unique (and thus, reusable) numbers.
     */
    private String updateSingleOrMultipleCitationNumbers(String citation, List<BibEntry> entries) {
        Pattern pattern = Pattern.compile("(\\D*)(\\d+)(\\D*)");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();
        Iterator<BibEntry> iterator = entries.iterator();

        while (matcher.find() && iterator.hasNext()) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(3);

            int currentNumber = markManager.getCitationNumber(iterator.next().getCitationKey().orElse(""));

            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + currentNumber + suffix));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Ideally, the methods of this class are supposed to work with {@link CSLReferenceMarkManager}, and not {@link CSLReferenceMark} directly.
     * However, all "generation" of CSL style citations (via {@link CitationStyleGenerator}) occur in this class, and not in {@link CSLReferenceMarkManager}.
     * Furthermore, {@link CSLReferenceMarkManager} is not composed of {@link CitationStyle}.
     * Hence, we keep {@link CSLReferenceMarkManager} independent of {@link CitationStyleGenerator} and {@link CitationStyle} and keep this method here.
     */
    private void updateAllCitationsWithNewStyle(CitationStyle style)
            throws IOException, Exception {
        /*
        Entries from multiple libraries may need to be updated, and new libraries could have been opened after the document connection
        So, to get all databases in real time without having to refresh the connection, we obtain all open databases via the state manager
         */
        StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

        List<BibDatabase> databases = new ArrayList<>();
        for (BibDatabaseContext database : stateManager.getOpenDatabases()) {
                databases.add(database.getDatabase());
        }

        // We first get a list of all cited entries to create a unified database context
        List<BibEntry> citedEntries = new ArrayList<>();
        for (BibDatabase database : databases) {
            for (BibEntry entry : database.getEntries()) {
                if (isCitedEntry(entry)) {
                    citedEntries.add(entry);
                }
            }
        }

        BibDatabase unifiedDatabase = new BibDatabase(citedEntries);
        BibDatabaseContext unifiedBibDatabaseContext = new BibDatabaseContext(unifiedDatabase);
        BibEntryTypesManager bibEntryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);

        List<CSLReferenceMark> marksInOrder = markManager.getMarksInOrder();

        // Now for each reference mark, in order of appearance in the document, we get the entries to be updated
        for (CSLReferenceMark mark : marksInOrder) {
            List<String> citationKeys = mark.getCitationKeys();
            List<BibEntry> entries = citationKeys.stream()
                                                 .map(unifiedDatabase::getEntryByCitationKey)
                                                 .filter(java.util.Optional::isPresent)
                                                 .map(java.util.Optional::get)
                                                 .collect(Collectors.toList());

            // We update the entries with the new style
            String newCitation;
            if (style.isAlphanumericStyle()) {
                newCitation = CSLFormatUtils.generateAlphanumericCitation(entries, unifiedBibDatabaseContext);
            } else {
                newCitation = CitationStyleGenerator.generateCitation(entries, style.getSource(),
                        CSLFormatUtils.OUTPUT_FORMAT, unifiedBibDatabaseContext, bibEntryTypesManager).getText();
            }

            String formattedCitation = CSLFormatUtils.transformHTML(newCitation);
            markManager.updateMarkAndTextWithNewStyle(mark, formattedCitation);
        }
    }

    /**
     * Checks if an entry has already been cited before in the document.
     * Required for consistent numbering of numeric citations - if present, the number is to be reused, else a new number is to be assigned.
     */
    public boolean isCitedEntry(BibEntry entry) {
        String citationKey = entry.getCitationKey().orElse("");
        return markManager.hasCitationForKey(citationKey);
    }
}
