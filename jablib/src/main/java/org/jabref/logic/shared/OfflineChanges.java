package org.jabref.logic.shared;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryTypeFactory;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNullElse;

/// Local changes that have not reached the shared database because the connection was down.
/// Kept in memory and mirrored to one file per database, so that they survive a restart and are
/// synchronized on the next connect (see [DBMSSynchronizer]).
// [impl->req~shared-database.offline-changes~1]
@NullMarked
public class OfflineChanges {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfflineChanges.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /// An entry as last seen locally. `baseVersion` is the shared version the change was made
    /// against: the optimistic lock needs it to notice that the shared entry moved on meanwhile.
    public record EntryState(int baseVersion, String entryType, Map<String, String> fields) {
        static EntryState of(BibEntry entry) {
            // Sorted, so that the file content is stable across sessions
            Map<String, String> fields = new TreeMap<>();
            entry.getFieldMap().forEach((field, value) -> fields.put(field.getName(), value));
            return new EntryState(entry.getSharedBibEntryData().getVersion(), entry.getType().getName(), fields);
        }

        public BibEntry toBibEntry() {
            BibEntry entry = new BibEntry(EntryTypeFactory.parse(entryType));
            applyTo(entry);
            return entry;
        }

        /// Restores this state on the given entry without triggering synchronization
        public void applyTo(BibEntry entry) {
            entry.setType(EntryTypeFactory.parse(entryType), EntriesEventSource.SHARED);
            fields.forEach((name, value) -> entry.setField(FieldFactory.parseField(name), value, EntriesEventSource.SHARED));
            for (Field field : entry.getFields()) {
                if (!fields.containsKey(field.getName())) {
                    entry.clearField(field, EntriesEventSource.SHARED);
                }
            }
            entry.getSharedBibEntryData().setVersion(baseVersion);
        }
    }

    /// Everything recorded up to a [#peek]. New entries are keyed by their local entry id, so
    /// that a reconnect without restart finds them in the local library.
    ///
    /// @param removedEntries the shared version each removed entry had when it was removed: the
    ///                       optimistic lock needs it to notice that the shared entry moved on
    /// @param metaDataBase   the shared metadata as last known before the recorded metadata was
    ///                       changed - the merge base for [#mergeMetaDataInto]
    public record Recorded(Map<Integer, EntryState> changedEntries,
                           Map<String, EntryState> newEntries,
                           Map<Integer, Integer> removedEntries,
                           @Nullable Map<String, String> metaData,
                           @Nullable Map<String, String> metaDataBase) {
        public boolean isEmpty() {
            return changedEntries.isEmpty() && newEntries.isEmpty() && removedEntries.isEmpty() && (metaData == null);
        }

        /// Metadata has no version to lock on, so the recorded metadata is merged key by key:
        /// a key the user changed while offline wins (also over a concurrent change on the
        /// shared side - the user was promised that these changes are kept), every other key
        /// stays as the shared side has it now.
        public Map<String, String> mergeMetaDataInto(Map<String, String> sharedMetaData) {
            Map<String, String> recorded = requireNonNullElse(metaData, Map.of());
            Map<String, String> base = requireNonNullElse(metaDataBase, Map.of());
            Map<String, String> merged = new LinkedHashMap<>(sharedMetaData);
            Stream.concat(base.keySet().stream(), recorded.keySet().stream())
                  .distinct()
                  .filter(key -> !Objects.equals(recorded.get(key), base.get(key)))
                  .forEach(key -> Optional.ofNullable(recorded.get(key))
                                          .ifPresentOrElse(value -> merged.put(key, value), () -> merged.remove(key)));
            return merged;
        }
    }

    private final Path file;
    private final Map<Integer, EntryState> changedEntries = new LinkedHashMap<>();
    private final Map<String, EntryState> newEntries = new LinkedHashMap<>();
    private final Map<Integer, Integer> removedEntries = new LinkedHashMap<>();
    private @Nullable Map<String, String> metaData;
    private @Nullable Map<String, String> metaDataBase;

    private OfflineChanges(Path file) {
        this.file = file;
    }

    /// Loads the changes recorded for the given database, if any
    public static OfflineChanges load(Path directory, DatabaseConnectionProperties properties) {
        OfflineChanges changes = new OfflineChanges(directory.resolve(fileName(properties)));
        changes.reload();
        return changes;
    }

    /// Takes over what is on disk, so that a change is never applied to a snapshot another JabRef
    /// session has meanwhile written to. Called before every modification.
    ///
    /// ponytail: read and write are not one atomic step - two sessions recording at the very same
    /// moment can still lose a record; a lock file would close that window
    private void reload() {
        changedEntries.clear();
        newEntries.clear();
        removedEntries.clear();
        metaData = null;
        metaDataBase = null;
        if (!Files.exists(file)) {
            return;
        }
        try {
            // Gson returns null for a file holding JSON null, and leaves absent members null
            Recorded recorded = GSON.fromJson(Files.readString(file), Recorded.class);
            if (recorded == null) {
                LOGGER.error("The changes recorded for the shared database in {} are empty", file);
                return;
            }
            changedEntries.putAll(requireNonNullElse(recorded.changedEntries(), Map.of()));
            newEntries.putAll(requireNonNullElse(recorded.newEntries(), Map.of()));
            removedEntries.putAll(requireNonNullElse(recorded.removedEntries(), Map.of()));
            metaData = recorded.metaData();
            metaDataBase = recorded.metaDataBase();
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Could not read the changes recorded for the shared database from {}", file, e);
        }
    }

