package org.jabref.gui.fieldeditors;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileTypes;

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

    /**
     * Checks whether the given String is a URL.
     * <p>
     * Currently only checks for a protocol String.
     *
     * @param url the String to check for a URL
     * @return true if <c>url</c> contains a valid URL
     */
    public static boolean isURL(String url) {
        return url.contains("://");
    }

    /**
     * Look for the last '.' in the link, and return the following characters.
     * <p>
     * This gives the extension for most reasonably named links.
     *
     * @param link The link
     * @return The suffix, excluding the dot (e.g. "pdf")
     */
    public static Optional<String> getSuffix(final String link) {
        String strippedLink = link;
        try {
            // Try to strip the query string, if any, to get the correct suffix:
            URL url = new URL(link);
            if ((url.getQuery() != null) && (url.getQuery().length() < (link.length() - 1))) {
                strippedLink = link.substring(0, link.length() - url.getQuery().length() - 1);
            }
        } catch (MalformedURLException e) {
            // Don't report this error, since this getting the suffix is a non-critical
            // operation, and this error will be triggered and reported elsewhere.
        }
        // First see if the stripped link gives a reasonable suffix:
        String suffix;
        int strippedLinkIndex = strippedLink.lastIndexOf('.');
        if ((strippedLinkIndex <= 0) || (strippedLinkIndex == (strippedLink.length() - 1))) {
            suffix = null;
        } else {
            suffix = strippedLink.substring(strippedLinkIndex + 1);
        }
        if (!ExternalFileTypes.getInstance().isExternalFileTypeByExt(suffix)) {
            // If the suffix doesn't seem to give any reasonable file type, try
            // with the non-stripped link:
            int index = link.lastIndexOf('.');
            if ((index <= 0) || (index == (link.length() - 1))) {
                // No occurrence, or at the end
                // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (strippedLink.substring(strippedLinkIndex + 1).indexOf('/') >= 1) {
                    return Optional.empty();
                } else {
                    return Optional.of(suffix); // return the first one we found, anyway.
                }
            } else {
                // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (link.substring(index + 1).indexOf('/') >= 1) {
                    return Optional.empty();
                } else {
                    return Optional.of(link.substring(index + 1));
                }
            }
        } else {
            return Optional.ofNullable(suffix);
        }
    }
}
