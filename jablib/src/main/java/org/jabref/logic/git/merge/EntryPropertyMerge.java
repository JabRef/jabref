package org.jabref.logic.git.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.merge.planning.util.EntryTriples;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Three-way merge of the entry properties the field-level merge does not look at: the rules of
/// [org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer] compare field maps, and a
/// [org.jabref.logic.git.model.MergePlan] carries field values only. The entry type and the
/// comment above an entry follow the same rules here: a value changed in `other` alone is taken
/// over, a value changed differently on both sides is a conflict, and so is deleting an entry
/// on one side while changing a property on the other.
@NullMarked
final class EntryPropertyMerge {

    private static final List<EntryProperty<?>> PROPERTIES = List.of(
            new EntryProperty<>(BibEntry::getType, BibEntry::setType),
            new EntryProperty<>(BibEntry::getUserComments, EntryPropertyMerge::setUserComments));

    private EntryPropertyMerge() {
    }

    /// Applies the property changes of `other` to the entries of `current`.
    ///
    /// @param keptAsIs citation keys of entries already known to be in conflict; they are left untouched
    /// @return the conflicts found, none of them among `keptAsIs`
    static List<ThreeWayEntryConflict> merge(BibDatabaseContext base, BibDatabaseContext current, BibDatabaseContext other, Set<String> keptAsIs) {
        EntryTriples triples = EntryTriples.from(base, current, other);
        List<ThreeWayEntryConflict> conflicts = new ArrayList<>();
        for (String key : triples.allKeys()) {
            if (keptAsIs.contains(key)) {
                continue;
            }
            @Nullable BibEntry baseEntry = triples.baseMap.get(key);
            @Nullable BibEntry currentEntry = triples.localMap.get(key);
            @Nullable BibEntry otherEntry = triples.remoteMap.get(key);
            if (isConflict(baseEntry, currentEntry, otherEntry)) {
                conflicts.add(new ThreeWayEntryConflict(baseEntry, currentEntry, otherEntry));
            } else if ((baseEntry != null) && (currentEntry != null) && (otherEntry != null)) {
                PROPERTIES.forEach(property -> property.takeChangeFromOther(baseEntry, currentEntry, otherEntry));
            }
        }
        return conflicts;
    }

    private static boolean isConflict(@Nullable BibEntry base, @Nullable BibEntry current, @Nullable BibEntry other) {
        if (base == null) {
            // Added on both sides: the field-level merge unions the fields, but a type or comment cannot be unioned
            return (current != null) && (other != null) && differ(current, other);
        }
        if (current == null) {
            return (other != null) && differ(base, other);
        }
        if (other == null) {
            return differ(base, current);
        }
        return PROPERTIES.stream().anyMatch(property -> property.diverged(base, current, other));
    }

    private static boolean differ(BibEntry one, BibEntry two) {
        return PROPERTIES.stream().anyMatch(property -> property.differs(one, two));
    }

    private static void setUserComments(BibEntry entry, String userComments) {
        entry.setCommentsBeforeEntry(userComments);
        // An unchanged entry is written from its parsed serialization, which still holds the old comment
        entry.setChanged(true);
    }

    private record EntryProperty<T>(Function<BibEntry, T> getter, BiConsumer<BibEntry, T> setter) {
        boolean differs(BibEntry one, BibEntry two) {
            return !getter.apply(one).equals(getter.apply(two));
        }

        boolean diverged(BibEntry base, BibEntry current, BibEntry other) {
            return differs(base, current) && differs(base, other) && differs(current, other);
        }

        void takeChangeFromOther(BibEntry base, BibEntry current, BibEntry other) {
            if (!differs(base, current) && differs(base, other)) {
                setter.accept(current, getter.apply(other));
            }
        }
    }
}
