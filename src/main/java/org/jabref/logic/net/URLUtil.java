package org.jabref.logic.net;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class URLUtil {
    private static final String URL_EXP = "^(https?|ftp)://.+";

    // Detect Google search URL
    private static final String GOOGLE_SEARCH_EXP = "^https?://(?:www\\.)?google\\.[\\.a-z]+?/url.*";

    private URLUtil() {
    }

    /**
     * Cleans URLs returned by Google search.
     *
     * <example>
     *  If you copy links from search results from Google, all links will be enriched with search meta data, e.g.
     *  https://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&&url=http%3A%2F%2Fwww.inrg.csie.ntu.edu.tw%2Falgorithm2014%2Fhomework%2FWagner-74.pdf&ei=DifeVYHkDYWqU5W0j6gD&usg=AFQjCNFl638rl5KVta1jIMWLyb4CPSZidg&sig2=0hSSMw9XZXL3HJWwEcJtOg
     * </example>
     *
     * @param url the Google search URL string
     * @return the cleaned Google URL or @code{url} if no search URL was detected
     */
    public static String cleanGoogleSearchURL(String url) {
        Objects.requireNonNull(url);

        if (!url.matches(GOOGLE_SEARCH_EXP)) {
            return url;
        }
        // Extract destination URL
        try {
            URL searchURL = new URL(url);
            // URL parameters
            String query = searchURL.getQuery();
            // no parameters
            if (query == null) {
                return url;
            }
            // extract url parameter
            String[] pairs = query.split("&");

            for (String pair: pairs) {
                // "clean" url is decoded value of "url" parameter
                if (pair.startsWith("url=")) {
                    String value = pair.substring(pair.indexOf('=') + 1, pair.length());

                    String decode = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                    // url?
                    if (decode.matches(URL_EXP)) {
                        return decode;
                    }
                }
            }
            return url;
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            return url;
        }
    }

    /**
     * Checks whether the given String is a URL.
     * Currently only checks for a protocol String.
     *
     * @param url the String to check for a URL
     * @return true if <c>url</c> contains a valid URL
     */
    public static boolean isURL(String url) {
        return url.contains("://");
    }

}
