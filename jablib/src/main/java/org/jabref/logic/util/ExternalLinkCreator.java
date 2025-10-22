package org.jabref.logic.util;

import java.net.URISyntaxException;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.apache.hc.core5.net.URIBuilder;

public class ExternalLinkCreator {
    private static final String SHORTSCIENCE_SEARCH_URL = "https://www.shortscience.org/internalsearch";
    private static final String GOOGLE_SCHOLAR_SEARCH_URL = "https://scholar.google.com/scholar";
    private static final String SEMANTIC_SCHOLAR_SEARCH_URL = "https://www.semanticscholar.org/search";

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

            // Converting LaTeX-formatted titles (e.g., containing braces) to plain Unicode to ensure compatibility with ShortScience's search URL.
            // LatexToUnicodeAdapter.format() is being used because it attempts to parse LaTeX, but gracefully degrades to a normalized title on failure.
            // This avoids sending malformed or literal LaTeX syntax titles that would give the wrong result.
            String filteredTitle = LatexToUnicodeAdapter.format(title);
            // Direct the user to the search results for the title.
            uriBuilder.addParameter("q", filteredTitle.trim());
            return uriBuilder.toString();
        });
    }

    /**
     * Get a URL to the search results of Google Scholar for the BibEntry's title.
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for successful return.
     * @return The URL if it was successfully created
     */
    public static Optional<String> getGoogleScholarSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).map(title -> {
            URIBuilder uriBuilder;
            try {
                uriBuilder = new URIBuilder(GOOGLE_SCHOLAR_SEARCH_URL);
            } catch (URISyntaxException e) {
                // This should never be able to happen as it would require the field to be misconfigured.
                throw new AssertionError("Google Scholar URL is invalid.", e);
            }

            String filteredTitle = LatexToUnicodeAdapter.format(title);
            uriBuilder.addParameter("q", filteredTitle.trim());
            return uriBuilder.toString();
        });
    }

    /**
     * Get a URL to the search results of Semantic Scholar for the BibEntry's title.
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for successful return.
     * @return The URL if it was successfully created
     */
    public static Optional<String> getSemanticScholarSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).map(title -> {
            URIBuilder uriBuilder;
            try {
                uriBuilder = new URIBuilder(SEMANTIC_SCHOLAR_SEARCH_URL);
            } catch (URISyntaxException e) {
                throw new AssertionError("Semantic Scholar URL is invalid.", e);
            }

            String filteredTitle = LatexToUnicodeAdapter.format(title);
            uriBuilder.addParameter("q", filteredTitle.trim());
            return uriBuilder.toString();
        });
    }
}
