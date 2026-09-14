package org.jabref.logic.sync;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.git.merge.planning.util.ConflictRules;
import org.jabref.logic.git.merge.planning.util.FieldPatchComputer;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// The library as it last matched its file on disk: the common ancestor for a three-way comparison of the in-memory
/// library with the file. For each item (entry, metadata, preamble, string) it tells which [Side] diverged, which
/// decides whether an external change can be taken over silently or needs a review, and merges entries changed on
/// both sides field by field with the Git merge rules.
///
/// Entries are keyed by the id of the in-memory entry they were taken from, so that a citation key change in memory
/// does not break the association. The metadata is kept in its serialized form, which is the only way to snapshot it.
@NullMarked
public final class LibraryBaseline {

    /// Which side diverged from the baseline. Only asked for items that differ between memory and disk, so at least
    /// one side changed.
    public enum Side {
        /// Only the file changed: the change can be taken over without asking
        DISK,
        /// Only memory changed: not an external change at all, but an unsaved edit
        MEMORY,
        /// Both changed differently: needs a review
        BOTH
    }

    private static final String ENCODING_KEY = "\0encoding";

    /// What the comparison needs of an entry; a `BibEntry` copy would carry an event bus, caches, and properties per
    /// entry, which for a large library adds up to far more than the bibliographic data itself.
    /// The comments written before the entry in the file count as content: they are kept by the parser and written
    /// back, so an external edit of them is an external change like any other.
    private record EntrySnapshot(EntryType type, Map<Field, String> fields, String comments) {
        static EntrySnapshot of(BibEntry entry) {
            return new EntrySnapshot(entry.getType(), Map.copyOf(entry.getFieldMap()), entry.getUserComments());
        }

        /// Wraps the live field map without copying, for a comparison made right away
        static EntrySnapshot view(BibEntry entry) {
            return new EntrySnapshot(entry.getType(), entry.getFieldMap(), entry.getUserComments());
        }

        Optional<String> citationKey() {
            return Optional.ofNullable(fields.get(InternalField.KEY_FIELD));
        }

        /// The entry without its citation key, for finding an entry whose key changed
        EntrySnapshot withoutKey() {
            if (!fields.containsKey(InternalField.KEY_FIELD)) {
                return this;
            }
            Map<Field, String> rest = new HashMap<>(fields);
            rest.remove(InternalField.KEY_FIELD);
            return new EntrySnapshot(type, rest, comments);
        }

        /// Only needed as the common ancestor of a field-level merge, which is the rare both-sides case
        BibEntry toEntry() {
            BibEntry entry = new BibEntry(type).withFields(fields);
            entry.setCommentsBeforeEntry(comments);
            return entry;
        }
    }

    private final Map<String, EntrySnapshot> entriesById;
    private final Map<String, String> metaData;
    private @Nullable String preamble;
    private final Map<String, String> strings;
    private final GlobalCitationKeyPatterns citationKeyPatterns;

    private LibraryBaseline(Map<String, EntrySnapshot> entriesById, Map<String, String> metaData, @Nullable String preamble, Map<String, String> strings, GlobalCitationKeyPatterns citationKeyPatterns) {
        this.entriesById = entriesById;
        this.metaData = metaData;
        this.preamble = preamble;
        this.strings = strings;
        this.citationKeyPatterns = citationKeyPatterns;
    }

    public static LibraryBaseline of(BibDatabaseContext context, GlobalCitationKeyPatterns citationKeyPatterns) {
        Map<String, EntrySnapshot> entries = new HashMap<>();
        for (BibEntry entry : context.getDatabase().getEntries()) {
            entries.put(entry.getId(), EntrySnapshot.of(entry));
        }
        Map<String, String> strings = new HashMap<>();
        for (BibtexString string : context.getDatabase().getStringValues()) {
            strings.put(string.getName(), string.getContent());
        }
        return new LibraryBaseline(entries,
                serialize(context.getMetaData(), citationKeyPatterns),
                context.getDatabase().getPreamble().orElse(null),
                strings,
                citationKeyPatterns);
    }

