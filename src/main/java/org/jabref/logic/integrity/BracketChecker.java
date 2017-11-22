package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class BracketChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        // metaphor: integer-based stack (push + / pop -)
        int counter = 0;
        for (char a : value.trim().toCharArray()) {
            if (a == '{') {
                counter++;
            } else if (a == '}') {
                if (counter == 0) {
                    return Optional.of(Localization.lang("unexpected closing curly bracket"));
                } else {
                    counter--;
                }
            }
        }

        if (counter > 0) {
            return Optional.of(Localization.lang("unexpected opening curly bracket"));
        }

        return Optional.empty();
    }
}
