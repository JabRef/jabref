package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.ISSN;

public class ISSNChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        // Check that the ISSN is on the correct form
        String issnString = value.trim();

        ISSN issn = new ISSN(issnString);
        if (!issn.isValidFormat()) {
            return Optional.of(Localization.lang("incorrect format"));
        }

        if (issn.isValidChecksum()) {
            return Optional.empty();
        } else {
            return Optional.of(Localization.lang("incorrect control digit"));
        }
    }
}
