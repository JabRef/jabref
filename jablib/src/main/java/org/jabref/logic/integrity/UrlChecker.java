package org.jabref.logic.integrity;

import java.net.URISyntaxException;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import org.apache.hc.core5.net.URIBuilder;

public class UrlChecker implements ValueChecker {
    /**
     * Validates that a URL contains a protocol and conforms to RFC 3986 URI syntax (updated RFC 2396).
     */
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        try {
            new URIBuilder(value);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }

        if (!value.contains("://")) {
            return Optional.of(Localization.lang("should contain a protocol") + ": http[s]://, file://, ftp://, ...");
        }

        return Optional.empty();
    }
}
