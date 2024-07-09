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
import de.undercouch.citeproc.output.Citation;
import org.apache.commons.text.StringEscapeUtils;
import org.tinylog.Logger;

public class CSLCitationOOAdapter {

    private static final BibEntryTypesManager BIBENTRYTYPESMANAGER = new BibEntryTypesManager();
    private static final List<CitationStyle> STYLE_LIST = CitationStyle.discoverCitationStyles();

    private static String selectedStyleName;

    public static void setSelectedStyleName(String styleName) {
        CSLCitationOOAdapter.selectedStyleName = styleName;
    }

    // Replace or modify the existing methods that use cslIndex
    private static CitationStyle getSelectedStyle() {
        return STYLE_LIST.stream()
                         .filter(style -> style.getTitle().equals(selectedStyleName))
                         .findFirst()
                         .orElse(STYLE_LIST.getFirst()); // Default to first style if not found
    }

    public static String getSelectedStyleName() {
        return selectedStyleName;
    }

    private static String transformHtml(String html) {
        // Remove unsupported tags
        html = StringEscapeUtils.unescapeHtml4(html);
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

    public static void insertBibliography(XTextDocument doc, XTextCursor cursor, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext)
            throws IllegalArgumentException, WrappedTargetException, CreationException {

        CitationStyle selectedStyle = getSelectedStyle();
        String style = selectedStyle.getSource();
        Logger.warn("Selected Style: " + selectedStyle.getTitle());

        CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;

        List<String> actualCitations = CitationStyleGenerator.generateCitation(entries, style, format, bibDatabaseContext, BIBENTRYTYPESMANAGER);
        for (String actualCitation: actualCitations) {
            Logger.warn("Unformatted Citation: " + actualCitation);
            String formattedHTML = transformHtml(actualCitation);
            Logger.warn("Formatted Citation: " + formattedHTML);

            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedHTML));
            OOTextIntoOO.write(doc, cursor, ooText);
            cursor.collapseToEnd();
        }
    }

    public static void insertInText(XTextDocument doc, XTextCursor cursor, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext) throws IOException, WrappedTargetException, CreationException {
        CitationStyle selectedStyle = getSelectedStyle();

        Citation citation = CitationStyleGenerator.generateInText(entries, selectedStyle.getSource(), CitationStyleOutputFormat.HTML, bibDatabaseContext, BIBENTRYTYPESMANAGER);
        Logger.warn("Unformatted in-text Citation: " + citation.getText());
        String formattedHTML = transformHtml(citation.getText());
        Logger.warn("Formatted in-text Citation: " + formattedHTML);
        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedHTML));
        OOTextIntoOO.write(doc, cursor, ooText);
        cursor.collapseToEnd();
    }
}
