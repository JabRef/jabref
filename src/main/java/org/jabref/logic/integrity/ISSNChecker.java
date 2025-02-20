package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.gui.integrity.IntegrityIssue;
import org.jabref.model.entry.identifier.ISSN;
import org.jabref.model.strings.StringUtil;

public class ISSNChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        // Check that the ISSN is on the correct form
        String issnString = value.trim();

        ISSN issn = new ISSN(issnString);
        if (!issn.isValidFormat()) {
            return Optional.of(IntegrityIssue.INCORRECT_FORMAT_ISSN.getText());
        }

        if (!issn.isValidChecksum()) {
            return Optional.of(IntegrityIssue.INCORRECT_CONTROL_DIGIT_ISSN.getText());
        }
        return Optional.empty();
    }
}
