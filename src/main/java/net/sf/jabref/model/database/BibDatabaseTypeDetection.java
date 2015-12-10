package net.sf.jabref.model.database;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BibDatabaseTypeDetection {
    /**
     * Tries to infer the database type by examining a BibEntry collection.
     *
     * All checks are based on the case-insensitive comparison of entry tag names.
     *
     * 1. Check if any of the entries is a type exclusive to Biblatex
     * 2. Check if all entries belong to the standard BibTex set
     * 2.1 Check if any Biblatex fields are present
     * 3. Otherwise return BibTex
     *
     * @param entries a BibEntry collection
     * @return the infered database type
     */
    // TODO: what about custom entry types?, Define type detection on type field in jabref bib file?
    public static BibDatabaseType inferType(Collection<BibEntry> entries) {
        final List<EntryType> bibtex = BibtexEntryTypes.ALL;
        final List<EntryType> biblatex = BibLatexEntryTypes.ALL;
        final List<EntryType> exclusiveBiblatex = filterEntryTypes(biblatex, isNotIncludedIn(bibtex));
        final List<EntryType> entryTypes = getEntryTypes(entries);

        // type-based check
        if(entryTypes.stream().anyMatch(isIncludedIn(exclusiveBiblatex))) {
            return BibDatabaseType.BIBLATEX;
        } else if(entryTypes.stream().allMatch(isIncludedIn(bibtex))) {
            // field-based check
        }
        return BibDatabaseType.BIBTEX;
    }

    private static List<EntryType> getEntryTypes(Collection<BibEntry> collection) {
        return collection.stream().map(BibEntry::getType).collect(Collectors.toList());
    }

    private static List<EntryType> filterEntryTypes(List<EntryType> types, Predicate<EntryType> predicate) {
        return types.stream().filter(predicate).collect(Collectors.<EntryType>toList());
    }

    private static Predicate<EntryType> isNotIncludedIn(List<EntryType> collection) {
        return entry -> collection.stream().noneMatch(c -> c.getName().equalsIgnoreCase(entry.getName()));
    }

    private static Predicate<EntryType> isIncludedIn(List<EntryType> collection) {
        return entry -> collection.stream().anyMatch(c -> c.getName().equalsIgnoreCase(entry.getName()));
    }
}
