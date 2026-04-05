package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

/// Checks whether a booktitle contains an explicit page-number pattern.
/// Page numbers belong in the dedicated {@code pages} field, not in {@code booktitle}.
/// Detected patterns include {@code pp. 1}, {@code p. 5}, {@code pages 3-7}, etc.
public class BooktitleContainsPagesChecker implements ValueChecker {

    /// Matches explicit page-number patterns such as "pp. 1–10", "p. 5", "pages 3-7".
    private static final Pattern CONTAINS_PAGES =
            Pattern.compile("(?i)\\b(pp?\\.?|pages?)\\s*[0-9]+");

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
