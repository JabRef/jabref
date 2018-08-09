package org.jabref.logic.formatter.bibtexfields;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Cleanup URL link
 */
public class CleanupURLFormatter extends Formatter {

    private static final Log LOGGER = LogFactory.getLog(CleanupURLFormatter.class);
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

    @Override
    public String format(String value) {
        String decodedLink = value;
        String toDecode = value;

        Matcher matcher = PATTERN_URL.matcher(value);
        if (matcher.find()) {
          toDecode = matcher.group(1);
        }
        try {
            decodedLink = URLDecoder.decode(toDecode, StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e) {
            LOGGER.warn("Used unsupported character encoding", e);
        }
        return decodedLink;
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
                "Flebenserwartung-werden-sie-100-jahre-alt_aid_363828.html" + "&usg=AOvVaw1G6m2jf-pTHYkXceii4hXU";
  }

}
