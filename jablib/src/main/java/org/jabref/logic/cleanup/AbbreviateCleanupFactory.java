package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.JournalMode;
import org.jabref.model.database.BibDatabase;

import jakarta.annotation.Nullable;

public final class AbbreviateCleanupFactory {

    private AbbreviateCleanupFactory() {
    }

    public static Optional<CleanupJob> abbreviateCleanup(JournalAbbreviationRepository repository, AbbreviationPreferences abbreviationPreferences) {
        return abbreviateCleanup(null, repository, abbreviationPreferences);
    }

    public static Optional<CleanupJob> abbreviateCleanup(@Nullable BibDatabase database,
                                                         JournalAbbreviationRepository repository,
                                                         AbbreviationPreferences abbreviationPreferences) {
        if (repository == null || abbreviationPreferences == null) {
            return Optional.empty();
        }

        JournalMode journalMode = abbreviationPreferences.useJournalAbbreviationMode();
        if (journalMode == JournalMode.NONE) {
            return Optional.empty();
        }

        AbbreviationType abbreviationType = abbreviationPreferences.useAbbreviationTypeOnFetch();

        if (journalMode == JournalMode.ABBREVIATE) {
            CleanupJob cleanupJob = database == null ? new AbbreviateJournalCleanup(repository, abbreviationType, abbreviationPreferences.shouldUseFJournalField())
                                                     : new AbbreviateJournalCleanup(database, repository, abbreviationType, abbreviationPreferences.shouldUseFJournalField());
            return Optional.of(cleanupJob);
        }

        CleanupJob cleanupJob = database == null ? new UnabbreviateJournalCleanup(repository) : new UnabbreviateJournalCleanup(database, repository);
        return Optional.of(cleanupJob);
    }
}
