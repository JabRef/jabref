package org.jabref.logic.integrity;

import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;

public class BooktitleChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (value.toLowerCase(Locale.ENGLISH).endsWith("conference on")) {
            return Optional.of(Localization.lang("booktitle ends with 'conference on'"));
        }

        return Optional.empty();
    }
}
