package org.jabref.gui.collab;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.preamblechange.PreambleChange;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.collab.stringchange.BibTexStringChange;
import org.jabref.gui.collab.stringdelete.BibTexStringDelete;
import org.jabref.gui.collab.stringrename.BibTexStringRename;
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

/// The library as it last matched its file on disk. Comparing both the in-memory library and the file against it tells
/// for each external change whether the same item was also modified in memory, which is the only case that needs a
/// review; everything else is merged silently (see [#triage]).
///
/// Entries are keyed by the id of the in-memory entry they were copied from, so that a citation key change in memory
/// does not break the association. The metadata is kept in its serialized form, which is the only way to snapshot it.
@NullMarked
public final class LibraryBaseline {

    /// Outcome of comparing external changes against the baseline.
    ///
    /// @param diskOnly   changes to items untouched in memory; already accepted, to be applied without asking
    /// @param bothSides  changes to items that were modified in memory as well, in a way that cannot be merged automatically; need a review
    /// @param memoryOnly not external changes at all: differences caused by unsaved in-memory edits; to be dropped
    public record Triage(List<DatabaseChange> diskOnly, List<DatabaseChange> bothSides, List<DatabaseChange> memoryOnly) {
    }

    private enum Side { DISK, MEMORY, BOTH }

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
            BibEntry entry = new BibEntry(type);
            fields.forEach(entry::setField);
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

    /// Lookup tables over the baseline entries, built once per pass over the changes so that each lookup is O(1)
    /// instead of a scan over the library.
    private final class Index {
        private final Map<String, List<Map.Entry<String, EntrySnapshot>>> byKey = new HashMap<>();
        private final Map<EntrySnapshot, Map.Entry<String, EntrySnapshot>> byContentExceptKey = new HashMap<>();

        Index() {
            for (Map.Entry<String, EntrySnapshot> entry : entriesById.entrySet()) {
                entry.getValue().citationKey().ifPresent(key -> byKey.computeIfAbsent(key, _ -> new ArrayList<>()).add(entry));
                byContentExceptKey.putIfAbsent(entry.getValue().withoutKey(), entry);
            }
        }

        /// The baseline of a disk entry without in-memory counterpart, keyed by the id of the in-memory entry it was
        /// taken from: by citation key (preferring identical content, as keys need not be unique), or else by the
        /// remaining content, which covers entries without a key as well as a key changed on disk.
        Optional<Map.Entry<String, EntrySnapshot>> find(BibEntry remote) {
            EntrySnapshot snapshot = EntrySnapshot.view(remote);
            Optional<Map.Entry<String, EntrySnapshot>> byKeyMatch = snapshot.citationKey()
                    .map(key -> byKey.getOrDefault(key, List.of()))
                    .flatMap(candidates -> candidates.stream().filter(entry -> entry.getValue().equals(snapshot)).findFirst()
                                                     .or(() -> candidates.stream().findFirst()));
            return byKeyMatch.or(() -> Optional.ofNullable(byContentExceptKey.get(snapshot.withoutKey())));
        }
    }

