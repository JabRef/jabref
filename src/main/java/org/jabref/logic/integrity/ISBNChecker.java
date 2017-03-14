package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.identifier.ISBN;
import org.jabref.logic.l10n.Localization;


public class ISBNChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        // Check that the ISBN is on the correct form
        ISBN isbn = new ISBN(value);

        if (!isbn.isValidFormat()) {
            return Optional.of(Localization.lang("incorrect format"));
        }

        if (!isbn.isValidChecksum()) {
            return Optional.of(Localization.lang("incorrect control digit"));
        }

        return Optional.empty();
    }
}
