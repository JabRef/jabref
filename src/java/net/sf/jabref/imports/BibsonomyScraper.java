package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;
import net.sf.jabref.net.URLDownload;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

/**
 * Convenience class for getting BibTeX entries from the BibSonomy scraper,
 * from an URL pointing to an entry.
 */
public class BibsonomyScraper {

    protected static final String BIBSONOMY_SCRAPER = "http://scraper.bibsonomy.org/service?url=";
    protected static final String BIBSONOMY_SCRAPER_POST = "&format=bibtex";

    /**
     * Return a BibtexEntry by looking up the given url from the BibSonomy scraper.
     * @param entryUrl
     * @return
     */
    public static BibtexEntry getEntry(String entryUrl) {
        try {
            // Replace special characters by corresponding sequences:
            entryUrl = entryUrl.replaceAll("%", "%25").replaceAll(":", "%3A").replaceAll("/", "%2F")
                    .replaceAll("\\?", "%3F").replaceAll("&", "%26").replaceAll("=", "%3D");
                    
            URL url = new URL(BIBSONOMY_SCRAPER+entryUrl+BIBSONOMY_SCRAPER_POST);
            URLDownload ud = new URLDownload(url);
            ud.download();
            String bibtex = ud.getStringContent();
            BibtexParser bp = new BibtexParser(new StringReader(bibtex));
            ParserResult pr = bp.parse();
            if ((pr != null) && (pr.getDatabase().getEntryCount() > 0)) {
                return pr.getDatabase().getEntries().iterator().next();
            }
            else return null;

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
