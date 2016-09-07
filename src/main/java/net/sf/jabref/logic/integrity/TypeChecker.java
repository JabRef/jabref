package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class TypeChecker implements Checker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(FieldName.PAGES);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        if ("proceedings".equalsIgnoreCase(entry.getType())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("wrong entry type as proceedings has page numbers"), entry, FieldName.PAGES));
        }

        return Collections.emptyList();
    }
}
