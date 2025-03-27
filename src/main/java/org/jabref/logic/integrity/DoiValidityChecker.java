package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.strings.StringUtil;

public class DoiValidityChecker implements ValueChecker {
    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (DOI.isValid(value)) {
            return Optional.empty();
        } else {
            return Optional.of(IntegrityIssue.DOI_IS_INVALID.getText());
        }
    }
}
