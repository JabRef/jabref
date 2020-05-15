package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class BibtexKeyDuplicationChecker implements EntryChecker {

    private final BibDatabase database;

    public BibtexKeyDuplicationChecker(BibDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> citeKey = entry.getCiteKeyOptional();
        if (citeKey.isEmpty()) {
            return Collections.emptyList();
        }

        boolean isDuplicate = database.isDuplicateCiteKeyExisting(citeKey.get());
        if (isDuplicate) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("Duplicate BibTeX key"), entry, StandardField.KEY));
        }
        return Collections.emptyList();
    }
}
