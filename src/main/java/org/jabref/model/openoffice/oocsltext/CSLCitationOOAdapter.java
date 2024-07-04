package org.jabref.model.openoffice.oocsltext;

import java.io.IOException;
import java.util.List;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
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
    private static int cslIndex;
    private static final List<CitationStyle> STYLE_LIST = CitationStyle.discoverCitationStyles();

    public static void setCslIndex(int cslIndex) {
        CSLCitationOOAdapter.cslIndex = cslIndex;
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

    public static void insertBibliography(XTextDocument doc, XTextCursor cursor)
            throws IllegalArgumentException, WrappedTargetException, CreationException {

        BibEntry entry = TestEntry.getTestEntry();
        String style = STYLE_LIST.get(cslIndex).getSource();
        Logger.warn("Selected Style: " + STYLE_LIST.get(cslIndex).getTitle());
        CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format, new BibDatabaseContext(), BIBENTRYTYPESMANAGER);
        Logger.warn("Unformatted Citation: " + actualCitation);
        String formattedHTML = transformHtml(actualCitation);
        Logger.warn("Formatted Citation: " + formattedHTML);

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedHTML));
        OOTextIntoOO.write(doc, cursor, ooText);
        cursor.collapseToEnd();
    }

    public static void insertInText(XTextDocument doc, XTextCursor cursor) throws IOException, WrappedTargetException, CreationException {

        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry(), TestEntry.getTestEntryBook())));
        context.setMode(BibDatabaseMode.BIBLATEX);

        Citation citation = CitationStyleGenerator.generateInText(List.of(TestEntry.getTestEntry(), TestEntry.getTestEntryBook()), STYLE_LIST.get(cslIndex).getSource(), CitationStyleOutputFormat.HTML, context, BIBENTRYTYPESMANAGER);
        Logger.warn("Unformatted in-text Citation: " + citation.getText());
        String formattedHTML = transformHtml(citation.getText());
        Logger.warn("Formatted in-text Citation: " + formattedHTML);
        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedHTML));
        OOTextIntoOO.write(doc, cursor, ooText);
        cursor.collapseToEnd();
    }
}
