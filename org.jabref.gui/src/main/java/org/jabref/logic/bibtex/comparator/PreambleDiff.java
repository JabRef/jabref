package org.jabref.logic.bibtex.comparator;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

public class PreambleDiff {

    private String newPreamble;

    private PreambleDiff(String newPreamble) {
        this.newPreamble = newPreamble;
    }

    public static Optional<PreambleDiff> compare(BibDatabaseContext originalDatabase, BibDatabaseContext newDatabase) {
        Optional<String> originalPreamble = originalDatabase.getDatabase().getPreamble();
        Optional<String> newPreamble = newDatabase.getDatabase().getPreamble();
        if (originalPreamble.equals(newPreamble)) {
            return Optional.empty();
        } else {
            return Optional.of(new PreambleDiff(newPreamble.orElse("")));
        }
    }

    public String getNewPreamble() {
        return newPreamble;
    }
}
