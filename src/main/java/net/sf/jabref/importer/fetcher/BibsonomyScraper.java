/*  Copyright (C) 2003-2011 JabRef contributors.
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

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.net.URLDownload;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Convenience class for getting BibTeX entries from the BibSonomy scraper,
 * from an URL pointing to an entry.
 */
class BibsonomyScraper {

    private static final String BIBSONOMY_SCRAPER = "http://scraper.bibsonomy.org/service?url=";
    private static final String BIBSONOMY_SCRAPER_POST = "&format=bibtex";


    /**
     * Return a BibEntry by looking up the given url from the BibSonomy scraper.
     * @param entryUrl
     * @return
     */
    public static BibEntry getEntry(String entryUrl) {
        try {
            // Replace special characters by corresponding sequences:
            entryUrl = entryUrl.replaceAll("%", "%25").replaceAll(":", "%3A").replaceAll("/", "%2F").replaceAll("\\?", "%3F").replaceAll("&", "%26").replaceAll("=", "%3D");

            URL url = new URL(BibsonomyScraper.BIBSONOMY_SCRAPER + entryUrl + BibsonomyScraper.BIBSONOMY_SCRAPER_POST);
            String bibtex = new URLDownload(url).downloadToString(StandardCharsets.UTF_8);
            BibtexParser bp = new BibtexParser(new StringReader(bibtex));
            ParserResult pr = bp.parse();
            if ((pr != null) && (pr.getDatabase().getEntryCount() > 0)) {
                return pr.getDatabase().getEntries().iterator().next();
            } else {
                return null;
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
