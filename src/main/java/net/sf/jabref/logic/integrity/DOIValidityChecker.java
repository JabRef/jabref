package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class DOIValidityChecker implements Checker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        final String field = FieldName.DOI;
        return entry.getField(field)
                .filter(d -> !DOI.isValid(d))
                .map(d -> Collections.singletonList(new IntegrityMessage(Localization.lang("DOI %0 is invalid", d), entry, field)))
                .orElse(Collections.emptyList());
    }
}
