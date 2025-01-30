package org.jabref.logic.integrity;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class BooktitleChecker implements ValueChecker {

    private static final Pattern FULL_URL_PATTERN = Pattern.compile(
            "(https?://\\S+/\\S+|www\\.\\S+/\\S+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DOMAIN_ONLY_PATTERN = Pattern.compile(
            "(https?://\\S+|www\\.\\S+)(/|$)", Pattern.CASE_INSENSITIVE);

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (value.toLowerCase(Locale.ENGLISH).endsWith("conference on")) {
            return Optional.of(Localization.lang("booktitle ends with 'conference on'"));
        }

        if (FULL_URL_PATTERN.matcher(value).find()) {
            return Optional.of(Localization.lang("The book title contains a full URL which is forbidden"));
        }

        if (DOMAIN_ONLY_PATTERN.matcher(value).find()) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}
