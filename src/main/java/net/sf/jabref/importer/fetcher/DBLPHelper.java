/*  Copyright (C) 2011 Sascha Hunold.
 *  Copyright (C) 2015 JabRef Contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer.fetcher;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;

class DBLPHelper {

    private final DBLPQueryCleaner cleaner = new DBLPQueryCleaner();
    private static final String START_PATTERN = "<pre class=\"verbatim select-on-click\">";
    private static final String END_PATTERN = "</pre>";


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
        bibtexList.add(BibtexParser.singleFromString(entry1));

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
            bibtexList.add(BibtexParser.singleFromString(entry2));
        }

        return bibtexList;
    }

    private String cleanEntry(final String bibEntry) {
        return bibEntry.replaceFirst("<a href=\".*\">DBLP</a>", "DBLP");
    }

}
