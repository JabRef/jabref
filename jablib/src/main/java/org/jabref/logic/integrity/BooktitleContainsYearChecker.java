package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

public class BooktitleContainsYearChecker implements ValueChecker {

    private static final Pattern CONTAINS_YEAR = Pattern.compile("(?<!\\p{Alnum})[12][0-9]{3}(?!\\p{Alnum})");

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }
        if (CONTAINS_YEAR.matcher(value).find()) {
            return Optional.of(Localization.lang("booktitle should not contain a year"));
        }
        return Optional.empty();
    }
}
