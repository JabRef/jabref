package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

/**
 * Makes sure the key is legal
 */
public class ValidCitationKeyChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return Optional.of(Localization.lang("empty citation key"));
        }

        String cleaned = CitationKeyGenerator.cleanKey(value, "");

        if (cleaned.equals(value)) {
            return Optional.empty();
        } else {
            return Optional.of(Localization.lang("Invalid citation key"));
        }
    }
}
