package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// What the synchronizer knows about each file of a directory library beyond the
/// [DirectoryLibraryCatalog]: the fingerprint of its own last write (so the watcher's echo of
/// it is swallowed), the fingerprint of the content last read or written (so an external edit
/// is noticed before a pending write would overwrite it), and the entries as last read or
/// written — the base of the three-way merge that lets an external edit only touch the fields
/// it changed. Callers hold the synchronizer's monitor.
@NullMarked
class TrackedFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackedFiles.class);

    private final BibDatabaseContext databaseContext;
    private final DirectoryLibraryCatalog catalog;
    private final Map<Path, String> lastWrittenFingerprints = new HashMap<>();
    private final Map<Path, String> lastSeenFingerprints = new HashMap<>();
    private final Map<Path, SequencedMap<String, BibEntry>> baselines = new HashMap<>();

    TrackedFiles(BibDatabaseContext databaseContext, DirectoryLibraryCatalog catalog) {
        this.databaseContext = databaseContext;
        this.catalog = catalog;
    }

    DirectoryLibraryCatalog catalog() {
        return catalog;
    }

    /// Only entries still in the database: removed entries stay cataloged until their file is
    /// rewritten (see [SidecarWriteBack]).
    List<BibEntry> entriesOf(Path file) {
        List<String> ids = catalog.entryIdsIn(file);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, BibEntry> byId = new HashMap<>();
        List<BibEntry> allEntries = databaseContext.getDatabase().getEntries();
        // The UI thread mutates the (synchronized) list concurrently; field reads need no lock
        synchronized (allEntries) {
            allEntries.forEach(entry -> byId.put(entry.getId(), entry));
        }
        return ids.stream().flatMap(id -> Optional.ofNullable(byId.get(id)).stream()).toList();
    }

    /// Records the scanned files' content as the merge base; the live entries still equal it
    /// at this point.
    void takeBaseline() {
        for (Path file : catalog.files()) {
            recordSeen(file);
            baselines.put(file, copiesByKey(entriesOf(file)));
        }
    }

    /// Registers a file this application just wrote itself: the next change event for it is
    /// recognized as a self-echo and not re-imported (consumed on match).
    void recordWritten(Path file, byte[] content) {
        String fingerprint = hash(content);
        lastWrittenFingerprints.put(normalize(file), fingerprint);
        lastSeenFingerprints.put(normalize(file), fingerprint);
    }

    boolean consumeSelfEcho(Path file) {
        Path normalized = normalize(file);
        if (!lastWrittenFingerprints.containsKey(normalized)) {
            return false;
        }
        return currentHash(file).map(current -> lastWrittenFingerprints.remove(normalized, current)).orElse(false);
    }

    /// Remembers the file's current content as read.
    void recordSeen(Path file) {
        currentHash(file).ifPresent(fingerprint -> lastSeenFingerprints.put(normalize(file), fingerprint));
    }

    /// Whether the file's content differs from what was last read or written; unknown files
    /// count as unchanged.
    boolean changedExternally(Path file) {
        return Optional.ofNullable(lastSeenFingerprints.get(normalize(file)))
                       .map(lastSeen -> Files.exists(file) && !currentHash(file).equals(Optional.of(lastSeen)))
                       .orElse(false);
    }

    Map<String, BibEntry> baseline(Path file) {
        return baselines.getOrDefault(file, new LinkedHashMap<>());
    }

    void setBaseline(Path file, SequencedMap<String, BibEntry> entriesByKey) {
        baselines.put(file, entriesByKey);
    }

    /// Re-homes everything known about `oldFile` to `newFile` (a rename or move).
    void relocate(Path oldFile, Path newFile) {
        catalog.relocateFile(oldFile, newFile);
        Optional.ofNullable(baselines.remove(oldFile)).ifPresent(baseline -> baselines.put(newFile, baseline));
        Optional.ofNullable(lastSeenFingerprints.remove(normalize(oldFile)))
                .ifPresent(fingerprint -> lastSeenFingerprints.put(normalize(newFile), fingerprint));
    }

    /// Forgets a file that no longer holds entries.
    void forget(Path file) {
        catalog.removeFile(file);
        baselines.remove(file);
        lastSeenFingerprints.remove(normalize(file));
    }

    static SequencedMap<String, BibEntry> copiesByKey(List<BibEntry> entries) {
        SequencedMap<String, BibEntry> copies = new LinkedHashMap<>();
        entries.forEach(entry -> copies.putIfAbsent(entry.getCitationKey().orElse(""), new BibEntry(entry)));
        return copies;
    }

    private static Path normalize(Path file) {
        return file.toAbsolutePath().normalize();
    }

    private static Optional<String> currentHash(Path file) {
        try {
            return Optional.of(hash(Files.readAllBytes(file)));
        } catch (IOException e) {
            LOGGER.debug("Could not fingerprint {}", file, e);
            return Optional.empty();
        }
    }

    private static String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is guaranteed to be available", e);
        }
    }
}
