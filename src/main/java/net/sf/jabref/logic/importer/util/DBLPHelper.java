package net.sf.jabref.logic.importer.util;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;

public class DBLPHelper {

    private final DBLPQueryCleaner cleaner = new DBLPQueryCleaner();
    private static final String START_PATTERN = "<pre class=\"verbatim select-on-click\">";
    private static final String END_PATTERN = "</pre>";

    private final ImportFormatPreferences importFormatPreferences;

    /*
     * This is a small helper class that cleans the user submitted query. Right
     * now, we cannot search for ":" on dblp.org. So, we remove colons from the
     * user submitted search string. Also, the search is case sensitive if we
     * use capitals. So, we better change the text to lower case.
     */

    static class DBLPQueryCleaner {

        public String cleanQuery(final String query) {
            String cleaned = query;

            cleaned = cleaned.replace("-", " ").replace(" ", "%20").replace(":", "").toLowerCase();

            return cleaned;
        }
    }


    public DBLPHelper(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    /**
     *
     * @param query
     *            string with the user query
     * @return a string with the user query, but compatible with dblp.org
     */
    public String cleanDBLPQuery(String query) {
        return cleaner.cleanQuery(query);
    }

    /**
     * Takes an HTML file (as String) as input and extracts the bibtex
     * information. After that, it will convert it into a BibEntry and return
     * it (them).
     *
     * @param page
     *            page as String
     * @return list of BibEntry
     */
    public List<BibEntry> getBibTexFromPage(final String page) {
        final List<BibEntry> bibtexList = new ArrayList<>();

        String tmpStr = page;
        int startIdx = tmpStr.indexOf(START_PATTERN);
        int endIdx = tmpStr.indexOf(END_PATTERN);

        // this entry exists for sure
        String entry1 = tmpStr.substring(startIdx + START_PATTERN.length(),
                endIdx);
        entry1 = cleanEntry(entry1);
        BibtexParser.singleFromString(entry1, importFormatPreferences).ifPresent(bibtexList::add);

        // let's see whether there is another entry (crossref)
        tmpStr = tmpStr
                .substring(endIdx + END_PATTERN.length(), tmpStr.length());
        startIdx = tmpStr.indexOf(START_PATTERN);
        if (startIdx != -1) {
            endIdx = tmpStr.indexOf(END_PATTERN);
            // this entry exists for sure
            String entry2 = tmpStr.substring(startIdx + START_PATTERN.length(),
                    endIdx);
            entry2 = cleanEntry(entry2);
            BibtexParser.singleFromString(entry2, importFormatPreferences).ifPresent(bibtexList::add);
        }

        return bibtexList;
    }

    private String cleanEntry(final String bibEntry) {
        return bibEntry.replaceFirst("<a href=\".*\">DBLP</a>", "DBLP");
    }

}
