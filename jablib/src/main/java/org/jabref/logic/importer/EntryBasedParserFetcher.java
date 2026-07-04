package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;

/// Provides a convenient interface for entry-based fetcher, which follow the usual three-step procedure:
/// 1. Open a URL based on the entry
/// 2. Parse the response to get a list of {@link BibEntry}
/// 3. Post-process fetched entries
public interface EntryBasedParserFetcher extends EntryBasedFetcher, ParserFetcher {

    /// Constructs a URL based on the {@link BibEntry}.
    ///
    /// @param entry the entry to look information for
    URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException;

    /// Returns the parser used to convert the response to a list of {@link BibEntry}.
    Parser getParser();

    @Override
    default List<BibEntry> performSearch(@NonNull BibEntry entry) throws FetcherException {
        URL urlForEntry;
        try {
            if ((urlForEntry = getURLForEntry(entry)) == null) {
                return List.of();
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        }

        try (InputStream stream = getUrlDownload(urlForEntry).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

            // Post-cleanup
            fetchedEntries.forEach(this::doPostCleanup);

            return fetchedEntries;
        } catch (IOException e) {
            if (e.getCause() instanceof FetcherException fetcherException) {
                // URLDownload reports HTTP errors such as 401 as FetcherException; preserve that user-facing detail.
                throw fetcherException;
            }
            LoggerFactory.getLogger(EntryBasedParserFetcher.class).error("Could not fetch from URL {}", urlForEntry, e);
            throw new FetcherException(urlForEntry, "A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException(urlForEntry, "An internal parser error occurred", e);
        }
    }
}
