package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BibDatabaseModeDetection {
    private static final List<EntryType> bibtex = BibtexEntryTypes.ALL;
    private static final List<EntryType> biblatex = BibLatexEntryTypes.ALL;
    private static final List<EntryType> exclusiveBiblatex = filterEntryTypes(biblatex, isNotIncludedIn(bibtex));

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
        final List<String> entryTypes = getEntryTypes(database.getEntries());

        // type-based check
        if (entryTypes.stream().anyMatch(isIncludedIn(exclusiveBiblatex))) {
            return BibDatabaseMode.BIBLATEX;
        } else {
            // field-based check
            return BibDatabaseMode.BIBTEX;
        }
    }

    private static List<String> getEntryTypes(Collection<BibEntry> collection) {
        return collection.stream().map(BibEntry::getType).collect(Collectors.toList());
    }

    private static List<EntryType> filterEntryTypes(List<EntryType> types, Predicate<EntryType> predicate) {
        return types.stream().filter(predicate).collect(Collectors.toList());
    }

    private static Predicate<EntryType> isNotIncludedIn(List<EntryType> collection) {
        return entry -> collection.stream().noneMatch(c -> c.getName().equalsIgnoreCase(entry.getName()));
    }

    private static Predicate<String> isIncludedIn(List<EntryType> collection) {
        return entry -> collection.stream().anyMatch(c -> c.getName().equalsIgnoreCase(entry));
    }

}
