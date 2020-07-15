package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.entry.BibEntry;

/**
 * This interface allows SearchBasedParserFetcher fetchers to test their corresponding
 * library APIs for their advanced search options, e.g. search in the "title" field.
 */
public interface AdvancedSearchBasedParserFetcher extends SearchBasedParserFetcher {

    /**
     * This method is used to send queries with advanced URL parameters.
     * This method is necessary as the performSearch method does not support certain URL parameters that are used for
     * fielded search, such as a title, author, or year parameter.
     *
     * @param complexSearchQuery the search query defining all fielded search parameters
     */
    default List<BibEntry> performComplexSearchQuery(ComplexSearchQuery complexSearchQuery) throws FetcherException {
        try (InputStream stream = getUrlDownload(getComplexQueryURL(complexSearchQuery)).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
            fetchedEntries.forEach(this::doPostCleanup);
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

    URL getComplexQueryURL(ComplexSearchQuery complexSearchQuery) throws URISyntaxException, MalformedURLException;
}
