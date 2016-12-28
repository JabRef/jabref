package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class DOIValidityChecker extends FieldChecker {

    public DOIValidityChecker() {
        super(FieldName.DOI);
    }

    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        if (DOI.isValid(value)) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(new IntegrityMessage(Localization.lang("DOI %0 is invalid", value), entry, field));
        }
    }
}
