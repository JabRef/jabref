package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.ISBN;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;


public class ISBNChecker extends FieldChecker {


    public ISBNChecker() {
        super(FieldName.ISBN);
    }

    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        // Check that the ISBN is on the correct form
        ISBN isbn = new ISBN(value);

        if (!isbn.isValidFormat()) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("incorrect format"), entry, FieldName.ISBN));
        }

        if (!isbn.isValidChecksum()) {
            return Collections
                    .singletonList(new IntegrityMessage(Localization.lang("incorrect control digit"), entry, FieldName.ISBN));
        }

        return Collections.emptyList();
    }
}
