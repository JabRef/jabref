package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Fetcher for ISBN using https://www.ottobib.com
 */
public class IsbnViaOttoBibFetcher extends AbstractIsbnFetcher {

    private static final String BASE_URL = "https://www.ottobib.com/isbn/";

    public IsbnViaOttoBibFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "ISBN (OttoBib)";
    }

    /**
     * @return null, because the identifier is passed using form data. This method is not used.
     */
    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        return null;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        this.ensureThatIsbnIsValid(identifier);

        Document html;
        try {
            html = Jsoup.connect(BASE_URL + identifier + "/bibtex").userAgent(URLDownload.USER_AGENT).get();
        } catch (IOException e) {
            throw new FetcherException("Could not ", e);
        }
        Element textArea = html.select("textarea").first();

        // inspect the "no results" error message (if there is one)
        Optional<Element> potentialErrorMessageDiv = Optional.ofNullable((html.select("div#flash-notice.notice.add-bottom").first()));
        if (potentialErrorMessageDiv.isPresent() && potentialErrorMessageDiv.get().text().contains("No Results")) {
            LOGGER.error("ISBN {} not found at ottobib", identifier);
        }

        Optional<BibEntry> entry = Optional.empty();
        try {
            entry = BibtexParser.singleFromString(textArea.text(), importFormatPreferences, new DummyFileUpdateMonitor());
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
        entry.ifPresent(bibEntry -> doPostCleanup(bibEntry));
        return entry;
    }
}
