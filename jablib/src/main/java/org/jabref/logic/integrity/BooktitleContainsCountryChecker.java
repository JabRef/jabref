package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

/// Checks whether a booktitle contains a country name.
/// Location information belongs in the dedicated {@code location} field, not in {@code booktitle}.
/// Country names are matched case-insensitively using a pre-compiled alternation built from
/// {@link Countries#COUNTRY_NAMES}. Word boundaries use {@code \p{Alnum}} lookarounds so that
/// short abbreviations inside alphanumeric tokens (e.g. {@code USA2015}) are not mis-flagged.
public class BooktitleContainsCountryChecker implements ValueChecker {

    private static final Pattern CONTAINS_COUNTRY;

    static {
        String alternation = Countries.COUNTRY_NAMES.stream()
                                                    .map(Pattern::quote)
                                                    .collect(Collectors.joining("|"));
        CONTAINS_COUNTRY = Pattern.compile(
                "(?i)(?<!\\p{Alnum})(" + alternation + ")(?!\\p{Alnum})");
    }

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }
        if (CONTAINS_COUNTRY.matcher(value).find()) {
            return Optional.of(Localization.lang("booktitle should not contain a location"));
        }
        return Optional.empty();
    }
}
