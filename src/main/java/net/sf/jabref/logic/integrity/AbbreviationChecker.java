package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

public class AbbreviationChecker extends FieldChecker {


    public AbbreviationChecker(String field) {
        super(field);
    }

    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        if (value.contains(".")) {
            return Collections
                    .singletonList(new IntegrityMessage(Localization.lang("abbreviation detected"), entry, field));
        }

        return Collections.emptyList();
    }
}
