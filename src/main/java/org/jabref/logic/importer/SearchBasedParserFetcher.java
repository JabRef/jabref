package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.database.BibDatabaseMode;
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
     */
    URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException;

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser();

    @Override
    default List<BibEntry> performSearch(String query, BibDatabaseMode targetBibEntryFormat) throws FetcherException {
        if (StringUtil.isBlank(query)) {
            return Collections.emptyList();
        }

        try (InputStream stream = getUrlDownload(getURLForQuery(query)).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

            // Post-cleanup
            fetchedEntries.forEach(bibEntry -> doPostCleanup(bibEntry, targetBibEntryFormat));

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

    /**
     * This method is used to send queries with advanced URL parameters.
     * This method is necessary as the performSearch method does not support certain URL parameters that are used for
     * fielded search, such as a title, author, or year parameter.
     *
     * @param complexSearchQuery the search query defining all fielded search parameters
     */
    @Override
    default List<BibEntry> performComplexSearch(ComplexSearchQuery complexSearchQuery, BibDatabaseMode targetBibEntryFormat) throws FetcherException {
        try (InputStream stream = getUrlDownload(getComplexQueryURL(complexSearchQuery)).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            fetchedEntries.forEach(bibEntry -> doPostCleanup(bibEntry, targetBibEntryFormat));
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

    default URL getComplexQueryURL(ComplexSearchQuery complexSearchQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        // Default Implementation behaves like getURLForQuery using the default field as query
        return this.getURLForQuery(complexSearchQuery.getDefaultField().orElse(""));
    }
}
