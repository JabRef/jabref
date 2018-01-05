package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.integrity.IntegrityCheck.Checker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class BibtexKeyDuplicationChecker implements Checker {

    private final BibDatabase database;

    public BibtexKeyDuplicationChecker(BibDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        boolean isDuplicate = database.getDuplicationChecker().isDuplicateCiteKeyExisting(entry);
        if (isDuplicate) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("Duplicate BibTeX key"), entry, FieldName.KEY));
        }
        return Collections.emptyList();
    }
}
