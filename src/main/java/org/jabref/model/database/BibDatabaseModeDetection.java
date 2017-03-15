package org.jabref.model.database;

import java.util.Locale;
import java.util.stream.Stream;

import org.jabref.model.EntryTypes;
import org.jabref.model.entry.BibEntry;

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
     * 2. Check if any exclusive Biblatex fields are present
     * 3. Otherwise return BibTex
     *
     * @param database a BibDatabase database
     * @return the inferred database type
     */
    public static BibDatabaseMode inferMode(BibDatabase database) {
        final Stream<String> entryTypes = database.getEntries().stream().map(BibEntry::getType);

        // type-based check
        if (entryTypes.anyMatch(type -> EntryTypes.isExclusiveBiblatex(type.toLowerCase(Locale.ENGLISH)))) {
            return BibDatabaseMode.BIBLATEX;
        } else {
            // field-based check
            return BibDatabaseMode.BIBTEX;
        }
    }
}
