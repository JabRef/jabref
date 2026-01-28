package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;

public class NoURLChecker implements ValueChecker {

    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (URLUtil.URL_PATTERN.matcher(value).find()) {
            return Optional.of(Localization.lang("contains a URL"));
        }

        return Optional.empty();
    }
}
