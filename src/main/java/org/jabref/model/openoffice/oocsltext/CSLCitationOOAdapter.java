package org.jabref.model.openoffice.oocsltext;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.util.TestEntry;
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

public class CSLCitationOOAdapter {

    private static final BibEntryTypesManager BIBENTRYTYPESMANAGER = new BibEntryTypesManager();
    private static int cslIndex;

    public static void setCslIndex(int cslIndex) {
        CSLCitationOOAdapter.cslIndex = cslIndex;
    }

    private static String transformHtml(String html) {
        // Remove unsupported tags
        html = html.replaceAll("<div[^>]*>", "");
        html = html.replaceAll("</div>", "");

        // Replace unsupported entities
        html = html.replace("&ndash;", "–");
        html = html.replace("&ldquo;", "\"");
        html = html.replace("&rdquo;", "\"");
        html = html.replace("&laquo;", "«");
        html = html.replace("&raquo;", "»");
        html = html.replace("&amp;", "AND");

        // Remove unsupported links
        html = html.replaceAll("<a[^>]*>", "");
        html = html.replaceAll("</a>", "");

        // Replace span tags with inline styles for bold
        html = html.replaceAll("<span style=\"font-weight: ?bold;?\">(.*?)</span>", "<b>$1</b>");

        // Replace span tags with inline styles for italic
        html = html.replaceAll("<span style=\"font-style: ?italic;?\">(.*?)</span>", "<i>$1</i>");

        html = html.replaceAll("<span style=\"font-variant: ?small-caps;?\">(.*?)</span>", "<smallcaps>$1</smallcaps>");

        // Clean up any remaining span tags
        html = html.replaceAll("</?span[^>]*>", "");

        return html;
    }

    public static void insertCitation(XTextDocument doc, XTextCursor cursor)
            throws IllegalArgumentException, WrappedTargetException, CreationException {

        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.discoverCitationStyles().get(cslIndex).getSource();
        System.out.println(CitationStyle.discoverCitationStyles().get(cslIndex).getTitle());
        CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format, new BibDatabaseContext(), BIBENTRYTYPESMANAGER);
        System.out.println(actualCitation);
        String formattedHTML = transformHtml(actualCitation);
        System.out.println(formattedHTML);

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedHTML));
        OOTextIntoOO.write(doc, cursor, ooText);
        cursor.collapseToEnd();
    }
}
