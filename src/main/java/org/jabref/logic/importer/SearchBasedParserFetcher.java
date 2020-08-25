package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.slf4j.LoggerFactory;

/**
 * Provides a convenient interface for search-based fetcher, which follow the usual three-step procedure:
 * <ol>
 *     <li>Open a URL based on the search query</li>
 *     <li>Parse the response to get a list of {@link BibEntry}</li>
 *     <li>Post-process fetched entries</li>
 * </ol>
 */
public interface SearchBasedParserFetcher extends SearchBasedFetcher {

    /**
     * Constructs a URL based on the query.
     *
     * @param query the search query
     */
    URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException;

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser();

    /**
     * Fetch the entries from the given URL. This method is used by {@link #performSearch(String)} and {@link #performComplexSearch(ComplexSearchQuery)}
     */
    default List<BibEntry> getBibEntries(URL urlForQuery) throws FetcherException {
        LoggerFactory.getLogger(this.getClass()).debug("Using query URL {}", urlForQuery.toString());
        try (InputStream stream = getUrlDownload(urlForQuery).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            fetchedEntries.forEach(this::doPostCleanup);
            return fetchedEntries;
        } catch (IOException e) {
            // TODO: Catch HTTP Response 401/403 errors and report that user has no rights to access resource
            throw new FetcherException(String.format("A network error occurred when fetching from %s", urlForQuery.toString()), e);
        } catch (ParseException e) {
            throw new FetcherException(String.format("An internal parser error occurred when fetching from %s", urlForQuery.toString()), e);
        }
    }

    @Override
    default List<BibEntry> performSearch(String query) throws FetcherException {
        if (StringUtil.isBlank(query)) {
            return Collections.emptyList();
        }

        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(query);
        } catch (URISyntaxException | MalformedURLException | FetcherException e) {
            throw new FetcherException(String.format("Search URI %s is malformed", query), e);
        }

        return getBibEntries(urlForQuery);
    }

    /**
     * This method is used to send queries with advanced URL parameters.
     * This method is necessary as the performSearch method does not support certain URL parameters that are used for
     * fielded search, such as a title, author, or year parameter.
     *
     * @param complexSearchQuery the search query defining all fielded search parameters
     */
    @Override
    default List<BibEntry> performComplexSearch(ComplexSearchQuery complexSearchQuery) throws FetcherException {
        URL complexQueryURL;
        try {
            complexQueryURL = getComplexQueryURL(complexSearchQuery);
        } catch (URISyntaxException | MalformedURLException | FetcherException e) {
            throw new FetcherException("Search URI is malformed", e);
        }
        return getBibEntries(complexQueryURL);
    }

    default URL getComplexQueryURL(ComplexSearchQuery complexSearchQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        // Default Implementation behaves like getURLForQuery using the default field as query
        return this.getURLForQuery(complexSearchQuery.getDefaultField().orElse(""));
    }

    /**
     * Performs a cleanup of the fetched entry.
     * <p>
     * Only systematic errors of the fetcher should be corrected here
     * (i.e. if information is consistently contained in the wrong field or the wrong format)
     * but not cosmetic issues which may depend on the user's taste (for example, LateX code vs HTML in the abstract).
     * <p>
     * Try to reuse existing {@link Formatter} for the cleanup. For example,
     * {@code new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);}
     * <p>
     * By default, no cleanup is done.
     *
     * @param entry the entry to be cleaned-up
     */
    default void doPostCleanup(BibEntry entry) {
        // Do nothing by default
    }
}
