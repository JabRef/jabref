package net.sf.jabref.logic.integrity;

import java.util.Optional;

import net.sf.jabref.logic.l10n.Localization;

public class BracketChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
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
