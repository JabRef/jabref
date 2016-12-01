package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class DOIValidityChecker implements Checker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        String field = FieldName.DOI;
        Optional<String> value = entry.getField(field);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        final boolean isDOIValid = value.flatMap(v -> DOI.build(v)).isPresent();
        if (!isDOIValid) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("DOI is invalid"), entry, field));
        }

        return Collections.emptyList();
    }
}
