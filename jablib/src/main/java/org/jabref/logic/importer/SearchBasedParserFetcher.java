package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.BaseQueryNode;

/// Provides a convenient interface for search-based fetcher, which follows the usual three-step procedure:
///
/// - Open a URL based on the search query
/// - Parse the response to get a list of [BibEntry]
/// - Post-process fetched entries
///
///
/// This interface is used for web resources which do NOT provide BibTeX data [BibEntry].
/// JabRef's infrastructure to convert arbitrary input data to BibTeX is [Parser].
///
///
/// This interface inherits [SearchBasedFetcher], because the methods `performSearch` have to be provided by both.
/// As non-BibTeX web fetcher one could do "magic" stuff without this helper interface and directly use [WebFetcher], but this is more work.
///
///
/// Note that this interface "should" be an abstract class.
/// However, Java does not support multi inheritance with classes (but with interfaces).
/// We need multi inheritance, because a fetcher might implement multiple query types (such as id fetching [IdBasedFetcher]), complete entry [EntryBasedFetcher], and search-based fetcher (this class).

public interface SearchBasedParserFetcher extends SearchBasedFetcher, ParserFetcher {

    /// This method is used to send queries with advanced URL parameters.
    /// This method is necessary as the performSearch method does not support certain URL parameters that are used for
    /// fielded search, such as a title, author, or year parameter.
    ///
    /// @param queryNode the first search node
    @Override
    default List<BibEntry> performSearch(BaseQueryNode queryNode) throws FetcherException {
        // ADR-0014
        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(queryNode);
        } catch (URISyntaxException | MalformedURLException | FetcherException e) {
            throw new FetcherException("Search URI crafted from complex search query is malformed", e);
        }
        return getBibEntries(urlForQuery);
    }

    /// Default implementation: builds the URL via [#getURLForRawQuery(String)], then downloads, parses, and post-cleans the result.
    @Override
    default List<BibEntry> performRawSearchQuery(String rawQuery) throws FetcherException {
        if (rawQuery.isBlank()) {
            return List.of();
        }
        URL urlForQuery;
        try {
            urlForQuery = getURLForRawQuery(rawQuery);
        } catch (URISyntaxException | MalformedURLException | FetcherException e) {
            throw new FetcherException("Search URI crafted from raw search query is malformed: " + rawQuery, e);
        }
        return getBibEntries(urlForQuery);
    }

    private List<BibEntry> getBibEntries(URL urlForQuery) throws FetcherException {
        try (InputStream stream = getUrlDownload(urlForQuery).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            fetchedEntries.forEach(this::doPostCleanup);
            return fetchedEntries;
        } catch (IOException e) {
            // Regular expression to redact API keys from the error message
            throw new FetcherException(urlForQuery, e);
        } catch (ParseException e) {
            // Regular expression to redact API keys from the error message
            throw new FetcherException(urlForQuery, "An internal parser error occurred while fetching", e);
        }
    }

    /// Returns the parser used to convert the response to a list of [BibEntry].
    Parser getParser();

    /// Constructs a URL based on the lucene query.
    ///
    /// @param queryList the list that contains the parsed nodes
    URL getURLForQuery(BaseQueryNode queryList) throws URISyntaxException, MalformedURLException, FetcherException;

    /// Constructs a URL that sends the raw, catalog-native query verbatim to the catalog (bypassing the query transformer).
    ///
    /// @param rawQuery catalog-native query string sent verbatim to the catalog
    default URL getURLForRawQuery(String rawQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        throw new UnsupportedOperationException(getName() + " has not yet been migrated to performRawSearchQuery");
    }
}
