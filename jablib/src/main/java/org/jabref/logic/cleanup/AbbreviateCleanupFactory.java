package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.JournalMode;
import org.jabref.model.database.BibDatabase;

public class AbbreviateCleanupFactory {
    private AbbreviateCleanupFactory() {
    }

    public static Optional<CleanupJob> abbreviateCleanup(BibDatabase database,
                                                         JournalAbbreviationRepository repository,
                                                         AbbreviationPreferences abbreviationPreferences) {
        if (abbreviationPreferences == null || repository == null) {
            return Optional.empty();
        }

        JournalMode journalMode = abbreviationPreferences.getJournalMode();
        AbbreviationType abbreviationType = abbreviationPreferences.getAbbreviationType();
        if (journalMode == JournalMode.NONE)
            return Optional.empty();

        if (journalMode == JournalMode.ABBREVIATE) {
            return Optional.of(new AbbreviateJournalCleanup(database, repository, abbreviationType,
                    abbreviationPreferences.shouldUseFJournalField()));
        }
        return Optional.of(new UnabbreviateJournalCleanup(database, repository));
    }
}
