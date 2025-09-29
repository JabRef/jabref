package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.RegexPatterns;
import org.jabref.model.strings.StringUtil;

public class BooktitlePageRangeChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (RegexPatterns.PAGE_RANGE_PATTERN.asPredicate().test(value)) {
            return Optional.of(Localization.lang("Page range found in booktitle"));
        }

        return Optional.empty();
    }
}
