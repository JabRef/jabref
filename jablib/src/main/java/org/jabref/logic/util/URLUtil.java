package org.jabref.logic.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;

/// URL utilities for URLs in the JabRef logic.
///
/// For GUI-oriented URL utilities see `org.jabref.gui.fieldeditors.URLUtil`
public class URLUtil {

    private static final String URL_REGEX = "(?i)\\b((?:https?|ftp)://[^\\s]+)";

    /**
     * Pattern matches a string containing a URL with a protocol
     */
    public static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String URL_EXP = "^(https?|ftp)://.+";
    // Detect Google search URL
    private static final String GOOGLE_SEARCH_EXP = "^https?://(?:www\\.)?google\\.[\\.a-z]+?/url.*";

    /**
     * Cleans URLs returned by Google search.
     * <h4>Example</h4>
     * <pre>{@code
     * If you copy links from search results from Google, all links will be enriched with search meta data, e.g.
     * https://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&&url=http%3A%2F%2Fwww.inrg.csie.ntu.edu.tw%2Falgorithm2014%2Fhomework%2FWagner-74.pdf&ei=DifeVYHkDYWqU5W0j6gD&usg=AFQjCNFl638rl5KVta1jIMWLyb4CPSZidg&sig2=0hSSMw9XZXL3HJWwEcJtOg
     * }</pre>
     *
     * @param url the Google search URL string
     * @return the cleaned Google URL or @code{url} if no search URL was detected
     */
    public static String cleanGoogleSearchURL(@NonNull String url) {
        if (!url.matches(GOOGLE_SEARCH_EXP)) {
            return url;
        }
        // Extract destination URL
        try {
            URL searchURL = create(url);
            // URL parameters
            String query = searchURL.getQuery();
            // no parameters
            if (query == null) {
                return url;
            }
            // extract url parameter
            String[] pairs = query.split("&");

            for (String pair : pairs) {
                // "clean" url is decoded value of "url" parameter
                if (pair.startsWith("url=")) {
                    String value = pair.substring(pair.indexOf('=') + 1);

                    String decode = URLDecoder.decode(value, StandardCharsets.UTF_8);
                    // url?
                    if (decode.matches(URL_EXP)) {
                        return decode;
                    }
                }
            }
            return url;
        } catch (MalformedURLException e) {
            return url;
        }
    }

    /// Checks whether the given String is a URL.
    ///
    /// Currently only checks for a protocol String.
    ///
    /// @param url the String to check for a URL
    /// @return true if `url` contains a valid URL
    public static boolean isURL(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // Check if the URL has a protocol (http://, https://, ftp://)
        if (!URL_PATTERN.matcher(url).matches()) {
            return false;
        }

        try {
            create(url);
            return true;
        } catch (MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Creates a {@link URL} object from the given string URL.
     *
     * @param url the URL string to be converted into a {@link URL}.
     * @return the {@link URL} object created from the string URL.
     * @throws MalformedURLException if the URL is malformed and cannot be converted to a {@link URL}.
     */
    public static URL create(String url) throws MalformedURLException {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL must not be null or empty.");
        }

        String trimmedUrl = url.trim();

        // Add https:// prefix to URLs starting with www. to make them absolute
        if (trimmedUrl.startsWith("www.")) {
            trimmedUrl = "https://" + trimmedUrl;
        }

        try {
            URI parsedUri = new URI(trimmedUrl);
            if (!parsedUri.isAbsolute()) {
                throw new MalformedURLException("URI is not absolute: " + url);
            }
            if (parsedUri.getScheme() == null || parsedUri.getHost() == null) {
                throw new MalformedURLException("URI must include both scheme and host: " + url);
            }
            return parsedUri.toURL();
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new MalformedURLException("Invalid URI: " + url + " | " + e.getMessage());
        }
    }

    /**
     * Creates a {@link URI} object from the given string URL.
     * This method attempts to convert the given URL string into a {@link URI} object.
     * The pipe character ('|') is replaced with its percent-encoded equivalent ("%7C") because the pipe character
     * is only a valid character according to RFC3986. However, JDK's URI implementation is implementing RFC2396 and RFC2732, but not RFC3986.
     *
     * @param url the URL string to be converted into a {@link URI}.
     * @return the {@link URI} object created from the string URL.
     * @throws IllegalArgumentException if the string URL is not a valid URI or if the URI format is incorrect.
     * @throws URISyntaxException       if the string URL has an invalid syntax and cannot be converted into a {@link URI}.
     */
    public static URI createUri(String url) {
        try {
            // Replace '|' character with its percent-encoded representation '%7C'.
            String urlFormat = url.replace("|", "%7C");
            return new URI(urlFormat);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
