package org.jabref.logic.importer;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a convenient interface for {@link IdFetcher}, which follow the usual three-step procedure:
 * 1. Open a URL based on the search query
 * 2. Parse the response to get a list of {@link BibEntry}
 * 3. Extract identifier
 */
public interface IdParserFetcher<T extends Identifier> extends IdFetcher<T> {

    Logger LOGGER = LoggerFactory.getLogger(IdParserFetcher.class);

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

    /**
     * Extracts the identifier from the list of fetched entries.
     *
     * @param inputEntry     the entry for which we are searching the identifier (can be used to find closest match in
     *                       the result)
     * @param fetchedEntries list of entries returned by the web service
     */
    Optional<T> extractIdentifier(BibEntry inputEntry, List<BibEntry> fetchedEntries) throws FetcherException;

    @Override
    default Optional<T> findIdentifier(BibEntry entry) throws FetcherException {
        Objects.requireNonNull(entry);

        try (InputStream stream = new BufferedInputStream(getURLForEntry(entry).openStream())) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

            if (fetchedEntries.isEmpty()) {
                return Optional.empty();
            }

            // Post-cleanup
            fetchedEntries.forEach(this::doPostCleanup);

            return extractIdentifier(entry, fetchedEntries);
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (FileNotFoundException e) {
            LOGGER.debug("Id not found");
            return Optional.empty();
        } catch (IOException e) {
            // TODO: Catch HTTP Response 401 errors and report that user has no rights to access resource
            // TODO catch 503 service unavailable and alert user
            throw new FetcherException("An I/O exception occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
    }
}
