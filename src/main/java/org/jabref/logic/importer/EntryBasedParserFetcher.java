package org.jabref.logic.importer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

/**
 * Provides a convenient interface for entry-based fetcher, which follow the usual three-step procedure:
 * 1. Open a URL based on the entry
 * 2. Parse the response to get a list of {@link BibEntry}
 * 3. Post-process fetched entries
 */
public interface EntryBasedParserFetcher extends EntryBasedFetcher {

    /**
     * Constructs a URL based on the {@link BibEntry}.
     *
     * @param entry the entry to look information for
     */
    URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException;

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser();

    @Override
    default List<BibEntry> performSearch(BibEntry entry, BibDatabaseMode targetFormat) throws FetcherException {
        Objects.requireNonNull(entry);

        try (InputStream stream = new BufferedInputStream(getURLForEntry(entry).openStream())) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

            // Post-cleanup
            fetchedEntries.forEach(fetchedEntry -> this.doPostCleanup(fetchedEntry, targetFormat));

            return fetchedEntries;
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            // TODO: Catch HTTP Response 401 errors and report that user has no rights to access resource
            throw new FetcherException("A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
    }
}
