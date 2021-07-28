package org.jabref.logic.importer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * Provides a convenient interface for citation-based fetcher, which follow the usual four-step procedure:
 * <ol>
 *   <li>Send request with doi of the entry</li>
 *   <li>Parse the response to get a list of DOIs</li>
 *   <li>Send new request to get bibliographic information for every DOI</li>
 *   <li>Parse the response and create a List of {@link BibEntry}</li>
 * </ol>
 */
public interface CitationBasedParserFetcher extends CitationFetcher {

    /**
     * Constructs a URL-String based on the given BibEntries.
     *
     * @param entries the entries to look information for
     */
    URL getURLForEntries(List<BibEntry> entries, SearchType searchType) throws URISyntaxException, MalformedURLException, FetcherException;

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser(SearchType searchType);

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
     *
     * @param entry the entry to be cleaned-up
     */
    default void doPostCleanup(BibEntry entry) {
        // Do nothing by default
    }

    @Override
    default List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
        Objects.requireNonNull(entry);
        return performSearch(entry, SearchType.CITED_BY);
    }

    @Override
    default List<BibEntry> searchCiting(BibEntry entry) throws FetcherException {
        Objects.requireNonNull(entry);
        return performSearch(entry, SearchType.CITING);
    }

    /**
     * Searches specified URL for related articles based on a {@link BibEntry}
     * and the search type {@link org.jabref.logic.importer.fetcher.OpenCitationFetcher.SearchType}
     *
     * @param entry      entry to search information for
     * @param searchType type of search to perform (CITING, CITEDBY, BIBINFO)
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     * @throws FetcherException Error message passed to {@link org.jabref.gui.entryeditor.CitationRelationsTab}
     */
    default List<BibEntry> performSearch(BibEntry entry, SearchType searchType) throws FetcherException {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(searchType);

        List<BibEntry> entries = new ArrayList<>();
        if (entry.getField(StandardField.DOI).isEmpty()) {
            return Collections.emptyList();
        }
        entries.add(entry);
        try (InputStream stream = new BufferedInputStream(getURLForEntries(entries, searchType).openStream())) {
            List<BibEntry> fetchedEntries = getParser(searchType).parseEntries(stream);

            // Post-cleanup
            fetchedEntries.forEach(this::doPostCleanup);

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
