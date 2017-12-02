package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.Globals;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

/**
 * Makes sure the key is legal
 */
public class ValidBibtexKeyChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        String cleaned = BibtexKeyPatternUtil.checkLegalKey(value, Globals.prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
        if ((cleaned == null) || cleaned.equals(value)) {
            return Optional.empty();
        } else {
            return Optional.of(Localization.lang("Invalid BibTeX key"));
        }
    }
}