    /// One file per database: identified by user, host, port and database name (or the JDBC URL
    /// in expert mode), hashed so that the name is file-system safe
    static String fileName(DatabaseConnectionProperties properties) {
        String identity = properties.isUseExpertMode()
                          ? properties.getJdbcUrl()
                          : properties.getUser() + "@" + properties.getHost() + ":" + properties.getPort() + "/" + properties.getDatabase();
        return Hashing.sha256().hashString(identity, StandardCharsets.UTF_8) + ".json";
    }

    public synchronized boolean isEmpty() {
        return snapshot().isEmpty();
    }

    public synchronized void recordChange(BibEntry entry) {
        reload();
        int sharedId = entry.getSharedBibEntryData().getSharedIdAsInt();
        if ((sharedId == -1) || newEntries.containsKey(entry.getId())) {
            // Not yet on the shared side
            newEntries.put(entry.getId(), EntryState.of(entry));
        } else {
            // The base version is the one of the first change - the entry cannot move on while offline
            changedEntries.merge(sharedId, EntryState.of(entry),
                    (first, latest) -> new EntryState(first.baseVersion(), latest.entryType(), latest.fields()));
        }
        save();
    }

    public synchronized void recordInsert(List<BibEntry> entries) {
        reload();
        for (BibEntry entry : entries) {
            newEntries.put(entry.getId(), EntryState.of(entry));
        }
        save();
    }

    public synchronized void recordRemoval(List<BibEntry> entries) {
        reload();
        for (BibEntry entry : entries) {
            if (newEntries.remove(entry.getId()) != null) {
                // Never reached the shared side - nothing to remove there
                continue;
            }
            int sharedId = entry.getSharedBibEntryData().getSharedIdAsInt();
            if (sharedId != -1) {
                EntryState recordedChange = changedEntries.remove(sharedId);
                // The version of the first offline change, if any - the entry cannot move on while offline
                removedEntries.put(sharedId, recordedChange == null ? entry.getSharedBibEntryData().getVersion() : recordedChange.baseVersion());
            }
        }
        save();
    }

    /// @param base the shared metadata as last known - kept from the first record, the metadata
    ///             cannot move on while offline
    public synchronized void recordMetaData(Map<String, String> serializedMetaData, @Nullable Map<String, String> base) {
        reload();
        if (metaData == null) {
            metaDataBase = base;
        }
        metaData = serializedMetaData;
        save();
    }

    /// Turns the record of a change into the record of a new entry, because the shared entry it
    /// was recorded against is gone and the entry is inserted afresh instead. One step, so that
    /// no moment exists in which the entry is recorded as neither.
    public synchronized void recordAsNew(int sharedId, BibEntry entry) {
        reload();
        changedEntries.remove(sharedId);
        newEntries.put(entry.getId(), EntryState.of(entry));
        save();
    }

    /// Re-keys the records of new entries restored after a restart under fresh local ids, so
    /// that a failed insert records them under the ids it knows instead of next to the old records
    public synchronized void rekeyInserts(Map<String, BibEntry> restoredEntriesByRecordedId) {
        reload();
        restoredEntriesByRecordedId.forEach((recordedId, entry) -> {
            EntryState state = newEntries.remove(recordedId);
            if (state != null) {
                newEntries.put(entry.getId(), state);
            }
        });
        save();
    }

    /// Drops the records of entries that reached the shared database after all
    public synchronized void forget(Collection<BibEntry> entries) {
        reload();
        boolean removed = false;
        for (BibEntry entry : entries) {
            removed |= newEntries.remove(entry.getId()) != null;
            removed |= changedEntries.remove(entry.getSharedBibEntryData().getSharedIdAsInt()) != null;
        }
        if (removed) {
            save();
        }
    }

    /// Drops the records of removals that reached the shared database (or need not, because the
    /// shared entry is gone or was kept)
    public synchronized void forgetRemovals(Collection<Integer> sharedIds) {
        reload();
        if (removedEntries.keySet().removeAll(sharedIds)) {
            save();
        }
    }

    /// Drops the recorded metadata after it reached the shared database
    public synchronized void forgetMetaData() {
        reload();
        if (metaData != null) {
            metaData = null;
            metaDataBase = null;
            save();
        }
    }

    /// Hands out everything recorded so far. The records are kept until each of them is written
    /// (see the `forget` methods), so that nothing is lost when the replay fails or is interrupted.
    public synchronized Recorded peek() {
        reload();
        return snapshot();
    }

    private Recorded snapshot() {
        return new Recorded(new LinkedHashMap<>(changedEntries), new LinkedHashMap<>(newEntries), new LinkedHashMap<>(removedEntries), metaData, metaDataBase);
    }

    private void save() {
        try {
            Recorded recorded = snapshot();
            if (recorded.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            Files.createDirectories(file.getParent());
            Path temporaryFile = Files.createTempFile(file.getParent(), "shared-database", ".json.tmp");
            Files.writeString(temporaryFile, GSON.toJson(recorded));
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.error("Could not save the changes made while the shared database was unavailable to {}", file, e);
        }
    }
}
