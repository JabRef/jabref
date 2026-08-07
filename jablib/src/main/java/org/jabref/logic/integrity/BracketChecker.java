package org.jabref.logic.integrity;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.Nullable;

public class BracketChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(@Nullable String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }
        List<String> errors = FieldWriter.checkBalancedBraces(value);
        if (!errors.isEmpty()) {
            return Optional.of(String.join("\n", errors));
        }

        return Optional.empty();
    }
}
