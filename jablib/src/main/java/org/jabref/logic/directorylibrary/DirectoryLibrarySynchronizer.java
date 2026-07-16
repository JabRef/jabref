package org.jabref.logic.directorylibrary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.HayagrivaImporter;
import org.jabref.logic.util.DirectoryMonitor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.StandardField;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileEntry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Keeps an open directory library in sync with external file changes (inbound direction:
/// file system to [BibDatabaseContext]). Registered as a [FileAlterationListener] with the
/// polling [DirectoryMonitor]; all event handling is serialized on a single "directory-sync"
/// executor, and model mutations are marshalled through the injected `modelUpdateMarshaller`
/// (the GUI passes the JavaFX thread executor).
///
/// All database mutations use [EntriesEventSource#SHARED] so that the future write-back
/// direction can ignore them (same echo-prevention policy as the shared-SQL synchronizer).
/// Conversely, [#recordWrittenFile] lets the write-back direction register a fingerprint of
/// its own writes, which this class then swallows instead of re-importing.
///
/// The file monitor reports renames as delete + create. Deletions are therefore staged for a
/// grace period spanning two poll cycles: a create whose parsed entries equal a staged
/// deletion's entries is treated as a move (the [BibEntry] instances survive, preserving
/// selection and undo history); only unmatched deletions are committed.
///
/// Sidecars come in two forms (see [MarkdownSidecar]): plain Hayagriva `.yml`/`.yaml` files and
/// Markdown `.md` files whose Hayagriva frontmatter carries the data; both are watched alike.
// [impl->req~directory-library.inbound-sync~2]
@NullMarked
public class DirectoryLibrarySynchronizer implements FileAlterationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryLibrarySynchronizer.class);

    /// Two poll cycles of [DirectoryMonitor] (1 s each), so a rename's create event can arrive
    /// in the poll cycle after its delete event.
    private static final Duration RENAME_GRACE = Duration.ofMillis(2500);

    private static final Set<String> SIDECAR_EXTENSIONS = Set.of("yml", "yaml", MarkdownSidecar.MARKDOWN_EXTENSION);
    private static final String PDF_EXTENSION = "pdf";

    private final BibDatabaseContext databaseContext;
    private final DirectoryLibraryCatalog catalog;
    private final PdfEntryFactory pdfEntryFactory;
    private final Path root;
    private final Consumer<Runnable> modelUpdateMarshaller;
    private final Clock clock;
    private final HayagrivaImporter importer = new HayagrivaImporter();
    private final MarkdownSidecar markdownSidecar = new MarkdownSidecar();
    private final ScheduledExecutorService syncExecutor;

    private final Map<Path, StagedDeletion> stagedDeletions = new HashMap<>();
    private final Map<Path, String> lastWrittenFingerprints = new HashMap<>();

    private @Nullable FileAlterationObserver observer;
    private @Nullable DirectoryMonitor directoryMonitor;

    private record StagedDeletion(List<BibEntry> entries, Instant expiry) {
    }

    public DirectoryLibrarySynchronizer(BibDatabaseContext databaseContext,
                                        DirectoryLibraryCatalog catalog,
                                        PdfEntryFactory pdfEntryFactory,
                                        Consumer<Runnable> modelUpdateMarshaller) {
        this(databaseContext, catalog, pdfEntryFactory, modelUpdateMarshaller, Clock.systemUTC());
    }

    DirectoryLibrarySynchronizer(BibDatabaseContext databaseContext,
                                 DirectoryLibraryCatalog catalog,
                                 PdfEntryFactory pdfEntryFactory,
                                 Consumer<Runnable> modelUpdateMarshaller,
                                 Clock clock) {
        this.databaseContext = databaseContext;
        this.catalog = catalog;
        this.pdfEntryFactory = pdfEntryFactory;
        this.root = databaseContext.getDirectoryLibraryRoot().orElseThrow(
                () -> new IllegalArgumentException("Context is not a directory library"));
        this.modelUpdateMarshaller = modelUpdateMarshaller;
        this.clock = clock;
        this.syncExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "directory-sync");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void startWatching(DirectoryMonitor monitor) {
        this.directoryMonitor = monitor;
        IOFileFilter relevantFiles = FileFilterUtils.or(
                FileFilterUtils.directoryFileFilter(),
                FileFilterUtils.suffixFileFilter(".yml", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".yaml", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".md", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".pdf", IOCase.INSENSITIVE));
        IOFileFilter notHidden = FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter("."));
        observer = FileAlterationObserver.builder()
                                         .setRootEntry(new FileEntry(root.toFile()))
                                         .setFileFilter(FileFilterUtils.and(notHidden, relevantFiles))
                                         .getUnchecked();
        // The monitor is already running and never initializes late-joining observers, so the
        // first poll would report every existing file as created. Checking once without any
        // listener attached takes the baseline snapshot silently.
        observer.checkAndNotify();
        monitor.addObserver(observer, this);
    }

    public void shutdown() {
        if (observer != null && directoryMonitor != null) {
            directoryMonitor.removeObserver(observer);
        }
        syncExecutor.shutdown();
    }

    /// Registers the fingerprint of a file this application just wrote itself, so the next
    /// change event for it is recognized as a self-echo and not re-imported. Consumed on match.
    public synchronized void recordWrittenFile(Path file, byte[] content) {
        lastWrittenFingerprints.put(file.toAbsolutePath().normalize(), hash(content));
    }

    @Override
    public void onFileCreate(File file) {
        syncExecutor.execute(() -> handleFileCreated(file.toPath()));
    }

    @Override
    public void onFileChange(File file) {
        syncExecutor.execute(() -> handleFileChanged(file.toPath()));
    }

    @Override
    public void onFileDelete(File file) {
        syncExecutor.execute(() -> handleFileDeleted(file.toPath()));
    }

    @Override
    public void onDirectoryCreate(File directory) {
        // files inside are reported individually
    }

    @Override
    public void onDirectoryChange(File directory) {
        // files inside are reported individually
    }

    @Override
    public void onDirectoryDelete(File directory) {
        // files inside are reported individually
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
        // no bookkeeping per scan round needed
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
        syncExecutor.execute(this::commitExpiredStagedDeletions);
    }

    synchronized void handleFileCreated(Path file) {
        commitExpiredStagedDeletions();
        if (isSidecar(file)) {
            if (consumeSelfEcho(file)) {
                return;
            }
            importFile(file);
        } else if (isPdf(file)) {
            handlePdfCreated(file);
        }
    }

    synchronized void handleFileChanged(Path file) {
        commitExpiredStagedDeletions();
        if (!isSidecar(file) || consumeSelfEcho(file)) {
            return;
        }
        List<BibEntry> knownEntries = entriesOf(file);
        if (knownEntries.isEmpty()) {
            importFile(file);
            return;
        }
        if (!looksLikeSidecar(file)) {
            // The file stopped being a sidecar (e.g. replaced by unrelated YAML or Markdown)
            removeEntries(knownEntries, file);
            return;
        }
        Optional<List<BibEntry>> parsed = parse(file);
        if (parsed.isEmpty()) {
            LOGGER.warn("Not applying changes of unparseable Hayagriva file {}", file);
            return;
        }
        applyChangedFile(file, knownEntries, parsed.get());
    }

    synchronized void handleFileDeleted(Path file) {
        commitExpiredStagedDeletions();
        if (isSidecar(file)) {
            List<BibEntry> entries = entriesOf(file);
            if (entries.isEmpty()) {
                return;
            }
            stagedDeletions.put(file, new StagedDeletion(entries, clock.instant().plus(RENAME_GRACE)));
            syncExecutor.schedule(this::commitExpiredStagedDeletions,
                    RENAME_GRACE.toMillis() + 100, TimeUnit.MILLISECONDS);
        } else if (isPdf(file)) {
            handlePdfDeleted(file);
        }
    }

    synchronized void commitExpiredStagedDeletions() {
        Instant now = clock.instant();
        List<Map.Entry<Path, StagedDeletion>> expired = stagedDeletions.entrySet().stream()
                                                                       .filter(staged -> !staged.getValue().expiry().isAfter(now))
                                                                       .toList();
        for (Map.Entry<Path, StagedDeletion> staged : expired) {
            stagedDeletions.remove(staged.getKey());
            removeEntries(staged.getValue().entries(), staged.getKey());
        }
    }

    private void importFile(Path file) {
        if (!entriesOf(file).isEmpty()) {
            // Already known (e.g. a create event for a file the scan covered) — diff instead
            handleFileChanged(file);
            return;
        }
        if (!looksLikeSidecar(file)) {
            return;
        }
        Optional<List<BibEntry>> parsed = parse(file);
        if (parsed.isEmpty() || parsed.get().isEmpty()) {
            return;
        }
        List<BibEntry> newEntries = parsed.get();

        // A staged deletion with equal content is this file being moved, not new content
        Optional<Path> movedFrom = stagedDeletions.entrySet().stream()
                                                  .filter(staged -> entriesMatch(staged.getValue().entries(), newEntries))
                                                  .map(Map.Entry::getKey)
                                                  .findFirst();
        if (movedFrom.isPresent()) {
            stagedDeletions.remove(movedFrom.get());
            catalog.relocateFile(movedFrom.get(), file);
            LOGGER.debug("Detected move {} -> {}", movedFrom.get(), file);
            return;
        }

        newEntries.forEach(entry -> catalog.register(entry, file, entry.getCitationKey().orElse("")));
        // Safe without event source: the entry is not yet inserted, so no listeners see this
        findPairedPdf(file).ifPresent(pdf -> newEntries.getFirst()
                                                       .addFile(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName())));
        modelUpdateMarshaller.accept(() ->
                databaseContext.getDatabase().insertEntries(newEntries, EntriesEventSource.SHARED));
    }

    private void applyChangedFile(Path file, List<BibEntry> knownEntries, List<BibEntry> parsedEntries) {
        SequencedMap<String, BibEntry> knownByKey = byCitationKey(knownEntries);
        SequencedMap<String, BibEntry> parsedByKey = byCitationKey(parsedEntries);

        List<BibEntry> toInsert = new ArrayList<>();
        List<BibEntry> toRemove = new ArrayList<>();
        List<Runnable> fieldUpdates = new ArrayList<>();

        parsedByKey.forEach((key, parsedEntry) -> {
            BibEntry knownEntry = knownByKey.get(key);
            if (knownEntry == null) {
                catalog.register(parsedEntry, file, key);
                toInsert.add(parsedEntry);
            } else {
                fieldUpdates.add(() -> copyContent(parsedEntry, knownEntry));
            }
        });
        knownByKey.forEach((key, knownEntry) -> {
            if (!parsedByKey.containsKey(key)) {
                toRemove.add(knownEntry);
            }
        });

        modelUpdateMarshaller.accept(() -> {
            fieldUpdates.forEach(Runnable::run);
            if (!toInsert.isEmpty()) {
                databaseContext.getDatabase().insertEntries(toInsert, EntriesEventSource.SHARED);
            }
            if (!toRemove.isEmpty()) {
                databaseContext.getDatabase().removeEntries(toRemove, EntriesEventSource.SHARED);
            }
        });
        catalog.removeFile(file);
        parsedByKey.forEach((key, parsedEntry) -> {
            BibEntry target = knownByKey.getOrDefault(key, parsedEntry);
            catalog.register(target, file, key);
        });
    }

    /// Applies `source`'s type and fields onto `target` without replacing the instance, so
    /// selection, undo history, and group membership survive external edits.
    private void copyContent(BibEntry source, BibEntry target) {
        if (!target.getType().equals(source.getType())) {
            target.setType(source.getType(), EntriesEventSource.SHARED);
        }
        // The PDF link is maintained by this synchronizer, not by the file content
        Optional<String> preservedFiles = target.getField(StandardField.FILE);
        source.getFields().forEach(field ->
                source.getField(field).ifPresent(value -> target.setField(field, value, EntriesEventSource.SHARED)));
        target.getFields().stream()
              .filter(field -> StandardField.FILE != field)
              .filter(field -> source.getField(field).isEmpty())
              .toList()
              .forEach(field -> target.clearField(field, EntriesEventSource.SHARED));
        preservedFiles.ifPresent(files -> target.setField(StandardField.FILE, files, EntriesEventSource.SHARED));
    }

    private void handlePdfCreated(Path pdf) {
        Optional<BibEntry> sidecarEntry = findSidecarEntry(pdf);
        if (sidecarEntry.isPresent()) {
            BibEntry entry = sidecarEntry.get();
            if (entry.getFiles().isEmpty()) {
                List<LinkedFile> files = List.of(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName()));
                modelUpdateMarshaller.accept(() ->
                        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(files), EntriesEventSource.SHARED));
            }
            return;
        }
        BibEntry entry = pdfEntryFactory.createEntry(pdf, root, databaseContext);
        modelUpdateMarshaller.accept(() -> {
            databaseContext.getDatabase().insertEntries(List.of(entry), EntriesEventSource.SHARED);
            pdfEntryFactory.generateCitationKeyIfMissing(entry, databaseContext);
        });
    }

    private void handlePdfDeleted(Path pdf) {
        String relativeLink = root.relativize(pdf).toString();
        List<BibEntry> linking = databaseContext.getDatabase().getEntries().stream()
                                                .filter(entry -> entry.getFiles().stream()
                                                                      .anyMatch(linked -> relativeLink.equals(linked.getLink())))
                                                .toList();
        for (BibEntry entry : linking) {
            boolean isStub = catalog.sourceOf(entry).isEmpty();
            modelUpdateMarshaller.accept(() -> {
                if (isStub) {
                    databaseContext.getDatabase().removeEntries(List.of(entry), EntriesEventSource.SHARED);
                } else {
                    List<LinkedFile> remaining = entry.getFiles().stream()
                                                      .filter(linked -> !relativeLink.equals(linked.getLink()))
                                                      .toList();
                    entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(remaining), EntriesEventSource.SHARED);
                }
            });
        }
    }

    private void removeEntries(List<BibEntry> entries, Path file) {
        catalog.removeFile(file);
        modelUpdateMarshaller.accept(() ->
                databaseContext.getDatabase().removeEntries(entries, EntriesEventSource.SHARED));
    }

    private List<BibEntry> entriesOf(Path file) {
        List<String> ids = catalog.entryIdsIn(file);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, BibEntry> byId = new HashMap<>();
        databaseContext.getDatabase().getEntries().forEach(entry -> byId.put(entry.getId(), entry));
        return ids.stream().flatMap(id -> Optional.ofNullable(byId.get(id)).stream()).toList();
    }

    private static SequencedMap<String, BibEntry> byCitationKey(List<BibEntry> entries) {
        SequencedMap<String, BibEntry> byKey = new LinkedHashMap<>();
        entries.forEach(entry -> byKey.putIfAbsent(entry.getCitationKey().orElse(""), entry));
        return byKey;
    }

    private static boolean entriesMatch(List<BibEntry> staged, List<BibEntry> parsed) {
        if (staged.size() != parsed.size()) {
            return false;
        }
        for (int i = 0; i < staged.size(); i++) {
            if (!staged.get(i).equals(parsed.get(i))) {
                return false;
            }
        }
        return true;
    }

    private Optional<List<BibEntry>> parse(Path file) {
        try {
            ParserResult parserResult = MarkdownSidecar.hasMarkdownExtension(file)
                                        ? markdownSidecar.read(file)
                                        : importer.importDatabase(file);
            if (parserResult.isInvalid()) {
                return Optional.empty();
            }
            return Optional.of(parserResult.getDatabase().getEntries());
        } catch (IOException e) {
            LOGGER.warn("Could not read {}", file, e);
            return Optional.empty();
        }
    }

    private boolean looksLikeSidecar(Path file) {
        try {
            if (MarkdownSidecar.hasMarkdownExtension(file)) {
                return markdownSidecar.looksLikeSidecar(file);
            }
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return importer.isRecognizedFormat(reader);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read {}", file, e);
            return false;
        }
    }

    private Optional<BibEntry> findSidecarEntry(Path pdf) {
        Path parent = pdf.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        String baseName = FileUtil.getBaseName(pdf);
        for (String extension : SIDECAR_EXTENSIONS) {
            List<BibEntry> entries = entriesOf(parent.resolve(baseName + "." + extension));
            if (!entries.isEmpty()) {
                return Optional.of(entries.getFirst());
            }
        }
        return Optional.empty();
    }

    private Optional<Path> findPairedPdf(Path yamlFile) {
        Path parent = yamlFile.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        Path pdf = parent.resolve(FileUtil.getBaseName(yamlFile) + ".pdf");
        return Files.exists(pdf) ? Optional.of(pdf) : Optional.empty();
    }

    private boolean consumeSelfEcho(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        String recorded = lastWrittenFingerprints.get(normalized);
        if (recorded == null) {
            return false;
        }
        try {
            String current = hash(Files.readAllBytes(file));
            if (recorded.equals(current)) {
                lastWrittenFingerprints.remove(normalized);
                return true;
            }
        } catch (IOException e) {
            LOGGER.debug("Could not fingerprint {}", file, e);
        }
        return false;
    }

    private static String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is guaranteed to be available", e);
        }
    }

    private static boolean isSidecar(Path file) {
        return SIDECAR_EXTENSIONS.contains(FileUtil.getFileExtension(file).orElse("").toLowerCase(Locale.ROOT));
    }

    private static boolean isPdf(Path file) {
        return PDF_EXTENSION.equals(FileUtil.getFileExtension(file).orElse("").toLowerCase(Locale.ROOT));
    }
}
