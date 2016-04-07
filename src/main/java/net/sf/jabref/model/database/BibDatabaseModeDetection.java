package net.sf.jabref.model.database;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.jabref.model.entry.*;

public class BibDatabaseModeDetection {
    private static final List<EntryType> bibtex = BibtexEntryTypes.ALL;
    private static final List<EntryType> biblatex = BibLatexEntryTypes.ALL;
    private static final List<String> exclusiveBiblatex = filterEntryTypesNames(biblatex, isNotIncludedIn(bibtex));

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
        if (entryTypes.anyMatch(type -> exclusiveBiblatex.contains(type.toLowerCase()))) {
            return BibDatabaseMode.BIBLATEX;
        } else {
            // field-based check
            return BibDatabaseMode.BIBTEX;
        }
    }

    private static List<String> filterEntryTypesNames(List<EntryType> types, Predicate<EntryType> predicate) {
        return types.stream().filter(predicate).map(type -> type.getName().toLowerCase()).collect(Collectors.toList());
    }

    private static Predicate<EntryType> isNotIncludedIn(List<EntryType> collection) {
        return entry -> collection.stream().noneMatch(c -> c.getName().equalsIgnoreCase(entry.getName()));
    }
}
