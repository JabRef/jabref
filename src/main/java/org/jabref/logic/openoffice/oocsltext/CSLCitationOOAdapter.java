package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.Random;
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

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import org.apache.commons.text.StringEscapeUtils;

public class CSLCitationOOAdapter {

    public static final String[] PREFIXES = {"JABREF_", "CSL_"};
    public static final int REFMARK_ADD_CHARS = 8;

    private final CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;
    private final XTextDocument document;
    private MarkManager markManager;

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

        List<String> citations = CitationStyleGenerator.generateCitation(entries, style, format, bibDatabaseContext, bibEntryTypesManager);

        for (int i = 0; i < citations.size(); i++) {
            BibEntry entry = entries.get(i);
            String citation = citations.get(i);
            insertCitation(doc, cursor, entry, citation);
        }
    }

    public void insertInText(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws Exception {

        String style = selectedStyle.getSource();

        String inTextCitation = CitationStyleGenerator.generateInText(entries, style, format, bibDatabaseContext, bibEntryTypesManager).getText();

        for (BibEntry entry : entries) {
            insertCitation(doc, cursor, entry, inTextCitation);
        }
    }

    private void insertCitation(XTextDocument doc, XTextCursor cursor, BibEntry entry, String citation) throws Exception {
        String citationKey = entry.getCitationKey().orElse("");
        int currentNumber = markManager.getCitationNumber(citationKey);

        ReferenceMark mark = markManager.createReferenceMark(entry, "ReferenceMark");

        String formattedCitation = updateSingleCitation(transformHtml(citation), currentNumber);
        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));

        mark.insertInText(doc, cursor, ooText);
        cursor.collapseToEnd();
    }

    private String updateSingleCitation(String citation, int currentNumber) {
        Pattern pattern = Pattern.compile("(\\[?)(\\d+)(\\]?)(\\.)?(\\s*)");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();
        boolean numberReplaced = false;

        while (matcher.find()) {
            if (!numberReplaced) {
                String prefix = matcher.group(1);
                String suffix = matcher.group(3);
                String dot = matcher.group(4) != null ? "." : "";
                String space = matcher.group(5);
                String replacement = prefix + currentNumber + suffix + dot + space;
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
