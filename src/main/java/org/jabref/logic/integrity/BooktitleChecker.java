package org.jabref.logic.integrity;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class BooktitleChecker implements ValueChecker {
    // DATE_CHECK is used to identify two potential date patterns (1. Month xx-xx, Year  2. Month xx, Year). x denotes a digit and can either be double or single digits
    private static final Predicate<String> DATE_CHECK = Pattern.compile("(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s\\d{1,2}(?:-\\d{1,2})?,\\s\\d{4}").asPredicate();
    // AUTHOR_CHECK is used to identify whether there is a pattern that matches '{author}' which represents how an Author is included in a booktitle
    private static final Predicate<String> AUTHOR_CHECK = Pattern.compile("\\{([^}]+)\\}").asPredicate();
    // PAGE_NUMBERS_CHECK is used to identify whether there are page numbers in the booktitle by looking for patterns like [Page x, Page. x, Pg x, Pg. x, Pp x, Pp. x] where x denotes an integer of any length
    private static final Predicate<String> PAGE_NUMBERS_CHECK = Pattern.compile("\\b(Page|Pg|Pp)\\.? \\d+\\b", Pattern.CASE_INSENSITIVE).asPredicate();
    // YEAR_CHECK_1 is used to identify whether there is a year after the occurrence of an author ({author} year)
    private static final Predicate<String> YEAR_CHECK_1 = Pattern.compile("\\d{4}(?=\\s*\\{[^}]+\\})").asPredicate();
    // YEAR_CHECK_2 is used to identify whether there is a year before the occurrence of an author (year {author})
    private static final Predicate<String> YEAR_CHECK_2 = Pattern.compile("\\{[^}]+\\}\\s*\\d{4}").asPredicate();

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (value.toLowerCase(Locale.ENGLISH).endsWith("conference on")) {
            return Optional.of(Localization.lang("booktitle ends with 'conference on'"));
        }

        if (DATE_CHECK.test(value.trim())) {
            return Optional.of(Localization.lang("Date is contained in the booktitle"));
        }

        if (YEAR_CHECK_1.test(value.trim()) || YEAR_CHECK_2.test(value.trim())) {
            return Optional.of(Localization.lang("Year is contained in the booktitle"));
        }

        if (AUTHOR_CHECK.test(value.trim())) {
            return Optional.of(Localization.lang("Author is contained in the booktitle"));
        }

        if (PAGE_NUMBERS_CHECK.test(value.trim())) {
            return Optional.of(Localization.lang("Page numbers are contained in the booktitle"));
        }

        return Optional.empty();
    }
}
