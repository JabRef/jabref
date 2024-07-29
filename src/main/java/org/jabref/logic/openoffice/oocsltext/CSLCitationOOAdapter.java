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
    private static final CitationStyleOutputFormat FORMAT = CitationStyleOutputFormat.HTML;

    public static void insertBibliography(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext)
            throws IllegalArgumentException, WrappedTargetException, CreationException {

        String style = selectedStyle.getSource();

        List<String> citations = CitationStyleGenerator.generateCitation(entries, style, FORMAT, bibDatabaseContext, BIB_ENTRY_TYPES_MANAGER);

        for (String citation: citations) {
            writeCitation(doc, cursor, citation);
        }
    }

    public static void insertInText(XTextDocument doc, XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext)
            throws IOException, WrappedTargetException, CreationException {

        String style = selectedStyle.getSource();

        String inTextCitation = CitationStyleGenerator.generateInText(entries, style, FORMAT, bibDatabaseContext, BIB_ENTRY_TYPES_MANAGER).getText();

        writeCitation(doc, cursor, inTextCitation);
    }

    public static void writeCitation(XTextDocument doc, XTextCursor cursor, String citation) throws WrappedTargetException, CreationException {

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
     * Additional information: https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html.
     *
     * @param html The HTML string to be transformed into OO-write ready HTML.
     */
    private static String transformHtml(String html) {
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
