package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

public class BooktitleContainsPagesChecker implements ValueChecker {

    private static final Pattern CONTAINS_PAGES = Pattern.compile("(?i)\\b(pp?\\.?|pages?)\\s*[0-9]+");

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }
        if (CONTAINS_PAGES.matcher(value).find()) {
            return Optional.of(Localization.lang("booktitle should not contain page numbers"));
        }
        return Optional.empty();
    }
}
