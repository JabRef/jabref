package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.ISSN;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;


public class ISSNChecker extends FieldChecker {

    public ISSNChecker() {
        super(FieldName.ISSN);
    }

    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        // Check that the ISSN is on the correct form
        String issnString = value.trim();

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
