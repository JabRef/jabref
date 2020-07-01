package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.net.URLDownload;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

/**
 * Provides a convenient interface for search-based fetcher, which follow the usual three-step procedure:
 * 1. Open a URL based on the search query
 * 2. Parse the response to get a list of {@link BibEntry}
 * 3. Post-process fetched entries
 */
public interface SearchBasedParserFetcher extends SearchBasedFetcher {

    /**
     * Constructs a URL based on the query.
     * @param query the search query
     */
    URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException;

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser();

    /**
     * Performs a cleanup of the fetched entry.
     *
     * Only systematic errors of the fetcher should be corrected here
     * (i.e. if information is consistently contained in the wrong field or the wrong format)
     * but not cosmetic issues which may depend on the user's taste (for example, LateX code vs HTML in the abstract).
     *
     * Try to reuse existing {@link Formatter} for the cleanup. For example,
     * {@code new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);}
     *
     * By default, no cleanup is done.
     * @param entry the entry to be cleaned-up
     */
    default void doPostCleanup(BibEntry entry) {
        // Do nothing by default
    }

    /**
     * Gets the {@link URLDownload} object for downloading content. Overwrite, if you need to send additional headers for the download
     *
     * @param query The search query
     * @throws MalformedURLException
     * @throws FetcherException
     * @throws URISyntaxException
     */
    default URLDownload getUrlDownload(String query) throws MalformedURLException, FetcherException, URISyntaxException {
        return new URLDownload(getURLForQuery(query));
    }

    @Override
    default List<BibEntry> performSearch(String query) throws FetcherException {
        if (StringUtil.isBlank(query)) {
            return Collections.emptyList();
        }

        try (InputStream stream = getUrlDownload(query).asInputStream()) {
            List<BibEntry> fetchedEntries = new ArrayList<>();

            // check if there is anything to read since mEDRA '404 not found' returns nothing
            PushbackInputStream pushbackInputStream = new PushbackInputStream(stream);
            int b;
            b = pushbackInputStream.read();
            if (b != -1) {
                pushbackInputStream.unread(b);
                fetchedEntries = getParser().parseEntries(pushbackInputStream);
                // Post-cleanup
                fetchedEntries.forEach(this::doPostCleanup);
            }
            pushbackInputStream.close();

            return fetchedEntries;
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            // TODO: Catch HTTP Response 401/403 errors and report that user has no rights to access resource
            throw new FetcherException("A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
    }
}
