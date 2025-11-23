package org.jabref.logic.util;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalLinkCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalLinkCreator.class);

    private static final String DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL = "https://scholar.google.com/scholar";
    private static final String DEFAULT_SEMANTIC_SCHOLAR_SEARCH_URL = "https://www.semanticscholar.org/search";
    private static final String DEFAULT_SHORTSCIENCE_SEARCH_URL = "https://www.shortscience.org/internalsearch";

    private final ImporterPreferences importerPreferences;

    public ExternalLinkCreator(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    // Note: We use configurable templates due to the requirement stated at https://github.com/JabRef/jabref/issues/12268#issuecomment-2523108605

    /**
     * Get a URL to the search results of Google Scholar for the BibEntry's title
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for a successful return.
     * @return The URL if it was successfully created
     */
    public Optional<String> getGoogleScholarSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).flatMap(title -> {
            String baseUrl = importerPreferences.getSearchEngineUrlTemplates()
                                                .getOrDefault("Google Scholar", DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL);
            String author = entry.getField(StandardField.AUTHOR).orElse(null);
            return buildSearchUrl(baseUrl, DEFAULT_GOOGLE_SCHOLAR_SEARCH_URL, title, author, "Google Scholar", false);
        });
    }

    /**
     * Get a URL to the search results of Semantic Scholar for the BibEntry's title
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for a successful return.
     * @return The URL if it was successfully created
     */
    public Optional<String> getSemanticScholarSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).flatMap(title -> {
            String baseUrl = importerPreferences.getSearchEngineUrlTemplates()
                                                .getOrDefault("Semantic Scholar", DEFAULT_SEMANTIC_SCHOLAR_SEARCH_URL);
            String author = entry.getField(StandardField.AUTHOR).orElse(null);
            return buildSearchUrl(baseUrl, DEFAULT_SEMANTIC_SCHOLAR_SEARCH_URL, title, author, "Semantic Scholar", true);
        });
    }

    /**
     * Get a URL to the search results of ShortScience for the BibEntry's title
     *
     * @param entry The entry to search for. Expects the BibEntry's title to be set for a successful return.
     * @return The URL if it was successfully created
     */
    public Optional<String> getShortScienceSearchURL(BibEntry entry) {
        return entry.getField(StandardField.TITLE).flatMap(title -> {
            String baseUrl = importerPreferences.getSearchEngineUrlTemplates()
                                                .getOrDefault("Short Science", DEFAULT_SHORTSCIENCE_SEARCH_URL);
            String author = entry.getField(StandardField.AUTHOR).orElse(null);
            return buildSearchUrl(baseUrl, DEFAULT_SHORTSCIENCE_SEARCH_URL, title, author, "ShortScience", false);
        });
    }

    /**
     * Builds a search URL using either template replacement or query parameters
     *
     * @param baseUrl The custom or default base URL
     * @param defaultUrl The fallback default URL
     * @param title The title to search for
     * @param author Optional author to include in search (null if not present)
     * @param serviceName Name of the service for logging
     * @paramm addAuthorIndex formats all authors as separate keys with indexing ("author[0]", "author[1]", etc.)
     * @return Optional containing the constructed URL, or empty if construction failed
     */
    private Optional<String> buildSearchUrl(String baseUrl, String defaultUrl, String title, @Nullable String author, String serviceName, boolean addAuthorIndex) {
        // Converting LaTeX-formatted titles (e.g., containing braces) to plain Unicode to ensure compatibility with ShortScience's search URL.
        // LatexToUnicodeAdapter.format() is being used because it attempts to parse LaTeX, but gracefully degrades to a normalized title on failure.
        // This avoids sending malformed or literal LaTeX syntax titles that would give the wrong result.
        String filteredTitle = LatexToUnicodeAdapter.format(title);

        // Validate the base URL scheme to prevent injection attacks
        // We cannot use URLUtil#isValidHttpUrl here as {title} placeholder will throw URISyntaxException till replaced (below)
        String lowerUrl = baseUrl.toLowerCase().trim();
        if (StringUtil.isBlank(lowerUrl) || !(lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://"))) {
            LOGGER.warn("Invalid URL scheme in {} preference: {}. Using default URL.", serviceName, baseUrl);
            return buildUrlWithQueryParams(defaultUrl, filteredTitle, author, serviceName, addAuthorIndex);
        }

        // If URL doesn't contain {title}, it's not a valid template, use query parameters
        if (!baseUrl.contains("{title}")) {
            LOGGER.warn("URL template for {} doesn't contain {{title}} placeholder. Using query parameters.", serviceName);
            return buildUrlWithQueryParams(defaultUrl, filteredTitle, author, serviceName, addAuthorIndex);
        }

        // Replace placeholders with URL-encoded values
        try {
            String encodedTitle = URLEncoder.encode(filteredTitle.trim(), StandardCharsets.UTF_8);
            String urlWithTitle = baseUrl.replace("{title}", encodedTitle);
            String finalUrl;

            if (author != null) {
                String encodedAuthor = URLEncoder.encode(author.trim(), StandardCharsets.UTF_8);
                finalUrl = urlWithTitle.replace("{author}", encodedAuthor);
            } else {
                // Remove the {author} placeholder if no author is present
                finalUrl = urlWithTitle.replace("{author}", "");
            }

            // Validate the final constructed URL
            if (URLUtil.isValidHttpUrl(finalUrl)) {
                return Optional.of(finalUrl);
            } else {
                LOGGER.warn("Constructed URL for {} is invalid: {}. Using default URL.", serviceName, finalUrl);
                return buildUrlWithQueryParams(defaultUrl, filteredTitle, author, serviceName, addAuthorIndex);
            }
        } catch (Exception ex) {
            LOGGER.error("Error constructing URL for {}: {}", serviceName, ex.getMessage(), ex);
            return buildUrlWithQueryParams(defaultUrl, filteredTitle, author, serviceName, addAuthorIndex);
        }
    }

    /**
     * Builds a URL using query parameters (fallback method).
     * <p>
     * The parameter addAuthorIndex is used for Semantic Scholar service because it does not understand "author=XYZ", but it uses "author[0]=XYZ&author[1]=ABC".
     */
    private Optional<String> buildUrlWithQueryParams(String baseUrl, String title, @Nullable String author, String serviceName, boolean addAuthorIndex) {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            // Title is already converted to Unicode by buildSearchUrl before reaching here
            uriBuilder.addParameter("q", title.trim());
            if (author != null) {
                if (addAuthorIndex) {
                    AuthorListParser authorListParser = new AuthorListParser();
                    AuthorList authors = authorListParser.parse(author);

                    int idx = 0;
                    for (Author authorObject : authors) {
                        uriBuilder.addParameter("author[" + idx + "]", authorObject.getNameForAlphabetization());
                        ++idx;
                    }
                } else {
                    uriBuilder.addParameter("author", author.trim());
                }
            }
            return Optional.of(uriBuilder.toString());
        } catch (URISyntaxException ex) {
            LOGGER.error("Failed to construct {} URL: {}", serviceName, ex.getMessage());
            return Optional.empty();
        }
    }
}