    /// Lookup tables over the baseline entries for disk entries without in-memory counterpart. Built once per pass
    /// over a set of changes, so that each lookup is O(1) instead of a scan over the library.
    public final class Lookup {
        private final Map<String, List<Map.Entry<String, EntrySnapshot>>> byKey = new HashMap<>();
        private final Map<EntrySnapshot, List<Map.Entry<String, EntrySnapshot>>> byContentExceptKey = new HashMap<>();

        private Lookup() {
            for (Map.Entry<String, EntrySnapshot> entry : entriesById.entrySet()) {
                entry.getValue().citationKey().ifPresent(key -> byKey.computeIfAbsent(key, _ -> new ArrayList<>()).add(entry));
                byContentExceptKey.computeIfAbsent(entry.getValue().withoutKey(), _ -> new ArrayList<>()).add(entry);
            }
        }

        /// The id of the in-memory entry the given disk entry was taken from: by citation key, or else by the
        /// remaining content, which covers entries without a key as well as a key changed on disk. Among several
        /// candidates (keys need not be unique, entries may be duplicated) the one with identical content wins;
        /// when that leaves more than one, the entry is not associated at all rather than with the wrong one.
        public Optional<String> baseIdOf(BibEntry remote) {
            return find(remote).map(Map.Entry::getKey);
        }

        /// A disk entry without in-memory counterpart is either new on disk, or was deleted in memory (and possibly
        /// modified on disk as well). Only an entry whose baseline counterpart is gone from memory counts as deleted
        /// there; a duplicate of an entry still in memory is an addition.
        ///
        /// @param existsInMemory whether the in-memory entry with the given id still exists
        public Side sideOfAddedEntry(BibEntry remote, Predicate<String> existsInMemory) {
            Optional<Map.Entry<String, EntrySnapshot>> base = find(remote).filter(found -> !existsInMemory.test(found.getKey()));
            if (base.isPresent()) {
                return base.get().getValue().equals(EntrySnapshot.view(remote)) ? Side.MEMORY : Side.BOTH;
            }
            // Neither key nor content match: an entry deleted in memory may still be the origin, with key and a
            // field changed on disk; then the deletion and the change need a review
            List<String> goneFromMemory = entriesById.keySet().stream().filter(id -> !existsInMemory.test(id)).toList();
            return closestOf(goneFromMemory, remote).isPresent() ? Side.BOTH : Side.DISK;
        }

        /// Identical content under the same key is the entry itself; identical content under another key is the entry
        /// renamed; only then does a same-key entry with other content count, and each only when unambiguous.
        private Optional<Map.Entry<String, EntrySnapshot>> find(BibEntry remote) {
            EntrySnapshot snapshot = EntrySnapshot.view(remote);
            List<Map.Entry<String, EntrySnapshot>> byKeyCandidates = snapshot.citationKey().map(key -> byKey.getOrDefault(key, List.of())).orElse(List.of());
            List<Map.Entry<String, EntrySnapshot>> identicalWithKey = byKeyCandidates.stream().filter(entry -> entry.getValue().equals(snapshot)).toList();
            if (!identicalWithKey.isEmpty()) {
                return single(identicalWithKey);
            }
            List<Map.Entry<String, EntrySnapshot>> sameContent = byContentExceptKey.getOrDefault(snapshot.withoutKey(), List.of());
            if (!sameContent.isEmpty()) {
                return single(sameContent);
            }
            return single(byKeyCandidates);
        }

        private static Optional<Map.Entry<String, EntrySnapshot>> single(List<Map.Entry<String, EntrySnapshot>> candidates) {
            return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
        }
    }

    public Lookup lookup() {
        return new Lookup();
    }

    public boolean hasEntry(String entryId) {
        return entriesById.containsKey(entryId);
    }

