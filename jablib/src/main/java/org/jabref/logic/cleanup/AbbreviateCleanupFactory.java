package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.JournalMode;
import org.jabref.model.database.BibDatabase;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AbbreviateCleanupFactory {

    private AbbreviateCleanupFactory() {
    }

    public static Optional<CleanupJob> abbreviateCleanup(@NonNull JournalAbbreviationRepository repository,
                                                         @NonNull AbbreviationPreferences abbreviationPreferences) {
        return abbreviateCleanup(null, repository, abbreviationPreferences);
    }

    public static Optional<CleanupJob> abbreviateCleanup(@Nullable BibDatabase database,
                                                         @NonNull JournalAbbreviationRepository repository,
                                                         @NonNull AbbreviationPreferences abbreviationPreferences) {
        JournalMode journalMode = abbreviationPreferences.useJournalAbbreviationMode();
        if (journalMode == JournalMode.NONE) {
            return Optional.empty();
        }

        AbbreviationType abbreviationType = abbreviationPreferences.useAbbreviationTypeOnFetch();

        if (journalMode == JournalMode.ABBREVIATE) {
            return Optional.of(Optional.ofNullable(database).<CleanupJob>map(db -> new AbbreviateJournalCleanup(
                                               db, repository, abbreviationType,
                                               abbreviationPreferences.shouldUseFJournalField()))
                                       .orElseGet(() -> new AbbreviateJournalCleanup(
                                               repository, abbreviationType,
                                               abbreviationPreferences.shouldUseFJournalField())));
        }
        return Optional.of(Optional.ofNullable(database).<CleanupJob>map(db -> new UnabbreviateJournalCleanup(db, repository))
                                   .orElseGet(() -> new UnabbreviateJournalCleanup(repository)));
    }
}
