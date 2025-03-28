package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class for getting BibTeX entries from the BibSonomy scraper, from an URL pointing to an entry.
 */
public class BibsonomyScraper {

    private static final String BIBSONOMY_SCRAPER = "https://scraper.bibsonomy.org/service?url=";
    private static final String BIBSONOMY_SCRAPER_POST = "&format=bibtex";

    private static final Logger LOGGER = LoggerFactory.getLogger(BibsonomyScraper.class);

    private BibsonomyScraper() {
    }

    /**
     * Return a BibEntry by looking up the given url from the BibSonomy scraper.
     */
    public static Optional<BibEntry> getEntry(String entryUrl, ImportFormatPreferences importFormatPreferences) {
        try {
            // Replace special characters by corresponding sequences:
            String cleanURL = entryUrl.replace("%", "%25").replace(":", "%3A").replace("/", "%2F").replace("?", "%3F")
                                      .replace("&", "%26").replace("=", "%3D");

            URL url = URLUtil.create(BibsonomyScraper.BIBSONOMY_SCRAPER + cleanURL + BibsonomyScraper.BIBSONOMY_SCRAPER_POST);
            String bibtex = new URLDownload(url).asString();
            return BibtexParser.singleFromString(bibtex, importFormatPreferences);
        } catch (IOException | FetcherException ex) {
            LOGGER.warn("Could not download entry", ex);
            return Optional.empty();
        } catch (ParseException ex) {
            LOGGER.warn("Could not parse entry", ex);
            return Optional.empty();
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not get entry", ex);
            return Optional.empty();
        }
    }
}
