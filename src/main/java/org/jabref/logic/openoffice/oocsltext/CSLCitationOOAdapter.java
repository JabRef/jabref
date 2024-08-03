package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    public static final int REFMARK_ADD_CHARS = 8;

    private final CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;
    private final XTextDocument document;
    private MarkManager markManager;
    private boolean isNumericStyle = false;

    public CSLCitationOOAdapter(XTextDocument doc) throws Exception {
        this.document = doc;
        this.markManager = new MarkManager(doc);
    }

    public void readExistingMarks() throws Exception {
        markManager.readExistingMarks();
    }

    public void insertBibliography(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws Exception {

        String style = selectedStyle.getSource();
        isNumericStyle = selectedStyle.isNumericStyle();

        List<String> citations = CitationStyleGenerator.generateCitation(entries, style, format, bibDatabaseContext, bibEntryTypesManager);

        for (int i = 0; i < citations.size(); i++) {
            BibEntry entry = entries.get(i);
            String citation = citations.get(i);
            writeCitation(doc, cursor, entry, citation);
        }
    }

    public void insertInText(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws Exception {

        String style = selectedStyle.getSource();
        isNumericStyle = selectedStyle.isNumericStyle();

        String inTextCitation = CitationStyleGenerator.generateInText(entries, style, format, bibDatabaseContext, bibEntryTypesManager).getText();

        writeInTextCitation(doc, cursor, entries, inTextCitation);
    }

    private void writeInTextCitation(XTextDocument doc, XTextCursor cursor, List<BibEntry> entries, String citation) throws Exception {
        String formattedCitation = transformHtml(citation);

        if (isNumericStyle) {
            formattedCitation = updateMultipleCitations(formattedCitation, entries);
        }

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));

        // Create a single reference mark for the entire in-text citation
        ReferenceMark mark = markManager.createReferenceMark(entries.get(0), "InTextReferenceMark");

        // Insert the citation text wrapped in a reference mark
        mark.insertReferenceIntoOO(doc, cursor, ooText);

        // Move the cursor to the end of the inserted text
        cursor.collapseToEnd();
    }

    private String updateMultipleCitations(String citation, List<BibEntry> entries) {
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(citation);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String[] numbers = matcher.group(1).split(",");
            List<String> updatedNumbers = entries.stream()
                                                 .map(entry -> String.valueOf(markManager.getCitationNumber(entry.getCitationKey().orElse(""))))
                                                 .collect(Collectors.toList());

            String replacement = "[" + String.join(",", updatedNumbers) + "]";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void writeCitation(XTextDocument doc, XTextCursor cursor, BibEntry entry, String citation) throws Exception {
        String citationKey = entry.getCitationKey().orElse("");
        int currentNumber = markManager.getCitationNumber(citationKey);

        ReferenceMark mark = markManager.createReferenceMark(entry, "ReferenceMark");
        String formattedCitation;
        if (isNumericStyle) {
            formattedCitation = updateSingleCitation(transformHtml(citation), currentNumber);
        } else {
            formattedCitation = transformHtml(citation);
        }
        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));

        // Insert the citation text wrapped in a reference mark
        mark.insertReferenceIntoOO(doc, cursor, ooText);

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
     * Additional information: <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html">...</a>.
     *
     * @param html The HTML string to be transformed into OO-write ready HTML.
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

        html = html.replaceAll("<span style=\"font-variant: ?small-caps;?\">(.*?)</span>", "<smallcaps>$1</smallcaps>");

        // Clean up any remaining span tags
        html = html.replaceAll("</?span[^>]*>", "");

        return html;
    }
}
