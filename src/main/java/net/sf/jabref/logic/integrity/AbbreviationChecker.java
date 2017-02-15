package net.sf.jabref.logic.integrity;

import java.util.Optional;

import net.sf.jabref.logic.l10n.Localization;

public class AbbreviationChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (value.contains(".")) {
            return Optional.of(Localization.lang("abbreviation detected"));
        }

        return Optional.empty();
    }
}