    /// An entry present on both sides, whose baseline is looked up by the in-memory entry's id.
    public Side sideOfEntry(BibEntry local, BibEntry remote) {
        return sideOf(entriesById.get(local.getId()), EntrySnapshot.view(local), EntrySnapshot.view(remote));
    }

    /// An in-memory entry without counterpart on disk is either deleted on disk, or new in memory (and possibly
    /// modified on disk as well).
    public Side sideOfDeletedEntry(BibEntry local) {
        EntrySnapshot base = entriesById.get(local.getId());
        if (base == null) {
            return Side.MEMORY;
        }
        return base.equals(EntrySnapshot.view(local)) ? Side.DISK : Side.BOTH;
    }

    /// Field-level merge of an entry modified on both sides: disk wins for every field memory left alone, memory wins
    /// where disk left the field alone. Empty if the same field was changed differently on both sides.
    public Optional<BibEntry> mergeEntry(BibEntry local, BibEntry remote) {
        EntrySnapshot base = entriesById.get(local.getId());
        // Without a common ancestor (entry new on both sides), an empty entry of the in-memory type makes every field
        // an addition, so only fields set differently on both sides count as conflicts; the in-memory type is kept
        BibEntry ancestor = base == null ? new BibEntry(local.getType()) : base.toEntry();
        boolean commentsChangedLocally = !ancestor.getUserComments().equals(local.getUserComments());
        boolean commentsChangedRemotely = !ancestor.getUserComments().equals(remote.getUserComments());
        boolean commentsConflict = commentsChangedLocally && commentsChangedRemotely && !local.getUserComments().equals(remote.getUserComments());
        if (commentsConflict || ConflictRules.hasConflictingFields(ancestor, local, remote)) {
            return Optional.empty();
        }
        BibEntry merged = new BibEntry(local);
        if (base != null && !ancestor.getType().equals(remote.getType())) {
            if (!ancestor.getType().equals(local.getType()) && !local.getType().equals(remote.getType())) {
                return Optional.empty();
            }
            merged.setType(remote.getType());
        }
        if (!commentsChangedLocally) {
            merged.setCommentsBeforeEntry(remote.getUserComments());
        }
        FieldPatchComputer.compute(ancestor, local, remote).forEach((field, value) -> {
            if (value == null) {
                merged.clearField(field);
            } else {
                merged.setField(field, value);
            }
        });
        return Optional.of(merged);
    }

    public Side sideOfMetaData(MetaData local, MetaData remote) {
        return sideOf(metaData, serialize(local, citationKeyPatterns), serialize(remote, citationKeyPatterns));
    }

    public Side sideOfPreamble(@Nullable String local, @Nullable String remote) {
        return sideOf(preamble, local, remote);
    }

    /// @param local  the content in memory, `null` when the string does not exist in memory
    /// @param remote the content on disk, `null` when the string does not exist on disk
    public Side sideOfString(String name, @Nullable String local, @Nullable String remote) {
        return sideOf(strings.get(name), local, remote);
    }

    /// A string that exists on disk only. The two-way comparison cannot see that it is the renamed form of a baseline
    /// string when memory deleted that string, so the content is matched against the baseline: a deletion in memory
    /// against a rename on disk is a conflict.
    ///
    /// @param existsInMemory whether memory has a string of the given name
    public Side sideOfAddedString(String name, String content, Predicate<String> existsInMemory) {
        if (strings.containsKey(name)) {
            return sideOf(strings.get(name), null, content);
        }
        // Only an unambiguous source counts: with several equal-valued strings gone from memory, nothing says which one was renamed
        long deletedWithSameContent = strings.entrySet().stream()
                                             .filter(base -> base.getValue().equals(content) && !existsInMemory.test(base.getKey()))
                                             .count();
        return deletedWithSameContent == 1 ? Side.BOTH : Side.DISK;
    }

