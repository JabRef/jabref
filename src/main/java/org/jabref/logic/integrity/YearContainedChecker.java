package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class YearContainedChecker implements ValueChecker {

    // YEAR_CHECK_1 is used to identify whether there is a year after the occurrence of an author ({author} year)
    private static final Predicate<String> YEAR_CHECK_1 = Pattern.compile("\\d{4}(?=\\s*\\{[^}]+\\})").asPredicate();
    // YEAR_CHECK_2 is used to identify whether there is a year before the occurrence of an author (year {author})
    private static final Predicate<String> YEAR_CHECK_2 = Pattern.compile("\\{[^}]+\\}\\s*\\d{4}").asPredicate();

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (YEAR_CHECK_1.test(value.trim()) || YEAR_CHECK_2.test(value.trim())) {
            return Optional.of(Localization.lang("Year is contained in the booktitle"));
        }

        return Optional.empty();
    }
}
