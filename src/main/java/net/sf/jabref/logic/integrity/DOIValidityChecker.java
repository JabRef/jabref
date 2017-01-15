package net.sf.jabref.logic.integrity;

import java.util.Optional;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.DOI;

public class DOIValidityChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (DOI.isValid(value)) {
            return Optional.empty();
        } else {
            return Optional.of(Localization.lang("DOI %0 is invalid", value));
        }
    }
}