    /// A string renamed on disk is taken over only if memory neither touched the old string nor already uses the new name.
    ///
    /// @param existsInMemory whether memory has a string of the given name
    public Side sideOfStringRename(String oldName, String oldContent, String newName, Predicate<String> existsInMemory) {
        boolean oldUntouchedInMemory = Objects.equals(strings.get(oldName), oldContent);
        return oldUntouchedInMemory && !existsInMemory.test(newName) ? Side.DISK : Side.BOTH;
    }

    /// Among the given baseline entries, the one a disk entry most likely was taken from when neither key nor exact
    /// content match anymore: same type and at most one field besides the citation key differing, and only when
    /// exactly one candidate is that close.
    public Optional<String> closestOf(Collection<String> candidateIds, BibEntry remote) {
        EntrySnapshot snapshot = EntrySnapshot.view(remote).withoutKey();
        List<String> close = candidateIds.stream()
                                         .filter(id -> entriesById.containsKey(id))
                                         .filter(id -> differingFields(entriesById.get(id).withoutKey(), snapshot) <= 1)
                                         .toList();
        return close.size() == 1 ? Optional.of(close.getFirst()) : Optional.empty();
    }

    private static int differingFields(EntrySnapshot one, EntrySnapshot other) {
        if (!one.type().equals(other.type())) {
            return Integer.MAX_VALUE;
        }
        Set<Field> fields = new HashSet<>(one.fields().keySet());
        fields.addAll(other.fields().keySet());
        return (int) fields.stream().filter(field -> !Objects.equals(one.fields().get(field), other.fields().get(field))).count();
    }

    /// `null` stands for "absent" on that side.
    private static <T> Side sideOf(@Nullable T base, @Nullable T local, @Nullable T remote) {
        boolean localChanged = !Objects.equals(base, local);
        boolean remoteChanged = !Objects.equals(base, remote);
        if (localChanged && remoteChanged) {
            return Side.BOTH;
        }
        return remoteChanged ? Side.DISK : Side.MEMORY;
    }

    // Carrying over items from a previous baseline: an external change that was not applied must be seen against the
    // same ancestor next time, otherwise the current in-memory version would pass as the common ancestor.

    public void keepEntry(LibraryBaseline previous, String entryId) {
        EntrySnapshot base = previous.entriesById.get(entryId);
        if (base == null) {
            entriesById.remove(entryId);
        } else {
            entriesById.put(entryId, base);
        }
    }

    /// Keeps the previous baseline of the in-memory entry a disk entry was taken from, if there is one.
    public void keepEntryFor(LibraryBaseline previous, Lookup previousLookup, BibEntry remote) {
        previousLookup.baseIdOf(remote).ifPresent(baseId -> entriesById.put(baseId, previous.entriesById.get(baseId)));
    }

    public void keepMetaData(LibraryBaseline previous) {
        metaData.clear();
        metaData.putAll(previous.metaData);
    }

    public void keepPreamble(LibraryBaseline previous) {
        preamble = previous.preamble;
    }

    public void keepString(LibraryBaseline previous, String name) {
        String base = previous.strings.get(name);
        if (base == null) {
            strings.remove(name);
        } else {
            strings.put(name, base);
        }
    }

    /// The encoding is not part of the serialized metadata (it is written as a file header instead), but it is part
    /// of what the two-way comparison reports.
    private static Map<String, String> serialize(MetaData metaData, GlobalCitationKeyPatterns citationKeyPatterns) {
        Map<String, String> serialized = new HashMap<>(MetaDataSerializer.getSerializedStringMap(metaData, citationKeyPatterns));
        serialized.put(ENCODING_KEY, metaData.getEncoding().map(Charset::name).orElse(""));
        // The synchronization setting itself is never synchronized: switching it on in memory must not be undone by
        // the older value in the file, nor must the file switch it off under a running synchronization
        serialized.remove(MetaData.SYNCHRONIZE_WITH_FILE);
        return serialized;
    }
}
