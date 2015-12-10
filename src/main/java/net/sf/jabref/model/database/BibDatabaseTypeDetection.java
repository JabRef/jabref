package net.sf.jabref.model.database;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BibDatabaseTypeDetection {
    private static final List<EntryType> bibtex = BibtexEntryTypes.ALL;
    private static final List<EntryType> biblatex = BibLatexEntryTypes.ALL;
    private static final List<EntryType> exclusiveBiblatex = filterEntryTypes(biblatex, isNotIncludedIn(bibtex));

    /**
     * Tries to infer the database type by examining a BibEntry collection.
     *
     * All checks are based on the case-insensitive comparison of entry tag names.
     * Only standard BibTex and Biblatex entry types are considered in the decision process.
     *
     * 1. Check if any of the entries is a type exclusive to Biblatex
     * 2. Check if all entries belong to the standard BibTex set
     * 2.1 Check if any Biblatex fields are present
     * 3. Otherwise return BibTex
     *
     * @param entries a BibEntry collection
     * @return the inferred database type
     */
    public static BibDatabaseType inferType(Collection<BibEntry> entries) {
        final List<EntryType> entryTypes = getEntryTypes(entries);

        // type-based check
        if (entryTypes.stream().anyMatch(isIncludedIn(exclusiveBiblatex))) {
            return BibDatabaseType.BIBLATEX;
        } else if (entryTypes.stream().allMatch(isIncludedIn(bibtex))) {
            // field-based check
            if(entries.stream().anyMatch(hasBiblatexFields())) {
                return BibDatabaseType.BIBLATEX;
            }
        }
        return BibDatabaseType.BIBTEX;
    }

    private static List<String> exclusiveBiblatexFields(EntryType type) {
        final Optional<EntryType> biblatexType = BibLatexEntryTypes.getType(type.getName());
        final Optional<EntryType> bibtexType = BibtexEntryTypes.getType(type.getName());

        if (!biblatexType.isPresent() || !bibtexType.isPresent()) {
            return new ArrayList<>(0);
        }

        final List<String> bibtexFields = bibtexType.get().getAllFields();
        final List<String> biblatexFields = biblatexType.get().getAllFields();

        return biblatexFields.stream().filter(f -> !bibtexFields.contains(f)).collect(Collectors.toList());
    }

    private static List<EntryType> getEntryTypes(Collection<BibEntry> collection) {
        return collection.stream().map(BibEntry::getType).collect(Collectors.toList());
    }

    private static List<EntryType> filterEntryTypes(List<EntryType> types, Predicate<EntryType> predicate) {
        return types.stream().filter(predicate).collect(Collectors.toList());
    }

    private static Predicate<EntryType> isNotIncludedIn(List<EntryType> collection) {
        return entry -> collection.stream().noneMatch(c -> c.getName().equalsIgnoreCase(entry.getName()));
    }

    private static Predicate<EntryType> isIncludedIn(List<EntryType> collection) {
        return entry -> collection.stream().anyMatch(c -> c.getName().equalsIgnoreCase(entry.getName()));
    }

    private static Predicate<BibEntry> hasBiblatexFields() {
        return e -> e.getFieldNames().stream()
                .anyMatch(name -> exclusiveBiblatexFields(e.getType()).stream().anyMatch(c -> c.equalsIgnoreCase(name)));
    }
}
