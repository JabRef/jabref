package org.jabref.logic.formatter.bibtexfields;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Cleanup URL link.
 * <p>
 *     Expose string representations URL links clean up logic.
 * </p>
 */
public class CleanupUrlFormatter extends Formatter {

    // This regexp find "url=" or "to=" parameter in full link and get text after them
    private static final Pattern PATTERN_URL = Pattern.compile("(?:url|to)=([^&]*)");

    @Override
    public String getName() {
        return Localization.lang("Cleanup URL link");
    }

    @Override
    public String getKey() {
        return "cleanup_url";
    }

    /**
     * Escape and decodes a String from the application/x-www-form-urlencoded MIME format.
     * <p>
     * Method will also try to find a URL placed after "url=" or "to=".
     * <p>
     * The conversion process is the same as executed by {@link URLDecoder} to try to
     * take guarantees against code injections.
     * <p>
     * The plus sign is replaced by its correspondent code (%2b) to avoid the character
     * to be replaced by a space during the decoding execution.
     *
     * @param url should not be null
     * @return the decoded URL as a String representation
     *
     * @see URLDecoder#decode(String, Charset)
     */
    @Override
    public String format(String url) {
        var toDecode = Objects
                .requireNonNull(url, "Null url")
                .replaceAll("\\+", "%2b");
        Matcher matcher = PATTERN_URL.matcher(toDecode);
        if (matcher.find()) {
            return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
        }
        return URLDecoder.decode(toDecode, StandardCharsets.UTF_8);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Cleanup URL link by removing special symbols and extracting simple link");
    }

    @Override
    public String getExampleInput() {
        return "https://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=11&cad=" +
               "rja&uact=8&ved=0ahUKEwjg3ZrB_ZPXAhVGuhoKHYdOBOg4ChAWCCYwAA&url=" +
               "http%3A%2F%2Fwww.focus.de%2Fgesundheit%2Fratgeber%2Fherz%2Ftest%2" +
               "Flebenserwartung-werden-sie-100-jahre-alt_aid_363828.html" +
               "&usg=AOvVaw1G6m2jf-pTHYkXceii4hXU";
    }
}
