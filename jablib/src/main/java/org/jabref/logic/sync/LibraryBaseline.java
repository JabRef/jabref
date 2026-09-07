package org.jabref.logic.sync;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private record EntrySnapshot(EntryType type, Map<Field, String> fields) {
        static EntrySnapshot of(BibEntry entry) {
            return new EntrySnapshot(entry.getType(), Map.copyOf(entry.getFieldMap()));
        }

        /// Wraps the live field map without copying, for a comparison made right away
        static EntrySnapshot view(BibEntry entry) {
            return new EntrySnapshot(entry.getType(), entry.getFieldMap());
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
            return new EntrySnapshot(type, rest);
        }

        /// Only needed as the common ancestor of a field-level merge, which is the rare both-sides case
        BibEntry toEntry() {
            return new BibEntry(type).withFields(fields);
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
        private final Map<EntrySnapshot, Map.Entry<String, EntrySnapshot>> byContentExceptKey = new HashMap<>();

        private Lookup() {
            for (Map.Entry<String, EntrySnapshot> entry : entriesById.entrySet()) {
                entry.getValue().citationKey().ifPresent(key -> byKey.computeIfAbsent(key, _ -> new ArrayList<>()).add(entry));
                byContentExceptKey.putIfAbsent(entry.getValue().withoutKey(), entry);
            }
        }

        /// The id of the in-memory entry the given disk entry was taken from: by citation key (preferring identical
        /// content, as keys need not be unique), or else by the remaining content, which covers entries without a
        /// key as well as a key changed on disk.
        public Optional<String> baseIdOf(BibEntry remote) {
            return find(remote).map(Map.Entry::getKey);
        }

        /// A disk entry without in-memory counterpart is either new on disk, or was deleted in memory (and possibly
        /// modified on disk as well).
        public Side sideOfAddedEntry(BibEntry remote) {
            return find(remote).map(base -> base.getValue().equals(EntrySnapshot.view(remote)) ? Side.MEMORY : Side.BOTH)
                               .orElse(Side.DISK);
        }

        private Optional<Map.Entry<String, EntrySnapshot>> find(BibEntry remote) {
            EntrySnapshot snapshot = EntrySnapshot.view(remote);
            Optional<Map.Entry<String, EntrySnapshot>> byKeyMatch = snapshot.citationKey()
                                                                            .map(key -> byKey.getOrDefault(key, List.of()))
                                                                            .flatMap(candidates -> candidates.stream().filter(entry -> entry.getValue().equals(snapshot)).findFirst()
                                                                                                             .or(() -> candidates.stream().findFirst()));
            return byKeyMatch.or(() -> Optional.ofNullable(byContentExceptKey.get(snapshot.withoutKey())));
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
        // an addition, so only fields set differently on both sides count as conflicts and the in-memory type is kept
        BibEntry ancestor = base == null ? new BibEntry(local.getType()) : base.toEntry();
        if (ConflictRules.hasConflictingFields(ancestor, local, remote)) {
            return Optional.empty();
        }
        BibEntry merged = new BibEntry(local);
        if (!ancestor.getType().equals(remote.getType())) {
            merged.setType(remote.getType());
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

    /// A string renamed on disk is taken over only if memory neither touched the old string nor already uses the new name.
    public Side sideOfStringRename(String oldName, String oldContent, String newName) {
        boolean oldUntouchedInMemory = Objects.equals(strings.get(oldName), oldContent);
        return oldUntouchedInMemory && !strings.containsKey(newName) ? Side.DISK : Side.BOTH;
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
        return serialized;
    }
}
