package org.jabref.logic.openoffice.oocsltext;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.BracketedPattern;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoTextSection;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.apache.commons.text.StringEscapeUtils;

public class CSLCitationOOAdapter {

    private static final Pattern YEAR_IN_CITATION_PATTERN = Pattern.compile("(.)(.*), (\\d{4}.*)");
    public static final String[] PREFIXES = {"JABREF_", "CSL_"};
    // TODO: These are static final fields right now, should add the functionality to let user select these and store them in preferences.
    public static final String BIBLIOGRAPHY_TITLE = "References";
    public static final String BIBLIOGRAPHY_HEADER_PARAGRAPH_FORMAT = "Heading 2";

    private final CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;
    private final XTextDocument document;
    private final CSLReferenceMarkManager markManager;
    private boolean isNumericStyle = false;

    public CSLCitationOOAdapter(XTextDocument doc) throws Exception {
        this.document = doc;
        this.markManager = new CSLReferenceMarkManager(doc);
    }

    public void readExistingMarks() throws Exception {
        markManager.readExistingMarks();
    }

    public static Optional<XTextRange> getBibliographyRange(XTextDocument doc)
            throws
            NoDocumentException,
            WrappedTargetException {
        return UnoTextSection.getAnchor(doc, "CSL_bibliography");
    }

