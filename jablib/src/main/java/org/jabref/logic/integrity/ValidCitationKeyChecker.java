package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

/// Makes sure the key is legal
public class ValidCitationKeyChecker implements ValueChecker {
    private final String unwantedCharacters;

    public ValidCitationKeyChecker(String unwantedCharacters) {
        this.unwantedCharacters = unwantedCharacters;
    }

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return Optional.of(Localization.lang("empty citation key"));
        }

        String cleaned = CitationKeyGenerator.removeUnwantedCharactersWithKeepDiacritics(value, unwantedCharacters);

        if (cleaned.equals(value)) {
            return Optional.empty();
        } else {
            return Optional.of(Localization.lang("Invalid citation key"));
        }
    }
}
