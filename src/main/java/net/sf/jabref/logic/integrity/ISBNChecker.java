package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.ISBN;
import net.sf.jabref.model.entry.BibEntry;


public class ISBNChecker implements Checker {


    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (!entry.hasField("isbn")) {
            return Collections.emptyList();
        }

        // Check that the ISBN is on the correct form
        ISBN isbn = new ISBN(entry.getFieldOptional("isbn").get());

        if (!isbn.isValidFormat()) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("incorrect format"), entry, "isbn"));
        }

        if (!isbn.isValidChecksum()) {
            return Collections
                    .singletonList(new IntegrityMessage(Localization.lang("incorrect control digit"), entry, "isbn"));
        }

        return Collections.emptyList();
    }

}
