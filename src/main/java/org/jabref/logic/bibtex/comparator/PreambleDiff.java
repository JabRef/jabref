package org.jabref.logic.bibtex.comparator;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

public class PreambleDiff {

    private final String originalPreamble;
    private final String newPreamble;

    private PreambleDiff(String originalPreamble, String newPreamble) {
        this.originalPreamble = originalPreamble;
        this.newPreamble = newPreamble;
    }

    public static Optional<PreambleDiff> compare(BibDatabaseContext originalDatabase, BibDatabaseContext newDatabase) {
        Optional<String> originalPreamble = originalDatabase.getDatabase().getPreamble();
        Optional<String> newPreamble = newDatabase.getDatabase().getPreamble();
        if (originalPreamble.equals(newPreamble)) {
            return Optional.empty();
        } else {
            return Optional.of(new PreambleDiff(originalPreamble.orElse(""), newPreamble.orElse("")));
        }
    }

    public String getNewPreamble() {
        return newPreamble;
    }

    public String getOriginalPreamble() {
        return originalPreamble;
    }
}
