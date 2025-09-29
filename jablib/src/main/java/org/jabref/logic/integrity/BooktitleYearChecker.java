package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.RegexPatterns;
import org.jabref.model.strings.StringUtil;

public class BooktitleYearChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (RegexPatterns.YEAR_PATTERN.asPredicate().test(value)) {
            return Optional.of(Localization.lang("Year(s) present in booktitle"));
        }

        return Optional.empty();
    }
}
