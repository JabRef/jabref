package org.jabref.logic.integrity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class CitationKeyDuplicationChecker implements EntryChecker {

    private final BibDatabase database;

    public CitationKeyDuplicationChecker(BibDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> citeKey = entry.getCitationKey();
        if (citeKey.isEmpty()) {
            return List.of();
        }

        boolean isDuplicate = database.isDuplicateCitationKeyExisting(citeKey.get());
        if (isDuplicate) {
            return List.of(
                    new IntegrityMessage(Localization.lang("Duplicate citation key"), entry, StandardField.KEY));
        }
        return List.of();
    }
}
