package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.BracketedPattern;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

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

    public CSLCitationOOAdapter(XTextDocument doc) {
        this.document = doc;
        this.markManager = new CSLReferenceMarkManager(doc);
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
        String style = selectedStyle.getSource();
        boolean isAlphanumeric = isAlphanumericStyle(selectedStyle);

        String inTextCitation;
        if (isAlphanumeric) {
            inTextCitation = CSLFormatUtils.generateAlphanumericCitation(entries, bibDatabaseContext);
        } else {
            inTextCitation = CitationStyleGenerator.generateCitation(entries, style, CSLFormatUtils.OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager).getText();
        }

        String formattedCitation = CSLFormatUtils.transformHTML(inTextCitation);

        if (selectedStyle.isNumericStyle()) {
            formattedCitation = updateSingleOrMultipleCitationNumbers(formattedCitation, entries);
        }

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));
        insertReferences(cursor, entries, ooText, selectedStyle.isNumericStyle());
        cursor.collapseToEnd();
    }

    /**
     * Inserts in-text citations for a group of entries.
     * Comparable to LaTeX's \citet command.
     *
     * @implNote Very similar to the {@link #insertCitation(XTextCursor, CitationStyle, List, BibDatabaseContext, BibEntryTypesManager)} method.insertInText method
     */
    public void insertInTextCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws IOException, CreationException, Exception {
        String style = selectedStyle.getSource();
        boolean isAlphanumeric = isAlphanumericStyle(selectedStyle);

        Iterator<BibEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            BibEntry currentEntry = iterator.next();
            String inTextCitation;
            if (isAlphanumeric) {
                // Generate the alphanumeric citation
                inTextCitation = CSLFormatUtils.generateAlphanumericCitation(List.of(currentEntry), bibDatabaseContext);
                // Get the author's name
                String authorName = currentEntry.getResolvedFieldOrAlias(StandardField.AUTHOR, bibDatabaseContext.getDatabase())
                                                .map(AuthorList::parse)
                                                .map(list -> BracketedPattern.joinAuthorsOnLastName(list, 1, "", " et al."))
                                                .orElse("");
                // Combine author name with the citation
                inTextCitation = authorName + " " + inTextCitation;
            } else {
                inTextCitation = CitationStyleGenerator.generateCitation(List.of(currentEntry), style, CSLFormatUtils.OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager).getText();
            }
            String formattedCitation = CSLFormatUtils.transformHTML(inTextCitation);
            String finalText;
            if (selectedStyle.isNumericStyle()) {
                formattedCitation = updateSingleOrMultipleCitationNumbers(formattedCitation, List.of(currentEntry));
                String prefix = currentEntry.getResolvedFieldOrAlias(StandardField.AUTHOR, bibDatabaseContext.getDatabase())
                                            .map(AuthorList::parse)
                                            .map(list -> BracketedPattern.joinAuthorsOnLastName(list, 1, "", " et al.") + " ")
                                            .orElse("");
                finalText = prefix + formattedCitation;
            } else if (isAlphanumeric) {
                finalText = formattedCitation;
            } else {
                finalText = CSLFormatUtils.changeToInText(formattedCitation);
            }
            if (iterator.hasNext()) {
                finalText += ",";
            }
            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(finalText));
            insertReferences(cursor, List.of(currentEntry), ooText, selectedStyle.isNumericStyle());
            cursor.collapseToEnd();
        }
    }

    /**
     * Inserts "empty" citations for a list of entries at the cursor to the document.
     * Adds the entries to the list for which bibliography is to be generated.
     */
    public void insertEmpty(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries)
            throws CreationException, Exception {
        OOText emptyOOText = OOFormat.setLocaleNone(OOText.fromString(""));
        insertReferences(cursor, entries, emptyOOText, selectedStyle.isNumericStyle());

        // Move the cursor to the end of the inserted text - although no need as we don't insert any text, but a good practice
        cursor.collapseToEnd();
    }

    /**
     * Creates a "Bibliography" section in the document and inserts a list of references.
     * The list is generated based on the existing citations, in-text citations and empty citations in the document.
     */
    public void insertBibliography(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws WrappedTargetException, CreationException, NoSuchElementException {
        markManager.setUpdateRequired(selectedStyle.isNumericStyle());
        readAndUpdateExistingMarks();

        OOText title = OOFormat.paragraph(OOText.fromString(CSLFormatUtils.DEFAULT_BIBLIOGRAPHY_TITLE), CSLFormatUtils.DEFAULT_BIBLIOGRAPHY_HEADER_PARAGRAPH_FORMAT);
        OOTextIntoOO.write(document, cursor, OOText.fromString(title.toString()));
        OOText ooBreak = OOFormat.paragraph(OOText.fromString(""), CSLFormatUtils.DEFAULT_BIBLIOGRAPHY_BODY_PARAGRAPH_FORMAT);
        OOTextIntoOO.write(document, cursor, ooBreak);

        String style = selectedStyle.getSource();

        if (selectedStyle.isNumericStyle()) {
            // Sort entries based on their order of appearance in the document
            entries.sort(Comparator.comparingInt(entry -> markManager.getCitationNumber(entry.getCitationKey().orElse(""))));

            for (BibEntry entry : entries) {
                String citation = CitationStyleGenerator.generateBibliography(List.of(entry), style, CSLFormatUtils.OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager).getFirst();
                String citationKey = entry.getCitationKey().orElse("");
                int currentNumber = markManager.getCitationNumber(citationKey);

                String formattedCitation = CSLFormatUtils.updateSingleBibliographyNumber(CSLFormatUtils.transformHTML(citation), currentNumber);
                OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));

                OOTextIntoOO.write(document, cursor, ooText);
                    // Select the paragraph break
                    cursor.goLeft((short) 1, true);

                    // Delete the selected content (paragraph break)
                    cursor.setString("");
            }
        } else {
            // Ordering will be according to citeproc item data provider (default)
            List<String> citations = CitationStyleGenerator.generateBibliography(entries, style, CSLFormatUtils.OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager);

            for (String citation : citations) {
                String formattedCitation = CSLFormatUtils.transformHTML(citation);
                OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));
                OOTextIntoOO.write(document, cursor, ooText);
            }
            cursor.collapseToEnd();
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

        CSLReferenceMark mark = markManager.createReferenceMark(entries);
        mark.insertReferenceIntoOO(document, cursor, ooText, !preceedingSpaceExists, false);

        // Move the cursor to the end of the inserted text
        cursor.collapseToEnd();

        markManager.setUpdateRequired(isNumericStyle);
        readAndUpdateExistingMarks();
        cursor.collapseToEnd();
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
     * Checks if an entry has already been cited before in the document.
     * Required for consistent numbering of numeric citations - if present, the number is to be reused, else a new number is to be assigned.
     */
    public boolean isCitedEntry(BibEntry entry) {
        String citationKey = entry.getCitationKey().orElse("");
        return markManager.hasCitationForKey(citationKey);
    }

    /**
     * Currently, we have support for one alphanumeric CSL style.
     * There is no tag or field in .csl style files that can be parsed to determine if it is an alphanumeric style.
     * Thus, we currently hardcode the check for "DIN 1505-2".
     */
    private boolean isAlphanumericStyle(CitationStyle style) {
        return "DIN 1505-2 (alphanumeric, Deutsch) - standard superseded by ISO-690".equals(style.getTitle());
    }
}
