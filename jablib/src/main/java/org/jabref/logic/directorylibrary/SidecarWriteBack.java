package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.exporter.AtomicFileOutputStream;
import org.jabref.logic.exporter.HayagrivaEntryWriter;
import org.jabref.logic.util.io.FileNameCleaner;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

/// Persists user changes of a directory library into its sidecar files (outbound direction):
/// an edit rewrites the entry's file read-modify-write (content JabRef does not understand
/// survives), the first user edit of an entry without a sidecar creates one — a Markdown
/// sidecar next to its PDF, sharing the base name, or named after the citation key — a
/// citation-key edit renames the YAML map key, and an entry that left the database is removed
/// from its file, which is disposed once its last entry is gone (the paired PDF is never
/// touched). A single-entry sidecar and its PDF are renamed together to the base name the
/// configured filename pattern generates for the entry.
///
/// The debounce, retry, and reporting of pending writes live in [PendingWrites]; this class
/// only knows how to write one file. Callers hold the synchronizer's monitor.
// [impl->req~directory-library.write-back~2]
// [impl->req~directory-library.pattern-rename~1]
@NullMarked
class SidecarWriteBack {

    private static final Logger LOGGER = LoggerFactory.getLogger(SidecarWriteBack.class);

    private final TrackedFiles files;
    private final Path root;
    private final Consumer<Runnable> modelUpdateMarshaller;
    private final Consumer<Path> fileDisposer;
    private final Function<BibEntry, Optional<String>> fileNameGenerator;
    private final Consumer<Path> externalChangeImporter;
    private final HayagrivaEntryWriter entryWriter = new HayagrivaEntryWriter();
    private final MarkdownSidecar markdownSidecar = new MarkdownSidecar();

    /// @param externalChangeImporter takes an external edit of a sidecar into the model (the
    ///                               inbound direction), called before such a file is rewritten
    SidecarWriteBack(TrackedFiles files,
                     Path root,
                     Consumer<Runnable> modelUpdateMarshaller,
                     Consumer<Path> fileDisposer,
                     Function<BibEntry, Optional<String>> fileNameGenerator,
                     Consumer<Path> externalChangeImporter) {
        this.files = files;
        this.root = root;
        this.modelUpdateMarshaller = modelUpdateMarshaller;
        this.fileDisposer = fileDisposer;
        this.fileNameGenerator = fileNameGenerator;
        this.externalChangeImporter = externalChangeImporter;
    }

    /// The file a changed entry is written to; assigned on the entry's first change.
    Path fileFor(BibEntry entry) {
        return files.catalog().sourceOf(entry)
                    .map(DirectoryLibraryCatalog.EntrySource::yamlFile)
                    .orElseGet(() -> assignSidecar(entry));
    }

    /// The files removed entries came from. They stay cataloged until the write runs, so an
    /// undo within the debounce window lands the entry back in its own file.
    List<Path> filesOf(List<BibEntry> entries) {
        return entries.stream()
                      .flatMap(entry -> files.catalog().sourceOf(entry).stream())
                      .map(DirectoryLibraryCatalog.EntrySource::yamlFile)
                      .distinct()
                      .toList();
    }

    /// Writes the file's current entries; disposes the file when none is left.
    ///
    /// @param immediate write even if the file changed externally in between (the external
    ///                  edit is taken into the model on the caller's thread first)
    /// @return whether the file was written; `false` defers the write until the model has
    /// taken in an external edit that landed since the file was last read or written
    boolean write(Path file, boolean immediate) throws IOException {
        if (files.changedExternally(file)) {
            // The model update is marshalled (asynchronously in the GUI), so the write is retried
            // one debounce later — unless the caller flushes, where the user's state must win
            externalChangeImporter.accept(file);
            if (!immediate) {
                return false;
            }
        }

        DirectoryLibraryCatalog catalog = files.catalog();
        List<BibEntry> entries = files.entriesOf(file);
        Set<String> liveIds = entries.stream().map(BibEntry::getId).collect(Collectors.toSet());
        catalog.entryIdsIn(file).stream().filter(id -> !liveIds.contains(id)).forEach(catalog::removeEntry);
        if (entries.isEmpty()) {
            files.forget(file);
            if (Files.exists(file)) {
                fileDisposer.accept(file);
            }
            return true;
        }
        // Multi-entry files have no single generating entry and keep their name
        Path target = entries.size() == 1 ? applyFileNamePattern(file, entries.getFirst()) : file;
        List<HayagrivaEntryWriter.KeyedEntry> keyedEntries = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();
        for (BibEntry entry : entries) {
            String previousKey = catalog.sourceOf(entry)
                                        .map(DirectoryLibraryCatalog.EntrySource::hayagrivaKey)
                                        .orElse("");
            String targetKey = entry.getCitationKey()
                                    .filter(not(String::isBlank))
                                    .orElse(previousKey.isBlank() ? "entry" : previousKey);
            String uniqueKey = targetKey;
            int counter = 1;
            while (!usedKeys.add(uniqueKey)) {
                uniqueKey = targetKey + "-" + counter++;
            }
            keyedEntries.add(new HayagrivaEntryWriter.KeyedEntry(previousKey, uniqueKey, entry));
        }
        String existingDocument = Files.exists(target) ? Files.readString(target, StandardCharsets.UTF_8) : "";
        String document = MarkdownSidecar.hasMarkdownExtension(target)
                          ? markdownSidecar.merge(existingDocument, keyedEntries)
                          : entryWriter.mergeIntoDocument(existingDocument, keyedEntries);
        byte[] content = document.getBytes(StandardCharsets.UTF_8);
        // Written atomically: the polling watcher (or another process) must never see a
        // half-written sidecar. The fingerprint is recorded only once the file is really there.
        try (AtomicFileOutputStream output = new AtomicFileOutputStream(target, false)) {
            output.write(content);
        }
        files.recordWritten(target, content);
        keyedEntries.forEach(keyedEntry -> catalog.updateHayagrivaKey(keyedEntry.entry(), keyedEntry.targetKey()));
        SequencedMap<String, BibEntry> written = new LinkedHashMap<>();
        keyedEntries.forEach(keyedEntry -> written.put(keyedEntry.targetKey(), new BibEntry(keyedEntry.entry())));
        files.setBaseline(target, written);
        return true;
    }

