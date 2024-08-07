package org.jabref.logic.openoffice.oocsltext;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import org.apache.commons.text.StringEscapeUtils;

public class CSLCitationOOAdapter {

    public static final String[] PREFIXES = {"JABREF_", "CSL_"};

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

    public void insertBibliography(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws Exception {

        String style = selectedStyle.getSource();
        isNumericStyle = selectedStyle.isNumericStyle();

        // Sort entries based on their order of appearance in the document
        entries.sort(Comparator.comparingInt(entry -> markManager.getCitationNumber(entry.getCitationKey().orElse(""))));
        for (BibEntry entry: entries) {
            String citation = CitationStyleGenerator.generateCitation(List.of(entry), style, format, bibDatabaseContext, bibEntryTypesManager).getFirst();
            System.out.println(citation);
            writeCitation(document, cursor, entry, citation);

            if (isNumericStyle) {
                // Select the paragraph break
                cursor.goLeft((short) 1, true);

                // Delete the selected content (paragraph break)
                cursor.setString("");
            }
        }
        // List<String> citations = CitationStyleGenerator.generateCitation(entries, style, format, bibDatabaseContext, bibEntryTypesManager);

//        for (int i = 0; i < citations.size(); i++) {
//            BibEntry entry = entries.get(i);
//            String citation = citations.get(i);
//            writeCitation(document, cursor, entry, citation);
//        }
    }

    public void insertInText(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws Exception {

        String style = selectedStyle.getSource();
        isNumericStyle = selectedStyle.isNumericStyle();

        // Generate a single in-text citation for a group of entries
        String inTextCitation = CitationStyleGenerator.generateInText(entries, style, format, bibDatabaseContext, bibEntryTypesManager).getText();

        String formattedCitation = transformHtml(inTextCitation);

        if (isNumericStyle) {
            formattedCitation = updateMultipleCitations(formattedCitation, entries);
        }

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));

        // Insert the citation text with multiple reference marks
        insertMultipleReferenceMarks(cursor, entries, ooText);

        // Move the cursor to the end of the inserted text
        cursor.collapseToEnd();
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
        // Check if there's already a space before the cursor
        boolean spaceExists = false;
        XTextCursor checkCursor = cursor.getText().createTextCursorByRange(cursor.getStart());

        // Check if we're at the start of the document - if yes we set the flag and don't insert a space
        if (!checkCursor.goLeft((short) 1, true)) {
            // We're at the start of the document
            spaceExists = true;
        } else {
            // If not at the start of document, check if there's a space before
            spaceExists = checkCursor.getString().equals(" ");
            // If not a space, check if it's a paragraph break
            if (!spaceExists) {
                spaceExists = checkCursor.getString().matches("\\R");
            }
        }

        if (entries.size() == 1) {
            CSLReferenceMark mark = markManager.createReferenceMark(entries.getFirst());
            mark.insertReferenceIntoOO(document, cursor, ooText, !spaceExists, true, true);
        } else {
            if (!spaceExists) {
                cursor.getText().insertString(cursor, " ", false);
            }
            OOTextIntoOO.write(document, cursor, ooText);
            for (BibEntry entry : entries) {
                CSLReferenceMark mark = markManager.createReferenceMark(entry);
                OOText emptyOOText = OOFormat.setLocaleNone(OOText.fromString(""));
                mark.insertReferenceIntoOO(document, cursor, emptyOOText, false, false, true);
            }
            cursor.getText().insertString(cursor, " ", false);
        }

        // Move the cursor to the end of the inserted text
        cursor.collapseToEnd();
    }

    private String updateMultipleCitations(String citation, List<BibEntry> entries) {
        Pattern pattern = Pattern.compile("(\\D*)(\\d+)(\\D*)");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();
        int entryIndex = 0;

        while (matcher.find() && entryIndex < entries.size()) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(3);

            int currentNumber = markManager.getCitationNumber(entries.get(entryIndex).getCitationKey().orElse(""));

            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + currentNumber + suffix));
            entryIndex++;
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

    private String updateSingleCitation(String citation, int currentNumber) {
        Pattern pattern = Pattern.compile("(\\[|\\()?(\\d+)(\\]|\\))?(\\.)?\\s*");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();
        boolean numberReplaced = false;

        while (matcher.find()) {
            if (!numberReplaced) {
                String prefix = matcher.group(1) != null ? matcher.group(1) : "";
                String suffix = matcher.group(3) != null ? matcher.group(3) : "";
                String dot = matcher.group(4) != null ? "." : "";

                String replacement;
                if (prefix.isEmpty() && suffix.isEmpty()) {
                    replacement = currentNumber + dot + " ";
                } else {
                    replacement = prefix + currentNumber + suffix + dot + " ";
                }

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                numberReplaced = true;
            } else {
                // If we've already replaced the number, keep any subsequent numbers as they are
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
