package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.ISBN;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;


public class ISBNChecker implements Checker {


    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (!entry.hasField(FieldName.ISBN)) {
            return Collections.emptyList();
        }

        // Check that the ISBN is on the correct form
        ISBN isbn = new ISBN(entry.getField(FieldName.ISBN).get());

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
