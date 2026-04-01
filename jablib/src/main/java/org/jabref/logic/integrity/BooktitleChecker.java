package org.jabref.logic.integrity;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

public class BooktitleChecker implements ValueChecker {

    // Matches a standalone 4-digit year in the range 1000–2999
    private static final Predicate<String> CONTAINS_YEAR =
            Pattern.compile("(?<![0-9])[12][0-9]{3}(?![0-9])").asPredicate();

    // Matches explicit page-number patterns such as "pp. 1–10", "p. 5", "pages 3-7"
    private static final Predicate<String> CONTAINS_PAGES =
            Pattern.compile("(?i)\\b(pp?\\.?|pages?)\\s*[0-9]+").asPredicate();

    // Single combined pattern built from all known country names (case-insensitive, whole-word)
    private static final Predicate<String> CONTAINS_COUNTRY;

    static {
        String alternation = Countries.COUNTRY_NAMES.stream()
                                                    .map(Pattern::quote)
                                                    .collect(Collectors.joining("|"));
        CONTAINS_COUNTRY = Pattern.compile("(?i)(?<![a-z])(" + alternation + ")(?![a-z])").asPredicate();
    }

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (value.toLowerCase(Locale.ENGLISH).endsWith("conference on")) {
            return Optional.of(Localization.lang("booktitle ends with 'conference on'"));
        }

        if (CONTAINS_YEAR.test(value)) {
            return Optional.of(Localization.lang("booktitle should not contain a year"));
        }

        if (CONTAINS_COUNTRY.test(value)) {
            return Optional.of(Localization.lang("booktitle should not contain a location"));
        }

        if (CONTAINS_PAGES.test(value)) {
            return Optional.of(Localization.lang("booktitle should not contain page numbers"));
        }

        return Optional.empty();
    }
}
