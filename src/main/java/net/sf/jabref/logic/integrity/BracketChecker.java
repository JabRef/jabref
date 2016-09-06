package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

public class BracketChecker implements Checker {

    private final String field;


    public BracketChecker(String field) {
        this.field = field;
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(field);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        // metaphor: integer-based stack (push + / pop -)
        int counter = 0;
        for (char a : value.get().trim().toCharArray()) {
            if (a == '{') {
                counter++;
            } else if (a == '}') {
                if (counter == 0) {
                    return Collections.singletonList(
                            new IntegrityMessage(Localization.lang("unexpected closing curly bracket"), entry, field));
                } else {
                    counter--;
                }
            }
        }

        if (counter > 0) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("unexpected opening curly bracket"), entry, field));
        }

        return Collections.emptyList();
    }

}
