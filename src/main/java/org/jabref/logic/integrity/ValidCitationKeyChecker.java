package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.model.strings.StringUtil;

/**
 * Makes sure the key is legal
 */
public class ValidCitationKeyChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return Optional.of(IntegrityIssue.EMPTY_CITATION_KEY.getText());
        }

        String cleaned = CitationKeyGenerator.cleanKey(value, "");

        if (cleaned.equals(value)) {
            return Optional.empty();
        } else {
            return Optional.of(IntegrityIssue.INVALID_CITATION_KEY.getText());
        }
    }
}
