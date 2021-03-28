package org.jabref.gui.openoffice;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

class CitationDatabaseLookup {

    public static class Result {
        BibEntry entry;
        BibDatabase database;
        Result(BibEntry entry, BibDatabase database) {
            this.entry = entry;
            this.database = database;
        }
    }

    public static Optional<Result> lookup(List<BibDatabase> databases,
                                          String key) {
        for (BibDatabase database : databases) {
            Optional<BibEntry> entry = database.getEntryByCitationKey(key);
            if (entry.isPresent()) {
                return Optional.of(new Result(entry.get(), database));
            }
        }
        return Optional.empty();
    }

}
