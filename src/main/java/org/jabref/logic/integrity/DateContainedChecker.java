package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class DateContainedChecker implements ValueChecker {

    // DATE_CHECK is used to identify two potential date patterns (1. Month xx-xx, Year  2. Month xx, Year). x denotes a digit and can either be double or single digits
    private static final Predicate<String> DATE_CHECK = Pattern.compile("(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s\\d{1,2}(?:-\\d{1,2})?,\\s\\d{4}").asPredicate();

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (DATE_CHECK.test(value.trim())) {
            return Optional.of(Localization.lang("Date is contained in the booktitle"));
        }

        return Optional.empty();
    }
}
