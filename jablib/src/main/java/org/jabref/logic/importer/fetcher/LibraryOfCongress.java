package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetcher for the Library of Congress Control Number (LCCN) using https://lccn.loc.gov/
public class LibraryOfCongress implements IdBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryOfCongress.class);
    // The Library of Congress asks clients to stay at or below 10 requests per minute.
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(10.0 / 60.0);

    private final ImportFormatPreferences importFormatPreferences;

    public LibraryOfCongress(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "Library of Congress";
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder("https://lccn.loc.gov/" + identifier + "/mods");
        return uriBuilder.build().toURL();
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        URL urlForIdentifier;
        try {
            urlForIdentifier = getUrlForIdentifier(identifier);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URI is malformed", e);
        }

        double waitingTime = RATE_LIMITER.acquire();
        LOGGER.trace("Thread {}, searching Library of Congress '{}', waited {} because of API rate limiter",
                Thread.currentThread().threadId(), urlForIdentifier, waitingTime);

        try (InputStream stream = getUrlDownload(urlForIdentifier).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            if (fetchedEntries.isEmpty()) {
                return Optional.empty();
            }
            if (fetchedEntries.size() > 1) {
                LOGGER.info("Fetcher {} found more than one result for identifier {}. We will use the first entry.", getName(), identifier);
            }
            BibEntry entry = fetchedEntries.getFirst();
            doPostCleanup(entry);
            return Optional.of(entry);
        } catch (IOException e) {
            if (e.getCause() instanceof FetcherException fe) {
                throw fe;
            }
            throw new FetcherException(urlForIdentifier, "A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException(urlForIdentifier, "An internal parser error occurred", e);
        }
    }

    @Override
    public Parser getParser() {
        return new ModsImporter(this.importFormatPreferences);
    }
}
