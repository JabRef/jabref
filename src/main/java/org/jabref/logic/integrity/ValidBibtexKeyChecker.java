package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
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
        String cleaned = BibtexKeyPatternUtil.checkLegalKey(value, enforceLegalKey);
        if ((cleaned == null) || cleaned.equals(value)) {
            return Optional.empty();
        } else {
            return Optional.of(Localization.lang("Invalid BibTeX key"));
        }
    }
}
