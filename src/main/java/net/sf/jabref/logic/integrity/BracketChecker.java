package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

public class BracketChecker extends FieldChecker {

    public BracketChecker(String field) {
        super(field);
    }

    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        // metaphor: integer-based stack (push + / pop -)
        int counter = 0;
        for (char a : value.trim().toCharArray()) {
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
