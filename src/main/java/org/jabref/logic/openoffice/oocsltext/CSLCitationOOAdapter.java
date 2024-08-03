package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public static final String[] PREFIXES = {"JABREF_", "CSL_"};
    public static final int REFMARK_ADD_CHARS = 8;

    private final CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;
    private final List<BibEntry> citedEntries = new ArrayList<>();
    private int lastCitationNumber = 0;
    private boolean isNumericStyle = false;
    private MarkManager markManager;

    public CSLCitationOOAdapter(XTextDocument doc) throws Exception {
        this.markManager = new MarkManager(doc);
    }

    public void insertBibliography(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws IllegalArgumentException, WrappedTargetException, CreationException {

        String style = selectedStyle.getSource();
        isNumericStyle = checkIfNumericStyle(style);

        List<String> citations = CitationStyleGenerator.generateCitation(entries, style, format, bibDatabaseContext, bibEntryTypesManager);

        for (int i = 0; i < citations.size(); i++) {
            BibEntry entry = entries.get(i);
            String citation = citations.get(i);
            insertCitation(doc, cursor, entry, citation);
        }
    }

    public void insertInText(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws IOException, WrappedTargetException, CreationException {

        String style = selectedStyle.getSource();
        isNumericStyle = checkIfNumericStyle(style);

        String inTextCitation = CitationStyleGenerator.generateInText(entries, style, format, bibDatabaseContext, bibEntryTypesManager).getText();

        for (BibEntry entry : entries) {
            insertCitation(doc, cursor, entry, inTextCitation);
        }
    }

    private void insertCitation(XTextDocument doc, XTextCursor cursor, BibEntry entry, String citation) throws WrappedTargetException, CreationException {
        try {
            ReferenceMark mark = markManager.createReferenceMark(entry, "ReferenceMark");

            if (!citedEntries.contains(entry)) {
                lastCitationNumber++;
                citedEntries.add(entry);
            }
            int currentNumber = citedEntries.indexOf(entry) + 1;

            if (isNumericStyle) {
                citation = updateCitationNumber(citation, currentNumber);
            }

            String formattedCitation = transformHtml(citation);
            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));

            mark.insertInText(doc, cursor, ooText);
            cursor.collapseToEnd();
        } catch (Exception e) {
            Logger.error("Error inserting citation", e);
        }
    }

    private String updateCitationNumber(String citation, int currentNumber) {
        return citation.replaceFirst("\\d+", String.valueOf(currentNumber));
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

    private String transformHtml(String html) {
        html = StringEscapeUtils.unescapeHtml4(html);
        html = html.replaceAll("<div class=\"csl-left-margin\">(.*?)</div><div class=\"csl-right-inline\">(.*?)</div>", "$1 $2");
        html = html.replaceAll("<div[^>]*>", "");
        html = html.replace("</div>", "");
        html = html.replaceAll("<a[^>]*>", "");
        html = html.replace("</a>", "");
        html = html.replaceAll("<span style=\"font-weight: ?bold;?\">(.*?)</span>", "<b>$1</b>");
        html = html.replaceAll("<span style=\"font-style: ?italic;?\">(.*?)</span>", "<i>$1</i>");
        html = html.replaceAll("<span style=\"font-variant: ?small-caps;?\">(.*?)</span>", "<smallcaps>$1</smallcaps>");
        html = html.replaceAll("</?span[^>]*>", "");
        return html;
    }

    public static String getRandomString(int len) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
