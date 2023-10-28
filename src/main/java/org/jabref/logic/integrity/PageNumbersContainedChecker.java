package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class PageNumbersContainedChecker implements ValueChecker {

    // PAGE_NUMBERS_CHECK is used to identify whether there are page numbers in the booktitle by looking for patterns like [Page x, Page. x, Pg x, Pg. x, Pp x, Pp. x] where x denotes an integer of any length
    private static final Predicate<String> PAGE_NUMBERS_CHECK = Pattern.compile("\\b(Page|Pg|Pp)\\.? \\d+\\b", Pattern.CASE_INSENSITIVE).asPredicate();

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (PAGE_NUMBERS_CHECK.test(value.trim())) {
            return Optional.of(Localization.lang("Page numbers are contained in the booktitle"));
        }

        return Optional.empty();
    }
}
