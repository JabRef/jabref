package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

public class TypeChecker implements EntryChecker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(StandardField.PAGES);
        if (value.isEmpty()) {
            return Collections.emptyList();
        }

        if (StandardEntryType.Proceedings.equals(entry.getType())) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("wrong entry type as proceedings has page numbers"), entry, StandardField.PAGES));
        }

        return Collections.emptyList();
    }
}
