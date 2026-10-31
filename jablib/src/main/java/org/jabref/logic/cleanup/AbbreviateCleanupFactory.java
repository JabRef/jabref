package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.JournalMode;
import org.jabref.model.database.BibDatabase;

import org.jspecify.annotations.NullMarked;

// [impl->feat~fetchers.journal-title-automatic-abbreviation~1]
@NullMarked
public final class AbbreviateCleanupFactory {

    private AbbreviateCleanupFactory() {
    }

    public static Optional<CleanupJob> abbreviateCleanup(Optional<BibDatabase> database, JournalAbbreviationRepository repository, AbbreviationPreferences abbreviationPreferences) {
        JournalMode journalMode = abbreviationPreferences.useJournalAbbreviationMode();

        if (journalMode == JournalMode.NONE) {
            return Optional.empty();
        }

        AbbreviationType abbreviationType = abbreviationPreferences.useAbbreviationTypeOnFetch();

        if (journalMode == JournalMode.ABBREVIATE) {
            return database
                    .<CleanupJob>map(db -> new AbbreviateJournalCleanup(
                            db, repository, abbreviationType,
                            abbreviationPreferences.shouldUseFJournalField()))
                    .or(() -> Optional.of(new AbbreviateJournalCleanup(
                            repository, abbreviationType,
                            abbreviationPreferences.shouldUseFJournalField())));
        }

        return database.<CleanupJob>map(db -> new UnabbreviateJournalCleanup(db, repository))
                       .or(() -> Optional.of(new UnabbreviateJournalCleanup(repository)));
    }
}
