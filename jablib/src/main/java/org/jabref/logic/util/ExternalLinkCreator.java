package org.jabref.logic.util;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalLinkCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalLinkCreator.class);

    private static final String DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL = "https://scholar.google.com/scholar";
    private static final String DEFAULT_SHORTSCIENCE_SEARCH_URL = "https://www.shortscience.org/internalsearch";

    private final ImporterPreferences importerPreferences;

    public ExternalLinkCreator(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    /**
     * Get a URL to the search results of Google Scholar for the BibEntry's title
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for successful return.
     * @return The URL if it was successfully created
     */
    public Optional<String> getGoogleScholarSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).flatMap(title -> {
            String baseUrl = importerPreferences.getSearchEngineUrlTemplates()
                                                .getOrDefault("Google Scholar", DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL);
            Optional<String> author = entry.getField(StandardField.AUTHOR);
            return buildSearchUrl(baseUrl, DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL, title, author, "Google Scholar");
        });
    }

    /**
     * Get a URL to the search results of ShortScience for the BibEntry's title
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for successful return.
     * @return The URL if it was successfully created
     */
    public Optional<String> getShortScienceSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).flatMap(title -> {
            String baseUrl = importerPreferences.getSearchEngineUrlTemplates()
                                                .getOrDefault("Short Science", DEFAULT_SHORTSCIENCE_SEARCH_URL);
            Optional<String> author = entry.getField(StandardField.AUTHOR);
            return buildSearchUrl(baseUrl, DEFAULT_SHORTSCIENCE_SEARCH_URL, title, author, "ShortScience");
        });
    }

    /**
     * Builds a search URL using either template replacement or query parameters
     *
     * @param baseUrl The custom or default base URL
     * @param defaultUrl The fallback default URL
     * @param title The title to search for
     * @param author Optional author to include in search
     * @param serviceName Name of the service for logging
     * @return Optional containing the constructed URL, or empty if construction failed
     */
    private Optional<String> buildSearchUrl(String baseUrl, String defaultUrl, String title, Optional<String> author, String serviceName) {
        // Converting LaTeX-formatted titles (e.g., containing braces) to plain Unicode to ensure compatibility with ShortScience's search URL.
        // LatexToUnicodeAdapter.format() is being used because it attempts to parse LaTeX, but gracefully degrades to a normalized title on failure.
        // This avoids sending malformed or literal LaTeX syntax titles that would give the wrong result.
        String filteredTitle = LatexToUnicodeAdapter.format(title);

        // Validate the base URL scheme to prevent injection attacks
        if (!isValidHttpUrl(baseUrl)) {
            LOGGER.warn("Invalid URL scheme in {} preference: {}. Using default URL.", serviceName, baseUrl);
            return buildUrlWithQueryParams(defaultUrl, filteredTitle, author, serviceName);
        }

        // If URL doesn't contain {title}, it's not a valid template, use query parameters
        if (!baseUrl.contains("{title}")) {
            LOGGER.debug("URL template for {} doesn't contain {{title}} placeholder. Using query parameters.", serviceName);
            return buildUrlWithQueryParams(defaultUrl, filteredTitle, author, serviceName);
        }

        // Replace placeholders with URL-encoded values
        try {
            String encodedTitle = URLEncoder.encode(filteredTitle.trim(), StandardCharsets.UTF_8);
            String urlWithTitle = baseUrl.replace("{title}", encodedTitle);
            String finalUrl;

            if (author.isPresent()) {
                String encodedAuthor = URLEncoder.encode(author.get().trim(), StandardCharsets.UTF_8);
                finalUrl = urlWithTitle.replace("{author}", encodedAuthor);
            } else {
                // Remove {author} placeholder if no author is present
                finalUrl = urlWithTitle.replace("{author}", "");
            }

            // Validate the final constructed URL
            if (isValidUrl(finalUrl)) {
                return Optional.of(finalUrl);
            } else {
                LOGGER.warn("Constructed URL for {} is invalid: {}. Using default URL.", serviceName, finalUrl);
                return buildUrlWithQueryParams(defaultUrl, filteredTitle, author, serviceName);
            }
        } catch (Exception ex) {
            LOGGER.error("Error constructing URL for {}: {}", serviceName, ex.getMessage(), ex);
            return buildUrlWithQueryParams(defaultUrl, filteredTitle, author, serviceName);
        }
    }

    /**
     * Builds a URL using query parameters (fallback method)
     */
    private Optional<String> buildUrlWithQueryParams(String baseUrl, String title, Optional<String> author, String serviceName) {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            // Title is already converted to Unicode by buildSearchUrl before reaching here
            uriBuilder.addParameter("q", title.trim());
            author.ifPresent(a -> uriBuilder.addParameter("author", a.trim()));
            return Optional.of(uriBuilder.toString());
        } catch (URISyntaxException ex) {
            LOGGER.error("Failed to construct {} URL: {}", serviceName, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validates that a URL has an HTTP or HTTPS scheme to prevent injection attacks
     */
    private boolean isValidHttpUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase().trim();
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://");
    }

    /**
     * Validates that a constructed URL is valid
     */
    private boolean isValidUrl(String url) {
        try {
            new URIBuilder(url);
            return isValidHttpUrl(url);
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
