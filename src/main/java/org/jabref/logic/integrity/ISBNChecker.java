package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.strings.StringUtil;

public class ISBNChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        // Check that the ISBN is on the correct form
        ISBN isbn = new ISBN(value);

        if (!isbn.isValidFormat()) {
            return Optional.of(IntegrityIssue.INCORRECT_FORMAT.getText());
        }

        if (!isbn.isValidChecksum()) {
            return Optional.of(IntegrityIssue.INCORRECT_CONTROL_DIGIT.getText());
        }

        return Optional.empty();
    }
}
