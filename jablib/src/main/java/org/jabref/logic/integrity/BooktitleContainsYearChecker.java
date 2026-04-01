package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

/// Checks whether a booktitle contains an embedded 4-digit year (1000–2999).
/// Years belong in the dedicated {@code year} field, not in {@code booktitle}.
public class BooktitleContainsYearChecker implements ValueChecker {

    /// Matches a standalone 4-digit year (1000–2999) not adjacent to other digits.
    private static final Predicate<String> CONTAINS_YEAR =
            Pattern.compile("(?<![0-9])[12][0-9]{3}(?![0-9])").asPredicate();

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }
        if (CONTAINS_YEAR.test(value)) {
            return Optional.of(Localization.lang("booktitle should not contain a year"));
        }
        return Optional.empty();
    }
}
