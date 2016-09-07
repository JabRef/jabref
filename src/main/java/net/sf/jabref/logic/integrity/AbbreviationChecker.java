package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

public class AbbreviationChecker implements Checker {

    private final String field;


    public AbbreviationChecker(String field) {
        this.field = field;
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(field);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        if (value.get().contains(".")) {
            return Collections
                    .singletonList(new IntegrityMessage(Localization.lang("abbreviation detected"), entry, field));
        }

        return Collections.emptyList();
    }
}
