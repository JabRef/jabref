package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.RegexPatterns;
import org.jabref.model.strings.StringUtil;

public class BooktitleMonthChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (RegexPatterns.MONTHS_PATTERN.asPredicate().test(value)) {
            return Optional.of(Localization.lang("Month found in booktitle"));
        }

        return Optional.empty();
    }
}
