package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

/// Makes sure the key is legal
public class ValidCitationKeyChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return Optional.of(Localization.lang("empty citation key"));
        }

        boolean hasIllegalChar = value.chars().anyMatch(c ->
                Character.isWhitespace(c) || CitationKeyGenerator.DISALLOWED_CHARACTERS.contains((char) c)
        );

        if (hasIllegalChar) {
            return Optional.of(Localization.lang("Invalid citation key"));
        }

        return Optional.empty();
    }
}
