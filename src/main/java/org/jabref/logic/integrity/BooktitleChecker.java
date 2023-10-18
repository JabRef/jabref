package org.jabref.logic.integrity;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class BooktitleChecker implements ValueChecker {

    private static final Predicate<String> DATE_IN_BOOKTITLE = Pattern.compile("(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s\\d{1,2}(?:-\\d{1,2})?,\\s\\d{4}").asPredicate();
    private static final Predicate<String> AUTHOR_IN_BOOKTITLE = Pattern.compile("\\{([^}]+)\\}").asPredicate();
    private static final Predicate<String> PAGE_NUMBERS_IN_BOOKTITLE = Pattern.compile("\\b(P(?:age)?\\.?\\s?\\d+(?:-\\d+)?)\\b").asPredicate();

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (value.toLowerCase(Locale.ENGLISH).endsWith("conference on")) {
            return Optional.of(Localization.lang("booktitle ends with 'conference on'"));
        } else
            // DATE CHECK
            if (DATE_IN_BOOKTITLE.test(value.trim())) {
                return Optional.of(Localization.lang("Date is contained in the booktitle"));
        } else
            // AUTHOR CHECK
            if (AUTHOR_IN_BOOKTITLE.test(value.trim())) {
                return Optional.of(Localization.lang("Author is contained in the booktitle"));
        } else
            // PAGE NUMBER CHECK
            if (PAGE_NUMBERS_IN_BOOKTITLE.test(value.trim())) {
                return Optional.of(Localization.lang("Page numbers are contained in the booktitle"));

        }

        // YEAR CHECK

        // LOCATION CHECK

        return Optional.empty();
    }
}
