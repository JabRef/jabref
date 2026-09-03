package org.jabref.logic.shared;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            Map<String, String> fields = new LinkedHashMap<>();
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

    /// Everything recorded up to a [#take]. New entries are keyed by their local entry id, so
    /// that a reconnect without restart finds them in the local library.
    public record Recorded(Map<Integer, EntryState> changedEntries,
                           Map<String, EntryState> newEntries,
                           Set<Integer> removedIds,
                           @Nullable Map<String, String> metaData) {
        public boolean isEmpty() {
            return changedEntries.isEmpty() && newEntries.isEmpty() && removedIds.isEmpty() && (metaData == null);
        }
    }

    private final Path file;
    private final Map<Integer, EntryState> changedEntries = new LinkedHashMap<>();
    private final Map<String, EntryState> newEntries = new LinkedHashMap<>();
    private final Set<Integer> removedIds = new LinkedHashSet<>();
    private @Nullable Map<String, String> metaData;

    private OfflineChanges(Path file) {
        this.file = file;
    }

    /// Loads the changes recorded for the given database, if any
    public static OfflineChanges load(Path directory, DatabaseConnectionProperties properties) {
        OfflineChanges changes = new OfflineChanges(directory.resolve(fileName(properties)));
        if (!Files.exists(changes.file)) {
            return changes;
        }
        try {
            Recorded recorded = GSON.fromJson(Files.readString(changes.file), Recorded.class);
            changes.changedEntries.putAll(recorded.changedEntries());
            changes.newEntries.putAll(recorded.newEntries());
            changes.removedIds.addAll(recorded.removedIds());
            changes.metaData = recorded.metaData();
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Could not read the changes recorded for the shared database from {}", changes.file, e);
        }
        return changes;
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
        return changedEntries.isEmpty() && newEntries.isEmpty() && removedIds.isEmpty() && (metaData == null);
    }

    public synchronized void recordChange(BibEntry entry) {
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
        for (BibEntry entry : entries) {
            newEntries.put(entry.getId(), EntryState.of(entry));
        }
        save();
    }

    public synchronized void recordRemoval(List<BibEntry> entries) {
        for (BibEntry entry : entries) {
            if (newEntries.remove(entry.getId()) != null) {
                // Never reached the shared side - nothing to remove there
                continue;
            }
            int sharedId = entry.getSharedBibEntryData().getSharedIdAsInt();
            if (sharedId != -1) {
                changedEntries.remove(sharedId);
                removedIds.add(sharedId);
            }
        }
        save();
    }

    public synchronized void recordMetaData(Map<String, String> serializedMetaData) {
        metaData = serializedMetaData;
        save();
    }

    /// Drops the record of an entry that reached the shared database after all
    public synchronized void forget(BibEntry entry) {
        boolean removed = newEntries.remove(entry.getId()) != null;
        removed |= changedEntries.remove(entry.getSharedBibEntryData().getSharedIdAsInt()) != null;
        if (removed) {
            save();
        }
    }

    /// Hands out everything recorded so far and forgets it: the caller synchronizes the changes,
    /// and whatever fails again is recorded again
    public synchronized Recorded take() {
        Recorded recorded = new Recorded(new LinkedHashMap<>(changedEntries), new LinkedHashMap<>(newEntries), new LinkedHashSet<>(removedIds), metaData);
        changedEntries.clear();
        newEntries.clear();
        removedIds.clear();
        metaData = null;
        save();
        return recorded;
    }

    private void save() {
        try {
            if (isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            Files.createDirectories(file.getParent());
            Path temporaryFile = Files.createTempFile(file.getParent(), "shared-database", ".json.tmp");
            Files.writeString(temporaryFile, GSON.toJson(new Recorded(changedEntries, newEntries, removedIds, metaData)));
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.error("Could not save the changes made while the shared database was unavailable to {}", file, e);
        }
    }
}
