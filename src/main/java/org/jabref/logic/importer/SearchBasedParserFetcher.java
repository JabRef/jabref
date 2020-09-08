package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

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

    @Override
    default List<BibEntry> performSearch(String query) throws FetcherException {
        if (StringUtil.isBlank(query)) {
            return Collections.emptyList();
        }

        // ADR-0014
        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(query);
        } catch (URISyntaxException | MalformedURLException | FetcherException e) {
            throw new FetcherException(String.format("Search URI crafted from query %s is malformed", query), e);
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
        // ADR-0014
        URL urlForQuery;
        try {
            urlForQuery = getComplexQueryURL(complexSearchQuery);
        } catch (URISyntaxException | MalformedURLException | FetcherException e) {
            throw new FetcherException("Search URI crafted from complex search query is malformed", e);
        }
        return getBibEntries(urlForQuery);
    }

    private List<BibEntry> getBibEntries(URL urlForQuery) throws FetcherException {
        try (InputStream stream = getUrlDownload(urlForQuery).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            fetchedEntries.forEach(this::doPostCleanup);
            return fetchedEntries;
        } catch (IOException e) {
            throw new FetcherException("A network error occurred while fetching from " + urlForQuery, e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred while fetching from " + urlForQuery, e);
        }
    }

    default URL getComplexQueryURL(ComplexSearchQuery complexSearchQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        // Default implementation behaves as getURLForQuery using the default field phrases as query
        List<String> defaultPhrases = complexSearchQuery.getDefaultFieldPhrases();
        return this.getURLForQuery(String.join(" ", defaultPhrases));
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
