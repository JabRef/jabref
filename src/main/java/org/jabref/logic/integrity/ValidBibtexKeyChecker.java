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
        // for the "value" parameter. The if statement below changes a null value
        // to a blank string
        String testValue;
        if (value == null) {
            testValue = "";
        } else {
            testValue = value;
        }

        String cleaned = BibtexKeyGenerator.cleanKey(testValue, enforceLegalKey);

        // Fix #3: added a test case that would take care of cases when the parameter
        // "value" was either null or blank
        if (testValue.length() == 0) {
            return Optional.of(Localization.lang("empty BibTeX key"));
        } else if (cleaned.equals(testValue)) {
            return Optional.empty();
        } else {
            return Optional.of(Localization.lang("Invalid BibTeX key"));
        }
    }
}
