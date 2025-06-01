package org.jabref.logic.util;

import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.hc.core5.net.URIBuilder;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class ExternalLinkCreator {
    private static final String DEFAULT_SHORTSCIENCE_SEARCH_URL = "https://www.shortscience.org/internalsearch";
    private static final String DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL = "https://scholar.google.com/scholar";

    private final ImporterPreferences importerPreferences;

    public ExternalLinkCreator(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    /**
     * Get a URL to the search results of ShortScience for the BibEntry's title
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for successful return.
     * @return The URL if it was successfully created
     */
    public Optional<String> getShortScienceSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).map(title -> {
            // Use custom URL template if available, otherwise use default
            String baseUrl = importerPreferences.getSearchEngineUrlTemplates()
                                                .getOrDefault("Short Science", DEFAULT_SHORTSCIENCE_SEARCH_URL);

            Optional<String> author = entry.getField(StandardField.AUTHOR);

            // If URL doesn't contain {title}, it's invalid, use default
            if (!baseUrl.contains("{title}")) {
                try {
                    URIBuilder uriBuilder = new URIBuilder(DEFAULT_SHORTSCIENCE_SEARCH_URL);
                    uriBuilder.addParameter("q", title);
                    author.ifPresent(a -> uriBuilder.addParameter("author", a));
                    return uriBuilder.toString();
                } catch (URISyntaxException ex) {
                    throw new AssertionError("ShortScience URL is invalid.", ex);
                }
            }

            String urlWithTitle = baseUrl.replace("{title}", title);
            return author.map(a -> urlWithTitle.replace("{author}", a)).orElse(urlWithTitle);
        });
    }

    /**
     * Get a URL to the search results of Google Scholar for the BibEntry's title
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for successful return.
     * @return The URL if it was successfully created
     */
    public Optional<String> getGoogleScholarSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).map(title -> {
            // Use custom URL template if available, otherwise use default
            String baseUrl = importerPreferences.getSearchEngineUrlTemplates()
                                                .getOrDefault("Google Scholar", DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL);

            Optional<String> author = entry.getField(StandardField.AUTHOR);

            // If URL doesn't contain {title}, it's invalid, use default
            if (!baseUrl.contains("{title}")) {
                try {
                    URIBuilder uriBuilder = new URIBuilder(DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL);
                    uriBuilder.addParameter("q", title);
                    author.ifPresent(a -> uriBuilder.addParameter("author", a));
                    return uriBuilder.toString();
                } catch (URISyntaxException ex) {
                    throw new AssertionError("Default Google Scholar URL is invalid.", ex);
                }
            }

            String urlWithTitle = baseUrl.replace("{title}", title);
            return author.map(a -> urlWithTitle.replace("{author}", a)).orElse(urlWithTitle);
        });
    }
}