    /// Sorts the differences between the in-memory library and its file, as computed by
    /// [DatabaseChangeList#compareAndGetChanges], by which side changed. Entries modified on both sides in different
    /// fields are merged field by field (with the Git merge rules) into a new, accepted [EntryChange].
    public Triage triage(List<DatabaseChange> changes, BibDatabaseContext local, @Nullable DatabaseChangeResolverFactory resolverFactory) {
        Triage triage = new Triage(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        // A group change is always accompanied by the metadata change it is part of, which precedes it in the list
        Side metaDataSide = Side.BOTH;
        Index index = new Index();
        for (DatabaseChange change : pairSplitEntries(changes, local, resolverFactory, index)) {
            Side side = switch (change) {
                case EntryChange entryChange -> {
                    Classified classified = classifyEntryChange(entryChange, local, resolverFactory);
                    change = classified.change();
                    yield classified.side();
                }
                // No in-memory counterpart: either new on disk, or deleted in memory (and possibly modified on disk)
                case EntryAdd entryAdd ->
                        index.find(entryAdd.getAddedEntry()).map(base -> base.getValue().equals(EntrySnapshot.view(entryAdd.getAddedEntry())) ? Side.MEMORY : Side.BOTH)
                                                      .orElse(Side.DISK);
                // No counterpart on disk: either deleted on disk, or added in memory (and possibly modified on disk)
                case EntryDelete entryDelete -> {
                    EntrySnapshot base = entriesById.get(entryDelete.getDeletedEntry().getId());
                    yield base == null ? Side.MEMORY : base.equals(EntrySnapshot.view(entryDelete.getDeletedEntry())) ? Side.DISK : Side.BOTH;
                }
                case MetadataChange metadataChange -> {
                    metaDataSide = sideOf(metaData, serialize(local.getMetaData(), citationKeyPatterns), serialize(metadataChange.getMetaDataDiff().getNewMetaData(), citationKeyPatterns));
                    yield metaDataSide;
                }
                case GroupChange _ ->
                        metaDataSide;
                case PreambleChange preambleChange ->
                        sideOf(preamble, local.getDatabase().getPreamble().orElse(null), preambleChange.getPreambleDiff().getNewPreamble());
                case BibTexStringAdd stringAdd ->
                        sideOf(strings.get(stringAdd.getAddedString().getName()), null, stringAdd.getAddedString().getContent());
                case BibTexStringDelete stringDelete ->
                        sideOf(strings.get(stringDelete.getDeletedString().getName()), stringDelete.getDeletedString().getContent(), null);
                case BibTexStringChange stringChange ->
                        sideOf(strings.get(stringChange.getOldString().getName()), stringChange.getOldString().getContent(), stringChange.getNewString().getContent());
                case BibTexStringRename stringRename -> {
                    boolean oldUntouchedInMemory = Objects.equals(strings.get(stringRename.getOldString().getName()), stringRename.getOldString().getContent());
                    yield oldUntouchedInMemory && !strings.containsKey(stringRename.getNewString().getName()) ? Side.DISK : Side.BOTH;
                }
            };
            switch (side) {
                case DISK -> {
                    change.accept();
                    triage.diskOnly().add(change);
                }
                case BOTH ->
                        triage.bothSides().add(change);
                case MEMORY ->
                        triage.memoryOnly().add(change);
            }
        }
        return triage;
    }

    /// Carries over the baseline of every item whose external change was not applied, so that the next scan sees
    /// the same divergence again instead of mistaking the current in-memory version for the common ancestor.
    public void keepUnresolved(LibraryBaseline previous, List<DatabaseChange> unresolved) {
        Index previousIndex = previous.new Index();
        for (DatabaseChange change : unresolved) {
            switch (change) {
                case EntryChange entryChange ->
                        keepEntry(previous, entryChange.getOldEntry().getId());
                case EntryDelete entryDelete ->
                        keepEntry(previous, entryDelete.getDeletedEntry().getId());
                case EntryAdd entryAdd ->
                        previousIndex.find(entryAdd.getAddedEntry()).ifPresent(base -> entriesById.put(base.getKey(), base.getValue()));
                case MetadataChange _,
                     GroupChange _ -> {
                    metaData.clear();
                    metaData.putAll(previous.metaData);
                }
                case PreambleChange _ ->
                        preamble = previous.preamble;
                case BibTexStringAdd stringAdd ->
                        keepString(previous, stringAdd.getAddedString().getName());
                case BibTexStringDelete stringDelete ->
                        keepString(previous, stringDelete.getDeletedString().getName());
                case BibTexStringChange stringChange ->
                        keepString(previous, stringChange.getOldString().getName());
                case BibTexStringRename stringRename -> {
                    keepString(previous, stringRename.getOldString().getName());
                    keepString(previous, stringRename.getNewString().getName());
                }
            }
        }
    }

    /// The two-way diff pairs entries by similarity, so an entry whose citation key changed on one side while fields
    /// changed on the other can fall below the similarity threshold and show up as a deletion plus an addition. The
    /// baseline knows both belong to the same entry. One pass over the changes: a paired addition becomes the change,
    /// its deletion is dropped.
    private List<DatabaseChange> pairSplitEntries(List<DatabaseChange> changes, BibDatabaseContext local, @Nullable DatabaseChangeResolverFactory resolverFactory, Index index) {
        Map<String, EntryDelete> deletesByBaseId = new HashMap<>();
        for (DatabaseChange change : changes) {
            if (change instanceof EntryDelete entryDelete && entriesById.containsKey(entryDelete.getDeletedEntry().getId())) {
                deletesByBaseId.put(entryDelete.getDeletedEntry().getId(), entryDelete);
            }
        }
        if (deletesByBaseId.isEmpty()) {
            return changes;
        }
        Map<EntryAdd, DatabaseChange> replacements = new HashMap<>();
        Set<EntryDelete> pairedDeletes = new HashSet<>();
        for (DatabaseChange change : changes) {
            if (change instanceof EntryAdd entryAdd) {
                index.find(entryAdd.getAddedEntry()).map(base -> deletesByBaseId.remove(base.getKey())).ifPresent(entryDelete -> {
                    pairedDeletes.add(entryDelete);
                    replacements.put(entryAdd, new EntryChange(entryDelete.getDeletedEntry(), entryAdd.getAddedEntry(), local, resolverFactory));
                });
            }
        }
        List<DatabaseChange> paired = new ArrayList<>(changes.size());
        for (DatabaseChange change : changes) {
            if (change instanceof EntryDelete entryDelete && pairedDeletes.contains(entryDelete)) {
                continue;
            }
            paired.add(change instanceof EntryAdd entryAdd ? replacements.getOrDefault(entryAdd, entryAdd) : change);
        }
        return paired;
    }

    /// Which side diverged from the baseline. Only called for items that differ between memory and disk, so at least
    /// one side changed. `null` stands for "absent" on that side.
    private static <T> Side sideOf(@Nullable T base, @Nullable T local, @Nullable T remote) {
        boolean localChanged = !Objects.equals(base, local);
        boolean remoteChanged = !Objects.equals(base, remote);
        if (localChanged && remoteChanged) {
            return Side.BOTH;
        }
        return remoteChanged ? Side.DISK : Side.MEMORY;
    }

    /// Field-level merge of an entry modified on both sides: disk wins for every field memory left alone, memory wins
    /// where disk left the field alone. Empty if the same field was changed differently on both sides.
    private static Optional<BibEntry> mergeFields(@Nullable EntrySnapshot base, BibEntry local, BibEntry remote) {
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

    private record Classified(Side side, DatabaseChange change) {
    }

    /// An entry changed on both sides in different fields is merged field by field into a new, accepted change.
    private Classified classifyEntryChange(EntryChange entryChange, BibDatabaseContext local, @Nullable DatabaseChangeResolverFactory resolverFactory) {
        EntrySnapshot base = entriesById.get(entryChange.getOldEntry().getId());
        Side side = sideOf(base, EntrySnapshot.view(entryChange.getOldEntry()), EntrySnapshot.view(entryChange.getNewEntry()));
        if (side != Side.BOTH) {
            return new Classified(side, entryChange);
        }
        return mergeFields(base, entryChange.getOldEntry(), entryChange.getNewEntry())
                .map(merged -> new Classified(Side.DISK, new EntryChange(entryChange.getOldEntry(), merged, local, resolverFactory)))
                .orElse(new Classified(Side.BOTH, entryChange));
    }

    private void keepEntry(LibraryBaseline previous, String entryId) {
        EntrySnapshot base = previous.entriesById.get(entryId);
        if (base == null) {
            entriesById.remove(entryId);
        } else {
            entriesById.put(entryId, base);
        }
    }

    private void keepString(LibraryBaseline previous, String name) {
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
