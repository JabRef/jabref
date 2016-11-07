package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.ISSN;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;


public class ISSNChecker implements Checker {


    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (!entry.hasField(FieldName.ISSN)) {
            return Collections.emptyList();
        }

        // Check that the ISSN is on the correct form
        String issnString = entry.getField(FieldName.ISSN).get().trim();

        ISSN issn = new ISSN(issnString);
        if (!issn.isValidFormat()) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("incorrect format"), entry, FieldName.ISSN));
        }

        if (issn.isValidChecksum()) {
            return Collections.emptyList();
        } else {
            return Collections
                    .singletonList(new IntegrityMessage(Localization.lang("incorrect control digit"), entry, FieldName.ISSN));
        }
    }

}
