package org.jabref.logic.directorylibrary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Remembers which file of a directory library each [BibEntry] came from. This is the identity
/// backbone of a directory library: entries are keyed by their stable in-memory id
/// ([BibEntry#getId()]), so citation-key edits do not lose the association, and the Hayagriva
/// key (the top-level YAML map key) is tracked separately so it can be renamed on write-back.
///
/// Entries without a source (e.g. stubs created for PDFs that have no sidecar yet) are simply
/// absent; they get registered once a sidecar is written for them.
@NullMarked
public class DirectoryLibraryCatalog {

    public record EntrySource(Path yamlFile, String hayagrivaKey) {
    }

    private final Map<String, EntrySource> sourceByEntryId = new HashMap<>();
    private final Map<Path, List<String>> entryIdsByFile = new HashMap<>();

    public void register(BibEntry entry, Path yamlFile, String hayagrivaKey) {
        sourceByEntryId.put(entry.getId(), new EntrySource(yamlFile, hayagrivaKey));
        entryIdsByFile.computeIfAbsent(yamlFile, _ -> new ArrayList<>()).add(entry.getId());
    }

    public Optional<EntrySource> sourceOf(BibEntry entry) {
        return Optional.ofNullable(sourceByEntryId.get(entry.getId()));
    }

    /// Entry ids of all entries read from the given file, in file order.
    public List<String> entryIdsIn(Path yamlFile) {
        return List.copyOf(entryIdsByFile.getOrDefault(yamlFile, List.of()));
    }

    /// Re-homes all entries of `oldFile` to `newFile` (a rename/move on disk).
    public void relocateFile(Path oldFile, Path newFile) {
        List<String> entryIds = entryIdsByFile.remove(oldFile);
        if (entryIds == null) {
            return;
        }
        entryIdsByFile.put(newFile, entryIds);
        entryIds.forEach(entryId -> {
            EntrySource source = sourceByEntryId.get(entryId);
            if (source != null) {
                sourceByEntryId.put(entryId, new EntrySource(newFile, source.hayagrivaKey()));
            }
        });
    }

    /// Forgets all entries of the given file (deleted on disk or re-registered afterwards).
    public void removeFile(Path yamlFile) {
        List<String> entryIds = entryIdsByFile.remove(yamlFile);
        if (entryIds != null) {
            entryIds.forEach(sourceByEntryId::remove);
        }
    }
}
