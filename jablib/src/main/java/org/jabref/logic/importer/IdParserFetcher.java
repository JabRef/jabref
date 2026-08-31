package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.Identifier;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Provides a convenient interface for [IdFetcher], which follow the usual three-step procedure:
/// 1. Open a URL based on the search query
/// 2. Parse the response to get a list of [BibEntry]
/// 3. Extract identifier
public interface IdParserFetcher<T extends Identifier> extends IdFetcher<T>, ParserFetcher {

    Logger LOGGER = LoggerFactory.getLogger(IdParserFetcher.class);

    /// Constructs a URL based on the [BibEntry].
    ///
    /// @param entry the entry to look information for
    URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException;

    /// Returns the parser used to convert the response to a list of [BibEntry].
    Parser getParser();

    /// Extracts the identifier from the list of fetched entries.
    ///
    /// @param inputEntry     the entry for which we are searching the identifier (can be used to find the closest match in the result)
    /// @param fetchedEntries list of entries returned by the web service
    Optional<T> extractIdentifier(BibEntry inputEntry, List<BibEntry> fetchedEntries) throws FetcherException;

    @Override
    default Optional<T> findIdentifier(@NonNull BibEntry entry) throws FetcherException {
        URL urlForEntry;
        try {
            urlForEntry = getURLForEntry(entry);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URL is malformed", e);
        }

        try {
            return FetcherRetry.executeWithRateLimitRetry(() -> fetchIdentifier(entry, urlForEntry));
        } catch (FetcherClientException exception) {
            if (exception.getHttpResponse()
                         .map(response -> response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND)
                         .orElse(false)) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private Optional<T> fetchIdentifier(BibEntry entry, URL urlForEntry) throws FetcherException {
        try (InputStream stream = getUrlDownload(urlForEntry).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

            if (fetchedEntries.isEmpty()) {
                return Optional.empty();
            }

            // Post-cleanup
            fetchedEntries.forEach(this::doPostCleanup);

            return extractIdentifier(entry, fetchedEntries);
        } catch (IOException e) {
            throw new FetcherException(urlForEntry, "An I/O exception occurred", e);
        } catch (ParseException e) {
            throw new FetcherException(urlForEntry, "An internal parser error occurred", e);
        }
    }
}
