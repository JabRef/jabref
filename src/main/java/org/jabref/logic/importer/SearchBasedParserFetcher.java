package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.model.entry.BibEntry;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

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
     * This method is used to send queries with advanced URL parameters.
     * This method is necessary as the performSearch method does not support certain URL parameters that are used for
     * fielded search, such as a title, author, or year parameter.
     *
     * @param luceneQuery the root node of the lucene query
     */
    @Override
    default List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        // ADR-0014
        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(luceneQuery);
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

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser();

    /**
     * Constructs a URL based on the lucene query.
     *
     * @param luceneQuery the root node of the lucene query
     */
    URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException;

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
