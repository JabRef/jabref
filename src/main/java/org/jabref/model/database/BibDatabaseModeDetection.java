package org.jabref.model.database;

import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;

public class BibDatabaseModeDetection {

    private BibDatabaseModeDetection() {
    }

    /**
     * Tries to infer the database type by examining a BibDatabase database.
     *
     * All checks are based on the case-insensitive comparison of entry tag names.
     * Only standard BibTex and Biblatex entry types are considered in the decision process.
     *
     * 1. Check if any of the entries is a type exclusive to Biblatex
     * 2. Otherwise return BibTex
     *
     * @param database a BibDatabase database
     * @return the inferred database type
     */
    public static BibDatabaseMode inferMode(BibDatabase database) {
        final Stream<EntryType> entryTypes = database.getEntries().stream().map(BibEntry::getType);

        if (entryTypes.anyMatch(EntryTypeFactory::isExclusiveBiblatex)) {
            return BibDatabaseMode.BIBLATEX;
        } else {
            return BibDatabaseMode.BIBTEX;
        }
    }
}
