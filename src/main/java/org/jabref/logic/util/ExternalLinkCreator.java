package org.jabref.logic.util;

import java.net.URISyntaxException;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.apache.http.client.utils.URIBuilder;

public class ExternalLinkCreator {
    private static final String SHORTSCIENCE_SEARCH_URL = "https://www.shortscience.org/internalsearch";

    /**
     * Get a URL to the search results of ShortScience for the BibEntry's title
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for successful return.
     * @return The URL if it was successfully created
     */
    public static Optional<String> getShortScienceSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).map(title -> {
            URIBuilder uriBuilder;
            try {
                uriBuilder = new URIBuilder(SHORTSCIENCE_SEARCH_URL);
            } catch (URISyntaxException e) {
                // This should never be able to happen as it would require the field to be misconfigured.
                throw new AssertionError("ShortScience URL is invalid.", e);
            }
            // Direct the user to the search results for the title.
            uriBuilder.addParameter("q", title);
            return uriBuilder.toString();
        });
    }
}
