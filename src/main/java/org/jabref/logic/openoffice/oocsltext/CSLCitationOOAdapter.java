package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import org.apache.commons.text.StringEscapeUtils;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class CSLCitationOOAdapter {

    private final CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;
    private final List<BibEntry> citedEntries = new ArrayList<>();
    private int lastCitationNumber = 0;
    private boolean isNumericStyle = false;

    public void insertBibliography(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws IllegalArgumentException, WrappedTargetException, CreationException {

        String style = selectedStyle.getSource();
        isNumericStyle = checkIfNumericStyle(style);

        List<String> citations = CitationStyleGenerator.generateCitation(entries, style, format, bibDatabaseContext, bibEntryTypesManager);
        if (isNumericStyle) {
            citations = updateCitationNumbers(citations, entries);
        }

        for (String citation : citations) {
            writeCitation(doc, cursor, citation);
        }
    }

    public void insertInText(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws IOException, WrappedTargetException, CreationException {

        String style = selectedStyle.getSource();

        String inTextCitation = CitationStyleGenerator.generateInText(entries, style, format, bibDatabaseContext, bibEntryTypesManager).getText();
        if (isNumericStyle) {
            inTextCitation = updateCitationNumbers(List.of(inTextCitation), entries).getFirst();
        }

        writeCitation(doc, cursor, inTextCitation);
    }

    private List<String> updateCitationNumbers(List<String> citations, List<BibEntry> entries) {
        List<String> updatedCitations = new ArrayList<>();
        for (int i = 0; i < citations.size(); i++) {
            updatedCitations.add(updateSingleCitation(citations.get(i), entries));
        }
        return updatedCitations;
    }

    private String updateSingleCitation(String citation, List<BibEntry> entries) {
        Pattern pattern = Pattern.compile("(\\[?)(\\d+)(\\]?)(\\.)?(\\s*)");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            try {
                int index = Integer.parseInt(matcher.group(2)) - 1;
                if (index >= 0 && index < entries.size()) {
                    BibEntry currentEntry = entries.get(index);
                    if (!citedEntries.contains(currentEntry)) {
                        lastCitationNumber++;
                        citedEntries.add(currentEntry);
                    }
                    int currentNumber = citedEntries.indexOf(currentEntry) + 1;

                    String prefix = matcher.group(1);
                    String suffix = matcher.group(3);
                    String dot = matcher.group(4) != null ? "." : "";
                    String space = matcher.group(5);

                    String replacement = prefix + currentNumber + suffix + dot + space;
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                } else {
                    // If the index is out of bounds, append the original match
                    matcher.appendReplacement(sb, matcher.group());
                }
            } catch (NumberFormatException e) {
                // If parsing fails, append the original match
                matcher.appendReplacement(sb, matcher.group());
                Logger.warn("Failed to parse citation number: " + matcher.group(2));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private boolean checkIfNumericStyle(String styleXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(styleXml)));

            Element styleElement = doc.getDocumentElement();
            Element infoElement = (Element) styleElement.getElementsByTagName("info").item(0);
            Element categoryElement = (Element) infoElement.getElementsByTagName("category").item(0);

            String citationFormat = categoryElement.getAttribute("citation-format");
            return "numeric".equals(citationFormat);
        } catch (Exception e) {
            Logger.error("Error parsing CSL style XML", e);
            return false;
        }
    }

    private void writeCitation(XTextDocument doc, XTextCursor cursor, String citation) throws WrappedTargetException, CreationException {
        String formattedCitation = transformHtml(citation);
        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));
        OOTextIntoOO.write(doc, cursor, ooText);
        cursor.collapseToEnd();
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