    public void insertBibliography(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws Exception {
//        XTextCursor bibliographyCursor = document.getText().createTextCursor();
//        bibliographyCursor.gotoEnd(false);
//        // My approach:
//        // Insert the bibliography title
//        XTextRange titleRange = bibliographyCursor.getStart();
//        titleRange.setString(BIBLIOGRAPHY_TITLE);
//
//        // Set the paragraph style for the title
//        XPropertySet titleProps = UnoRuntime.queryInterface(XPropertySet.class, titleRange);
//        titleProps.setPropertyValue("ParaStyleName", BIBLIOGRAPHY_HEADER_PARAGRAPH_FORMAT);
//
//        // Move the cursor to the end of the title
//        bibliographyCursor.gotoEnd(false);
//
//        // Insert a paragraph break after the title
//        bibliographyCursor.getText().insertControlCharacter(bibliographyCursor, ControlCharacter.PARAGRAPH_BREAK, false);
//
//        // Create a new paragraph for references and set its style
//        XTextRange refParagraph = bibliographyCursor.getStart();
//        XPropertySet refParaProps = UnoRuntime.queryInterface(XPropertySet.class, refParagraph);
//        refParaProps.setPropertyValue("ParaStyleName", "Body Text");
        // Always creating at the end of the document.
        // Alternatively, we could receive a cursor.
//        XTextCursor textCursor = document.getText().createTextCursor();
//        textCursor.gotoEnd(false);
//        DocumentAnnotation annotation = new DocumentAnnotation(document, "CSL_bibliography", textCursor, false);
//        UnoTextSection.create(annotation);

        // antalk2's derivative
        OOText title = OOFormat.paragraph(OOText.fromString(BIBLIOGRAPHY_TITLE), BIBLIOGRAPHY_HEADER_PARAGRAPH_FORMAT);
        OOTextIntoOO.write(document, cursor, OOText.fromString(title.toString()));
        OOText ooBreak = OOFormat.paragraph(OOText.fromString(""), "Body Text");
        OOTextIntoOO.write(document, cursor, ooBreak);

        String style = selectedStyle.getSource();
        isNumericStyle = selectedStyle.isNumericStyle();

        // Sort entries based on their order of appearance in the document
        entries.sort(Comparator.comparingInt(entry -> markManager.getCitationNumber(entry.getCitationKey().orElse(""))));
        for (BibEntry entry : entries) {
            String citation = CitationStyleGenerator.generateCitation(List.of(entry), style, format, bibDatabaseContext, bibEntryTypesManager).getFirst();
            String citationKey = entry.getCitationKey().orElse("");
            int currentNumber = markManager.getCitationNumber(citationKey);
            System.out.println(citation);

            String formattedCitation;
            if (isNumericStyle) {
                formattedCitation = updateSingleCitation(transformHtml(citation), currentNumber);
            } else {
                formattedCitation = transformHtml(citation);
            }
            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));

            OOTextIntoOO.write(document, cursor, ooText);
            if (isNumericStyle) {
                // Select the paragraph break
                cursor.goLeft((short) 1, true);

                // Delete the selected content (paragraph break)
                cursor.setString("");
            }
        }
        // remove the initial empty paragraph from the section.
//        XTextRange sectionRange = getBibliographyRange(document).orElseThrow(IllegalStateException::new);
//        XTextCursor initialParagraph = document.getText().createTextCursorByRange(sectionRange);
//        initialParagraph.collapseToStart();
//        initialParagraph.goRight((short) 1, true);
//        initialParagraph.setString("");
//        // TODO: Chris help - newlines!!
    }

    public void insertCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager) throws Exception {
        String style = selectedStyle.getSource();
        isNumericStyle = selectedStyle.isNumericStyle();

        // Generate a single in-text citation for a group of entries
        String inTextCitation = CitationStyleGenerator.generateInText(entries, style, format, bibDatabaseContext, bibEntryTypesManager).getText();

        String formattedCitation = transformHtml(inTextCitation);

        if (isNumericStyle) {
            formattedCitation = updateMultipleCitations(formattedCitation, entries);
        }

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));
        insertMultipleReferenceMarks(cursor, entries, ooText);
        cursor.collapseToEnd();
    }

    /**
     * Inserts the in-text citation for a group of entries.
     * Comparable to LaTeX's \citet command.
     *
     * @implNote Very similar to the {@link #insertCitation(XTextCursor, CitationStyle, List, BibDatabaseContext, BibEntryTypesManager)} method.insertInText method
     */
    public void insertInTextCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws Exception {
        String style = selectedStyle.getSource();
        isNumericStyle = selectedStyle.isNumericStyle();

        boolean twoEntries = entries.size() == 2;

        Iterator<BibEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            BibEntry currentEntry = iterator.next();
            String inTextCitation = CitationStyleGenerator.generateInText(List.of(currentEntry), style, format, bibDatabaseContext, bibEntryTypesManager).getText();
            String formattedCitation = transformHtml(inTextCitation);
            String finalText;
            if (isNumericStyle) {
                formattedCitation = updateMultipleCitations(formattedCitation, List.of(currentEntry));
                String prefix = currentEntry.getResolvedFieldOrAlias(StandardField.AUTHOR, bibDatabaseContext.getDatabase())
                                            .map(authors -> AuthorList.parse(authors))
                                            .map(list -> BracketedPattern.joinAuthorsOnLastName(list, 1, "", " et al.") + " ")
                                            .orElse("");
                finalText = prefix + formattedCitation;
            } else {
                finalText = changeToInText(formattedCitation);
            }
            if (iterator.hasNext()) {
                finalText += ",";
                // The next space is inserted somehow magically by other routines. Therefore, we do not add a space here.
            }
            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(finalText));
            insertMultipleReferenceMarks(cursor, List.of(currentEntry), ooText);
            cursor.collapseToEnd();
        }
    }

    private String changeToInText(String formattedCitation) {
        Matcher matcher = YEAR_IN_CITATION_PATTERN.matcher(formattedCitation);
        if (matcher.find()) {
            return matcher.group(2) + " " + matcher.group(1) + matcher.group(3);
        }
        return formattedCitation;
    }

    public void insertEmpty(XTextCursor cursor, List<BibEntry> entries)
            throws Exception {
        for (BibEntry entry : entries) {
            CSLReferenceMark mark = markManager.createReferenceMark(entry);
            OOText emptyOOText = OOFormat.setLocaleNone(OOText.fromString(""));
            mark.insertReferenceIntoOO(document, cursor, emptyOOText, false, false, true);
        }

        // Move the cursor to the end of the inserted text - although no need as we don't insert any text, but a good practice
        cursor.collapseToEnd();
    }

    private void insertMultipleReferenceMarks(XTextCursor cursor, List<BibEntry> entries, OOText ooText) throws Exception {
        boolean preceedingSpaceExists;
        XTextCursor checkCursor = cursor.getText().createTextCursorByRange(cursor.getStart());

        // Check if we're at the start of the document - if yes we set the flag and don't insert a space
        if (!checkCursor.goLeft((short) 1, true)) {
            // We're at the start of the document
            preceedingSpaceExists = true;
        } else {
            // If not at the start of document, check if there is a space before
            preceedingSpaceExists = checkCursor.getString().equals(" ");
            // If not a space, check if it's a paragraph break
            if (!preceedingSpaceExists) {
                preceedingSpaceExists = checkCursor.getString().matches("\\R");
            }
        }

        if (entries.size() == 1) {
            CSLReferenceMark mark = markManager.createReferenceMark(entries.getFirst());
            mark.insertReferenceIntoOO(document, cursor, ooText, !preceedingSpaceExists, false, true);
        } else {
            if (!preceedingSpaceExists) {
                cursor.getText().insertString(cursor, " ", false);
            }
            OOTextIntoOO.write(document, cursor, ooText);
            for (BibEntry entry : entries) {
                CSLReferenceMark mark = markManager.createReferenceMark(entry);
                OOText emptyOOText = OOFormat.setLocaleNone(OOText.fromString(""));
                mark.insertReferenceIntoOO(document, cursor, emptyOOText, false, false, true);
            }
        }

        // Move the cursor to the end of the inserted text
        cursor.collapseToEnd();
    }

    /**
     * Transforms the numbers in the citation to globally-unique numbers
     */
    private String updateMultipleCitations(String citation, List<BibEntry> entries) {
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

    private void writeCitation(XTextDocument doc, XTextCursor cursor, BibEntry entry, String citation) throws Exception {
        String citationKey = entry.getCitationKey().orElse("");
        int currentNumber = markManager.getCitationNumber(citationKey);

        CSLReferenceMark mark = markManager.createReferenceMark(entry);
        String formattedCitation;
        if (isNumericStyle) {
            formattedCitation = updateSingleCitation(transformHtml(citation), currentNumber);
        } else {
            formattedCitation = transformHtml(citation);
        }
        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));

        // Insert the citation text wrapped in a reference mark
        mark.insertReferenceIntoOO(doc, cursor, ooText, false, false, true);

        // Move the cursor to the end of the inserted text
        cursor.collapseToEnd();
    }

    public static String updateSingleCitation(String citation, int currentNumber) {
        Pattern pattern = Pattern.compile("(\\[|\\()?(\\d+)(\\]|\\))?(\\.)?\\s*");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();
        boolean numberReplaced = false;

        while (matcher.find()) {
            if (!numberReplaced) {
                String prefix = matcher.group(1) != null ? matcher.group(1) : "";
                String suffix = matcher.group(3) != null ? matcher.group(3) : "";
                String dot = matcher.group(4) != null ? "." : "";
                String space = matcher.group().endsWith(" ") ? " " : "";

                String replacement = prefix + currentNumber + suffix + dot + space;

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                numberReplaced = true;
            } else {
                matcher.appendReplacement(sb, matcher.group());
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Transforms provided HTML into a format that can be fully parsed by OOTextIntoOO.write(...)
     * The transformed HTML can be used for inserting into a LibreOffice document
     * Context: The HTML produced by CitationStyleGenerator.generateCitation(...) is not directly (completely) parsable by OOTextIntoOO.write(...)
     * For more details, read the documentation of the write(...) method in the {@link OOTextIntoOO} class.
     * <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html">Additional Information</a>.
     *
     * @param html The HTML string to be transformed into OO-write ready HTML.
     * @return The formatted html string
     */
    private String transformHtml(String html) {
        // Initial clean up of escaped characters
        html = StringEscapeUtils.unescapeHtml4(html);

        // Handle margins (spaces between citation number and text)
        html = html.replaceAll("<div class=\"csl-left-margin\">(.*?)</div><div class=\"csl-right-inline\">(.*?)</div>", "$1 $2");

        // Remove unsupported tags
        html = html.replaceAll("<div[^>]*>", "");
        html = html.replace("</div>", "");

        // Remove unsupported links
        html = html.replaceAll("<a[^>]*>", "");
        html = html.replace("</a>", "");

        // Replace span tags with inline styles for bold
        html = html.replaceAll("<span style=\"font-weight: ?bold;?\">(.*?)</span>", "<b>$1</b>");

        // Replace span tags with inline styles for italic
        html = html.replaceAll("<span style=\"font-style: ?italic;?\">(.*?)</span>", "<i>$1</i>");

        // Replace span tags with inline styles for underline
        html = html.replaceAll("<span style=\"text-decoration: ?underline;?\">(.*?)</span>", "<u>$1</u>");

        html = html.replaceAll("<span style=\"font-variant: ?small-caps;?\">(.*?)</span>", "<smallcaps>$1</smallcaps>");

        // Clean up any remaining span tags
        html = html.replaceAll("</?span[^>]*>", "");

        return html;
    }

    public boolean isCitedEntry(BibEntry entry) {
        String citationKey = entry.getCitationKey().orElse("");
        return markManager.hasCitationForKey(citationKey);
    }
}
