package org.jabref.logic.integrity;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldWriter;

public class BracketChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        List<String> errors = FieldWriter.checkBalancedBraces(value);
        if (!errors.isEmpty()) {
            return Optional.of(String.join("\n", errors));
        }

        return Optional.empty();
    }
}
