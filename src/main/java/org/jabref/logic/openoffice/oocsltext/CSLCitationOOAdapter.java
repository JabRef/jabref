package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.List;

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

public class CSLCitationOOAdapter {

    private static final BibEntryTypesManager BIB_ENTRY_TYPES_MANAGER = new BibEntryTypesManager();
    private static final List<CitationStyle> STYLE_LIST = CitationStyle.discoverCitationStyles();
    private static final CitationStyleOutputFormat FORMAT = CitationStyleOutputFormat.HTML;
    private static String selectedStyleName;

    public static CitationStyle getSelectedStyle() {
        return STYLE_LIST.stream()
                         .filter(style -> style.getTitle().equals(selectedStyleName))
                         .findFirst()
                         .orElse(STYLE_LIST.getFirst());
    }

    public static void insertBibliography(XTextDocument doc, XTextCursor cursor, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext)
            throws IllegalArgumentException, WrappedTargetException, CreationException {

        String selectedStyle = getSelectedStyle().getSource();

        List<String> citations = CitationStyleGenerator.generateCitation(entries, selectedStyle, FORMAT, bibDatabaseContext, BIB_ENTRY_TYPES_MANAGER);

        for (String citation: citations) {
            writeCitation(doc, cursor, citation);
        }
    }

    public static void insertInText(XTextDocument doc, XTextCursor cursor, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext)
            throws IOException, WrappedTargetException, CreationException {

        String selectedStyle = getSelectedStyle().getSource();

        String inTextCitation = CitationStyleGenerator.generateInText(entries, selectedStyle, FORMAT, bibDatabaseContext, BIB_ENTRY_TYPES_MANAGER).getText();

        writeCitation(doc, cursor, inTextCitation);
    }

    public static void writeCitation(XTextDocument doc, XTextCursor cursor, String citation) throws WrappedTargetException, CreationException {

        String formattedCitation = transformHtml(citation);
        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));
        OOTextIntoOO.write(doc, cursor, ooText);
        cursor.collapseToEnd();
    }

    private static String transformHtml(String html) {
        // Initial clean up of escaped characters
        html = StringEscapeUtils.unescapeHtml4(html);

        // Handle margins (spaces between citation number and text)
        html = html.replaceAll("<div class=\"csl-left-margin\">(.*?)</div><div class=\"csl-right-inline\">(.*?)</div>", "$1 $2");

        // Remove unsupported tags
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("<div[^>]*>", "");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("</div>", "");

        // Replace unsupported entities
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("&ndash;", "–");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("&ldquo;", "\"");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("&rdquo;", "\"");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("&laquo;", "«");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("&raquo;", "»");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("&amp;", "&");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("&lt;", "<");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("&rt;", ">");

        // Remove unsupported links
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("<a[^>]*>", "");
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("</a>", "");

        // Replace span tags with inline styles for bold
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("<span style=\"font-weight: ?bold;?\">(.*?)</span>", "<b>$1</b>");

        // Replace span tags with inline styles for italic
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("<span style=\"font-style: ?italic;?\">(.*?)</span>", "<i>$1</i>");

        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("<span style=\"font-variant: ?small-caps;?\">(.*?)</span>", "<smallcaps>$1</smallcaps>");

        // Clean up any remaining span tags
        html = StringEscapeUtils.unescapeHtml4(html).replaceAll("</?span[^>]*>", "");

        return html;
    }

    public static void setSelectedStyleName(String styleName) {
        CSLCitationOOAdapter.selectedStyleName = styleName;
    }
}
