package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.util.strings.StringUtil;

import org.apache.hc.core5.net.URIBuilder;

/// Fetcher for the Library of Congress Control Number (LCCN) using https://lccn.loc.gov/
public class LibraryOfCongress implements IdBasedParserFetcher {

    // The Library of Congress asks clients to stay at or below 10 requests per minute.
    private static final FetcherRateLimiter RATE_LIMITER = FetcherRateLimiter.ofRequestsPerInterval("Library of Congress", 10, Duration.ofMinutes(1));

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
    public Optional<org.jabref.model.entry.BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        RATE_LIMITER.acquire(identifier);

        return IdBasedParserFetcher.super.performSearchById(identifier);
    }

    @Override
    public Parser getParser() {
        return new ModsImporter(this.importFormatPreferences);
    }
}
