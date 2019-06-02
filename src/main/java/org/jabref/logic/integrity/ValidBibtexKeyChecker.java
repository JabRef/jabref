package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.l10n.Localization;

/**
 * Makes sure the key is legal
 */
public class ValidBibtexKeyChecker implements ValueChecker {

    private final boolean enforceLegalKey;

    public ValidBibtexKeyChecker(boolean enforceLegalKey) {
        this.enforceLegalKey = enforceLegalKey;
    }

    @Override
    public Optional<String> checkValue(String value) {

        // Fix #2: BibtexKeyGenerator.cleanKey() does not accept a null value
        // for the "value" parameter. The 'if' statement below is added to check for
        // both a null and zero length string and to add the empty key warning.
        if ((value == null) || (value.length() == 0)) {
            return Optional.of(Localization.lang("empty BibTeX key"));
        }

        String cleaned = BibtexKeyGenerator.cleanKey(value, enforceLegalKey);

        if (cleaned.equals(value)) {
            return Optional.empty();
        } else {
            return Optional.of(Localization.lang("Invalid BibTeX key"));
        }
    }
}
