package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience class for getting BibTeX entries from the BibSonomy scraper,
 * from an URL pointing to an entry.
 */
public class BibsonomyScraper {

    private static final String BIBSONOMY_SCRAPER = "http://scraper.bibsonomy.org/service?url=";
    private static final String BIBSONOMY_SCRAPER_POST = "&format=bibtex";

    private static final Log LOGGER = LogFactory.getLog(BibsonomyScraper.class);

    /**
     * Return a BibEntry by looking up the given url from the BibSonomy scraper.
     * @param entryUrl
     * @return
     */
    public static Optional<BibEntry> getEntry(String entryUrl, ImportFormatPreferences importFormatPreferences) {
        try {
            // Replace special characters by corresponding sequences:
            String cleanURL = entryUrl.replace("%", "%25").replace(":", "%3A").replace("/", "%2F").replace("?", "%3F")
                    .replace("&", "%26").replace("=", "%3D");

            URL url = new URL(BibsonomyScraper.BIBSONOMY_SCRAPER + cleanURL + BibsonomyScraper.BIBSONOMY_SCRAPER_POST);
            String bibtex = new URLDownload(url).downloadToString(StandardCharsets.UTF_8);
            return BibtexParser.singleFromString(bibtex, importFormatPreferences);
        } catch (IOException ex) {
            LOGGER.warn("Could not download entry", ex);
            return Optional.empty();
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not get entry", ex);
            return Optional.empty();
        }
    }
}