    /// The first user change of an entry without a source materializes its sidecar — a Markdown
    /// sidecar (see [MarkdownSidecar]): next to the entry's PDF (sharing the base name, per the
    /// pairing convention), or named after the citation key for entries without a file.
    private Path assignSidecar(BibEntry entry) {
        DirectoryLibraryCatalog catalog = files.catalog();
        Path sidecar = entry.getFiles().stream()
                            .filter(linkedFile -> !linkedFile.isOnlineLink())
                            .map(linkedFile -> root.resolve(linkedFile.getLink()).normalize())
                            .filter(linkedPath -> linkedPath.startsWith(root))
                            .findFirst()
                            .map(paired -> paired.resolveSibling(FileUtil.getBaseName(paired) + "." + MarkdownSidecar.MARKDOWN_EXTENSION))
                            // A second entry linking the same PDF, or a foreign file of that name, cannot share it
                            .filter(candidate -> !Files.exists(candidate) && catalog.entryIdsIn(candidate).isEmpty())
                            .orElseGet(() -> unusedSidecar(entry.getCitationKey()
                                                                .map(FileNameCleaner::cleanFileName)
                                                                .filter(not(String::isBlank))
                                                                .orElse("entry")));
        catalog.register(entry, sidecar, entry.getCitationKey().orElse(""));
        return sidecar;
    }

    /// Also skips names already assigned to entries whose sidecar is not written yet.
    private Path unusedSidecar(String baseName) {
        Path candidate = root.resolve(baseName + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
        for (int counter = 1; Files.exists(candidate) || !files.catalog().entryIdsIn(candidate).isEmpty(); counter++) {
            candidate = root.resolve(baseName + "-" + counter + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
        }
        return candidate;
    }

    /// Renames the sidecar and its equally named PDF to the base name the filename pattern
    /// generates for the entry — kept in sync as a pair, per the pairing convention. A pattern
    /// failure, or a target name any pair member of another entry already occupies, leaves the
    /// current name untouched. Never touches other files.
    private Path applyFileNamePattern(Path file, BibEntry entry) {
        return fileNameGenerator.apply(entry)
                                .map(String::trim)
                                .filter(not(String::isEmpty))
                                .filter(not(FileUtil.getBaseName(file)::equals))
                                .map(newBaseName -> renamePair(file, entry, newBaseName))
                                .orElse(file);
    }

    private Path renamePair(Path file, BibEntry entry, String newBaseName) {
        Path newSidecar = file.resolveSibling(newBaseName + "." + FileUtil.getFileExtension(file).orElseThrow());
        Path oldPdf = file.resolveSibling(FileUtil.getBaseName(file) + ".pdf");
        Path newPdf = file.resolveSibling(newBaseName + ".pdf");
        boolean occupied = (Files.exists(newPdf) && !linksFile(entry, newPdf))
                || DirectoryLibrarySynchronizer.SIDECAR_EXTENSIONS.stream()
                                                                  .anyMatch(extension -> Files.exists(file.resolveSibling(newBaseName + "." + extension)));
        if (occupied) {
            return file;
        }
        boolean hasPdf = Files.exists(oldPdf);
        try {
            // The PDF first: if that fails nothing has changed, and a failing sidecar move is
            // rolled back, so the pair never ends up half renamed
            if (hasPdf) {
                Files.move(oldPdf, newPdf);
            }
            try {
                if (Files.exists(file)) {
                    Files.move(file, newSidecar);
                }
            } catch (IOException e) {
                if (hasPdf) {
                    Files.move(newPdf, oldPdf);
                }
                throw e;
            }
        } catch (IOException e) {
            LOGGER.warn("Could not rename {} to the configured pattern", file, e);
            return file;
        }
        files.relocate(file, newSidecar);
        if (hasPdf) {
            String newLink = root.relativize(newPdf).toString();
            String oldLink = root.relativize(oldPdf).toString();
            modelUpdateMarshaller.accept(() -> {
                List<LinkedFile> updated = entry.getFiles().stream()
                                                .map(linkedFile -> oldLink.equals(linkedFile.getLink())
                                                                   ? new LinkedFile(linkedFile.getDescription(), newLink, linkedFile.getFileType())
                                                                   : linkedFile)
                                                .toList();
                entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(updated), EntriesEventSource.SHARED);
            });
        }
        return newSidecar;
    }

    private boolean linksFile(BibEntry entry, Path file) {
        return entry.getFiles().stream()
                    .filter(linkedFile -> !linkedFile.isOnlineLink())
                    .anyMatch(linkedFile -> root.resolve(linkedFile.getLink()).normalize().equals(file.toAbsolutePath().normalize()));
    }
}
