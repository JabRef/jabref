package org.jabref.logic.integrity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

public class BooktitleContainsCountryChecker implements ValueChecker {

    private static final Pattern CONTAINS_COUNTRY;

    static {
        String alternation = Arrays.stream(Locale.getISOCountries())
                                   .map(code -> Locale.of("", code).getDisplayCountry(Locale.ENGLISH))
                                   .filter(name -> !name.isEmpty())
                                   .map(name -> name.toLowerCase(Locale.ENGLISH))
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
