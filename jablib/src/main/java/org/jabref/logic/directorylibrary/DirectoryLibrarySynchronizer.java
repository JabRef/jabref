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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.exporter.AtomicFileOutputStream;
import org.jabref.logic.exporter.HayagrivaEntryWriter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.HayagrivaImporter;
import org.jabref.logic.util.DirectoryMonitor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileNameCleaner;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;
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
import tools.jackson.core.JacksonException;

import static java.util.function.Predicate.not;

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
///
/// The outbound direction subscribes to entry events (relayed through the
/// [org.jabref.logic.util.CoarseChangeFilter] installed by
/// [BibDatabaseContext#attachDirectorySynchronizer]) and persists user changes back into the
/// sidecar files: edits rewrite the entry's file read-modify-write, the first user edit of an
/// entry without a sidecar creates one (next to its PDF, sharing the base name), a citation-key
/// edit renames the YAML map key, and deleting an entry removes it from its file (disposing the
/// file once its last entry is gone — the paired PDF is never touched). Writes are debounced
/// per file; [#flush] forces them, and shutdown flushes implicitly. A file that could not be
/// written stays pending and is reported by [#flush], so the GUI can tell the user.
// [impl->req~directory-library.inbound-sync~2]
// [impl->req~directory-library.write-back~2]
@NullMarked
public class DirectoryLibrarySynchronizer implements FileAlterationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryLibrarySynchronizer.class);

    /// Two poll cycles of [DirectoryMonitor], so a rename's create event can arrive in the poll
    /// cycle after its delete event.
    private static final Duration RENAME_GRACE = DirectoryMonitor.POLL_INTERVAL.multipliedBy(2).plusMillis(500);

    /// In precedence order when several sidecars share a base name.
    private static final List<String> SIDECAR_EXTENSIONS = List.of("yml", "yaml", MarkdownSidecar.MARKDOWN_EXTENSION);
    private static final String PDF_EXTENSION = "pdf";

    /// Collects keystroke-level bursts into one write per file. Trailing edge: every change
    /// event re-arms the file's timer, so the write fires once typing pauses and always
    /// persists the latest state.
    private static final Duration WRITE_DEBOUNCE = Duration.ofMillis(500);

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
    /// Content of each sidecar as last read or written, so a write notices an external edit
    /// that landed in between and takes it into the model first instead of overwriting it.
    private final Map<Path, String> lastSeenFingerprints = new HashMap<>();
    /// Entries (by Hayagriva key) as last read from or written to each file: the base of the
    /// three-way merge in [#applyChangedFile], so an external edit only touches the fields it
    /// changed and in-memory edits of other fields survive.
    private final Map<Path, SequencedMap<String, BibEntry>> baselines = new HashMap<>();
    private final HayagrivaEntryWriter entryWriter = new HayagrivaEntryWriter();
    private final SequencedSet<Path> dirtyFiles = new LinkedHashSet<>();
    private final Map<Path, ScheduledFuture<?>> scheduledWrites = new HashMap<>();
    private final Consumer<Path> fileDisposer;
    private final Function<BibEntry, Optional<String>> fileNameGenerator;

    private @Nullable Watch watch;

    private record StagedDeletion(List<BibEntry> entries, Instant expiry) {
    }

    private record Watch(DirectoryMonitor monitor, FileAlterationObserver observer) {
    }

    public DirectoryLibrarySynchronizer(BibDatabaseContext databaseContext,
                                        DirectoryLibraryCatalog catalog,
                                        PdfEntryFactory pdfEntryFactory,
                                        Consumer<Path> fileDisposer,
                                        Function<BibEntry, Optional<String>> fileNameGenerator,
                                        Consumer<Runnable> modelUpdateMarshaller) {
        this(databaseContext, catalog, pdfEntryFactory, fileDisposer, fileNameGenerator, modelUpdateMarshaller, Clock.systemUTC());
    }

    DirectoryLibrarySynchronizer(BibDatabaseContext databaseContext,
                                 DirectoryLibraryCatalog catalog,
                                 PdfEntryFactory pdfEntryFactory,
                                 Consumer<Path> fileDisposer,
                                 Function<BibEntry, Optional<String>> fileNameGenerator,
                                 Consumer<Runnable> modelUpdateMarshaller,
                                 Clock clock) {
        this.databaseContext = databaseContext;
        this.catalog = catalog;
        this.pdfEntryFactory = pdfEntryFactory;
        this.fileDisposer = fileDisposer;
        this.fileNameGenerator = fileNameGenerator;
        this.root = databaseContext.getDirectoryLibraryRoot().orElseThrow(
                () -> new IllegalArgumentException("Context is not a directory library"));
        this.modelUpdateMarshaller = modelUpdateMarshaller;
        this.clock = clock;
        // A dedicated single thread (not BackgroundTask: events must be serialized and writes
        // debounced). Events polled while this synchronizer shuts down are dropped instead of
        // throwing into the shared monitor thread.
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1,
                Thread.ofPlatform().name("directory-sync").daemon(true).factory());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        // Pending debounce and grace timers are superseded by the final flush on shutdown
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.syncExecutor = executor;
    }

    public void startWatching(DirectoryMonitor monitor) {
        IOFileFilter relevantFiles = FileFilterUtils.or(
                FileFilterUtils.directoryFileFilter(),
                FileFilterUtils.suffixFileFilter(".yml", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".yaml", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".md", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".pdf", IOCase.INSENSITIVE));
        IOFileFilter notHidden = FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter("."));
        FileAlterationObserver observer = FileAlterationObserver.builder()
                                                                .setRootEntry(new FileEntry(root.toFile()))
                                                                .setFileFilter(FileFilterUtils.and(notHidden, relevantFiles))
                                                                .getUnchecked();
        watch = new Watch(monitor, observer);
        // The monitor is already running and never initializes late-joining observers, so the
        // first poll would report every existing file as created. Checking once without any
        // listener attached takes the baseline snapshot silently — off the caller's thread,
        // since it walks the whole tree.
        syncExecutor.execute(() -> {
            takeBaseline();
            observer.checkAndNotify();
            monitor.addObserver(observer, this);
        });
    }

    /// Records the scanned files' content as the merge base; the live entries still equal it
    /// at this point.
    synchronized void takeBaseline() {
        for (Path file : catalog.files()) {
            currentHash(file).ifPresent(fingerprint -> lastSeenFingerprints.put(file.toAbsolutePath().normalize(), fingerprint));
            baselines.put(file, copiesByKey(entriesOf(file)));
        }
    }

    /// The sidecar an entry is written to (tests).
    Path sidecarOf(BibEntry entry) {
        return catalog.sourceOf(entry).map(DirectoryLibraryCatalog.EntrySource::yamlFile).orElseThrow();
    }

    /// Waits until every event queued so far has been handled (tests).
    void awaitPendingEvents() throws InterruptedException, ExecutionException {
        syncExecutor.submit(() -> { }).get();
    }

    /// Stops watching and writes what is still pending. Events already queued (the last
    /// keystroke) are drained first, so the final flush sees every change.
    ///
    /// @return the files whose changes could not be written
    public List<Path> shutdown() {
        Optional.ofNullable(watch).ifPresent(active -> active.monitor().removeObserver(active.observer()));
        syncExecutor.shutdown();
        try {
            syncExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return flush();
    }

    /// Writes all pending sidecar changes now (they are otherwise debounced).
    ///
    /// @return the files whose changes could not be written; they stay pending
    public synchronized List<Path> flush() {
        scheduledWrites.values().forEach(pending -> pending.cancel(false));
        scheduledWrites.clear();
        return writeFiles(List.copyOf(dirtyFiles), true);
    }

    /// Registers the fingerprint of a file this application just wrote itself, so the next
    /// change event for it is recognized as a self-echo and not re-imported. Consumed on match.
    public synchronized void recordWrittenFile(Path file, byte[] content) {
        String fingerprint = hash(content);
        lastWrittenFingerprints.put(file.toAbsolutePath().normalize(), fingerprint);
        lastSeenFingerprints.put(file.toAbsolutePath().normalize(), fingerprint);
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

    @Subscribe
    public void listen(EntryChangedEvent event) {
        if (!isUserChange(event)) {
            return;
        }
        // Events the CoarseChangeFilter marks as filtered (the keystrokes of a typing burst)
        // still re-arm the debounce: the write captures the entry's state at fire time, so the
        // tail of a burst — which produces only filtered events — is never lost.
        BibEntry entry = event.getBibEntry();
        syncExecutor.execute(() -> handleLocalChange(entry));
    }

    @Subscribe
    public void listen(EntriesAddedEvent event) {
        if (!isUserChange(event)) {
            return;
        }
        List<BibEntry> entries = List.copyOf(event.getBibEntries());
        syncExecutor.execute(() -> entries.forEach(this::handleLocalChange));
    }

    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        if (!isUserChange(event)) {
            return;
        }
        List<BibEntry> entries = List.copyOf(event.getBibEntries());
        syncExecutor.execute(() -> handleLocalRemoval(entries));
    }

    private static boolean isUserChange(EntriesEvent event) {
        return event.getEntriesEventSource() == EntriesEventSource.LOCAL
                || event.getEntriesEventSource() == EntriesEventSource.UNDO;
    }

    synchronized void handleLocalChange(BibEntry entry) {
        scheduleWrite(catalog.sourceOf(entry)
                             .map(DirectoryLibraryCatalog.EntrySource::yamlFile)
                             .orElseGet(() -> assignSidecar(entry)));
    }

    /// The catalog keeps the removed entries' sources until the debounced write runs, so an
    /// undo within that window lands the entry back in its own file instead of a fresh one.
    synchronized void handleLocalRemoval(List<BibEntry> entries) {
        entries.stream()
               .flatMap(entry -> catalog.sourceOf(entry).stream())
               .map(DirectoryLibraryCatalog.EntrySource::yamlFile)
               .distinct()
               .forEach(this::scheduleWrite);
    }

    /// The first user change of an entry without a source materializes its sidecar — a Markdown
    /// sidecar (see [MarkdownSidecar]): next to the entry's PDF (sharing the base name, per the
    /// pairing convention), or named after the citation key for entries without a file.
    private Path assignSidecar(BibEntry entry) {
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
        for (int counter = 1; Files.exists(candidate) || !catalog.entryIdsIn(candidate).isEmpty(); counter++) {
            candidate = root.resolve(baseName + "-" + counter + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
        }
        return candidate;
    }

    private synchronized void scheduleWrite(Path file) {
        dirtyFiles.add(file);
        Optional.ofNullable(scheduledWrites.remove(file)).ifPresent(pending -> pending.cancel(false));
        if (syncExecutor.isShutdown()) {
            // Written by the final flush
            return;
        }
        scheduledWrites.put(file, syncExecutor.schedule(() -> writeScheduled(file), WRITE_DEBOUNCE.toMillis(), TimeUnit.MILLISECONDS));
    }

    private synchronized void writeScheduled(Path file) {
        scheduledWrites.remove(file);
        writeFiles(List.of(file), false);
    }

    /// Files that could not be written stay dirty: the next flush retries them and the caller
    /// can report them. `immediate` writes even if the file changed externally in between (the
    /// external edit has then been taken into the model on the caller's thread, see
    /// [#writeFile]).
    private synchronized List<Path> writeFiles(List<Path> files, boolean immediate) {
        List<Path> failed = new ArrayList<>();
        for (Path file : files) {
            if (!dirtyFiles.contains(file)) {
                continue;
            }
            try {
                if (writeFile(file, immediate)) {
                    dirtyFiles.remove(file);
                } else {
                    scheduleWrite(file);
                }
            } catch (IOException | JacksonException e) {
                LOGGER.error("Could not write sidecar {}", file, e);
                failed.add(file);
            }
        }
        return failed;
    }

    /// @return whether the file was written; `false` defers the write until the model has taken
    /// in an external edit that landed since the file was last read or written
    private boolean writeFile(Path file, boolean immediate) throws IOException {
        Path normalized = file.toAbsolutePath().normalize();
        boolean changedExternally = Optional.ofNullable(lastSeenFingerprints.get(normalized))
                                            .map(lastSeen -> Files.exists(file) && !currentHash(file).equals(Optional.of(lastSeen)))
                                            .orElse(false);
        if (changedExternally) {
            // The model update is marshalled (asynchronously in the GUI), so the write is retried
            // one debounce later — unless the caller flushes, where the user's state must win
            handleFileChanged(file);
            if (!immediate) {
                return false;
            }
        }

        List<BibEntry> entries = entriesOf(file);
        Set<String> liveIds = entries.stream().map(BibEntry::getId).collect(Collectors.toSet());
        catalog.entryIdsIn(file).stream().filter(id -> !liveIds.contains(id)).forEach(catalog::removeEntry);
        if (entries.isEmpty()) {
            catalog.removeFile(file);
            lastSeenFingerprints.remove(normalized);
            baselines.remove(file);
            if (Files.exists(file)) {
                fileDisposer.accept(file);
            }
            return true;
        }
        // The user's rename rule: a single-entry sidecar and its paired PDF share the base name
        // generated by the configured filename pattern; multi-entry files have no single
        // generating entry and keep their name
        Path target = entries.size() == 1 ? applyFileNamePattern(file, entries.getFirst()) : file;
        List<HayagrivaEntryWriter.KeyedEntry> keyedEntries = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();
        for (BibEntry entry : entries) {
            String previousKey = catalog.sourceOf(entry)
                                        .map(DirectoryLibraryCatalog.EntrySource::hayagrivaKey)
                                        .orElse("");
            String targetKey = entry.getCitationKey()
                                    .filter(key -> !key.isBlank())
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
        recordWrittenFile(target, content);
        keyedEntries.forEach(keyedEntry -> catalog.updateHayagrivaKey(keyedEntry.entry(), keyedEntry.targetKey()));
        SequencedMap<String, BibEntry> written = new LinkedHashMap<>();
        keyedEntries.forEach(keyedEntry -> written.put(keyedEntry.targetKey(), new BibEntry(keyedEntry.entry())));
        baselines.put(target, written);
        return true;
    }

    /// Renames the sidecar and its equally named PDF to the base name the filename pattern
    /// generates for the entry — kept in sync as a pair, per the pairing convention. A pattern
    /// failure, or a target name any pair member of another entry already occupies, leaves the
    /// current name untouched. Never touches other files.
    // [impl->req~directory-library.pattern-rename~1]
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
                || SIDECAR_EXTENSIONS.stream().anyMatch(extension -> Files.exists(file.resolveSibling(newBaseName + "." + extension)));
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
        catalog.relocateFile(file, newSidecar);
        Optional.ofNullable(baselines.remove(file)).ifPresent(baseline -> baselines.put(newSidecar, baseline));
        Optional.ofNullable(lastSeenFingerprints.remove(file.toAbsolutePath().normalize()))
                .ifPresent(fingerprint -> lastSeenFingerprints.put(newSidecar.toAbsolutePath().normalize(), fingerprint));
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
            // The file stopped being a sidecar — or an editor that truncates and rewrites was
            // polled mid-write, so the entries are only staged: a complete sidecar arriving
            // within the grace window keeps them
            stageDeletion(file, knownEntries);
            return;
        }
        parse(file).ifPresentOrElse(parsedEntries -> {
            stagedDeletions.remove(file);
            applyChangedFile(file, knownEntries, parsedEntries);
        }, () -> LOGGER.warn("Not applying changes of unparseable Hayagriva file {}", file));
    }

    synchronized void handleFileDeleted(Path file) {
        commitExpiredStagedDeletions();
        if (isSidecar(file)) {
            List<BibEntry> entries = entriesOf(file);
            if (entries.isEmpty()) {
                return;
            }
            stageDeletion(file, entries);
        } else if (isPdf(file)) {
            handlePdfDeleted(file);
        }
    }

    private void stageDeletion(Path file, List<BibEntry> entries) {
        stagedDeletions.put(file, new StagedDeletion(entries, clock.instant().plus(RENAME_GRACE)));
        syncExecutor.schedule(this::commitExpiredStagedDeletions, RENAME_GRACE.toMillis() + 100, TimeUnit.MILLISECONDS);
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
            // Already known: a create event for a file the scan covered, or a deletion undone
            // within the grace window — diff instead of importing twice
            stagedDeletions.remove(file);
            handleFileChanged(file);
            return;
        }
        if (!looksLikeSidecar(file)) {
            return;
        }
        List<BibEntry> newEntries = parse(file).orElse(List.of());
        if (newEntries.isEmpty()) {
            return;
        }

        // A staged deletion with equal content is this file being moved, not new content
        stagedDeletions.entrySet().stream()
                       .filter(staged -> entriesMatch(staged.getValue().entries(), newEntries))
                       .map(Map.Entry::getKey)
                       .findFirst()
                       .ifPresentOrElse(movedFrom -> {
                           stagedDeletions.remove(movedFrom);
                           catalog.relocateFile(movedFrom, file);
                           Optional.ofNullable(baselines.remove(movedFrom)).ifPresent(baseline -> baselines.put(file, baseline));
                           modelUpdateMarshaller.accept(this::refreshGroupsView);
                           LOGGER.debug("Detected move {} -> {}", movedFrom, file);
                       }, () -> insertNewEntries(file, newEntries));
    }

    private void insertNewEntries(Path file, List<BibEntry> newEntries) {
        newEntries.forEach(entry -> catalog.register(entry, file, entry.getCitationKey().orElseThrow()));
        // Safe without event source: the entry is not yet inserted, so no listeners see this
        findPairedPdf(file).ifPresent(pdf -> newEntries.getFirst()
                                                       .addFile(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName())));
        modelUpdateMarshaller.accept(() -> {
            databaseContext.getDatabase().insertEntries(newEntries, EntriesEventSource.SHARED);
            refreshGroupsView();
        });
    }

    private void applyChangedFile(Path file, List<BibEntry> knownEntries, List<BibEntry> parsedEntries) {
        SequencedMap<String, BibEntry> knownByKey = byCitationKey(knownEntries);
        SequencedMap<String, BibEntry> parsedByKey = byCitationKey(parsedEntries);

        List<BibEntry> toInsert = new ArrayList<>();
        List<BibEntry> toRemove = new ArrayList<>();
        List<Runnable> fieldUpdates = new ArrayList<>();

        Map<String, BibEntry> baseline = baselines.getOrDefault(file, new LinkedHashMap<>());
        parsedByKey.forEach((key, parsedEntry) ->
                Optional.ofNullable(knownByKey.get(key)).ifPresentOrElse(
                        knownEntry -> fieldUpdates.add(() -> copyContent(parsedEntry, knownEntry, Optional.ofNullable(baseline.get(key)))),
                        () -> {
                            catalog.register(parsedEntry, file, key);
                            toInsert.add(parsedEntry);
                        }));
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
        baselines.put(file, copiesByKey(parsedEntries));
    }

    /// Applies what changed on disk (`source` versus `base`) onto `target` without replacing
    /// the instance, so selection, undo history, group membership — and in-memory edits of
    /// other fields — survive external edits. Without a base, the file wins entirely.
    private void copyContent(BibEntry source, BibEntry target, Optional<BibEntry> base) {
        boolean typeChangedOnDisk = base.map(BibEntry::getType).map(type -> !type.equals(source.getType())).orElse(true);
        if (typeChangedOnDisk && !target.getType().equals(source.getType())) {
            target.setType(source.getType(), EntriesEventSource.SHARED);
        }
        // The PDF link is maintained by this synchronizer, not by the file content
        Set<Field> candidates = new LinkedHashSet<>(source.getFields());
        base.ifPresentOrElse(baseEntry -> candidates.addAll(baseEntry.getFields()), () -> candidates.addAll(target.getFields()));
        candidates.remove(StandardField.FILE);
        for (Field field : candidates) {
            Optional<String> onDisk = source.getField(field);
            boolean unchangedOnDisk = base.map(baseEntry -> baseEntry.getField(field).equals(onDisk)).orElse(false);
            if (unchangedOnDisk) {
                continue;
            }
            onDisk.ifPresentOrElse(value -> target.setField(field, value, EntriesEventSource.SHARED),
                    () -> target.clearField(field, EntriesEventSource.SHARED));
        }
    }

    private static SequencedMap<String, BibEntry> copiesByKey(List<BibEntry> entries) {
        SequencedMap<String, BibEntry> copies = new LinkedHashMap<>();
        entries.forEach(entry -> copies.putIfAbsent(entry.getCitationKey().orElse(""), new BibEntry(entry)));
        return copies;
    }

    private void handlePdfCreated(Path pdf) {
        findSidecarEntry(pdf).ifPresentOrElse(entry -> {
            if (entry.getFiles().isEmpty()) {
                List<LinkedFile> files = List.of(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName()));
                modelUpdateMarshaller.accept(() ->
                        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(files), EntriesEventSource.SHARED));
            }
        }, () -> {
            BibEntry stub = pdfEntryFactory.createStub(pdf, root);
            modelUpdateMarshaller.accept(() -> {
                databaseContext.getDatabase().insertEntries(List.of(stub), EntriesEventSource.SHARED);
                refreshGroupsView();
            });
            // Metadata extraction may hit the network, so it runs without this synchronizer's
            // monitor: flush and shutdown on the UI thread must never wait for it
            syncExecutor.execute(() -> enrichStub(stub, pdf));
        });
    }

    private void enrichStub(BibEntry stub, Path pdf) {
        Optional<BibEntry> extracted = pdfEntryFactory.extractMetadata(pdf, databaseContext);
        modelUpdateMarshaller.accept(() -> {
            extracted.ifPresent(metadata -> pdfEntryFactory.applyExtractedMetadata(metadata, stub));
            pdfEntryFactory.generateCitationKeyIfMissing(stub, databaseContext);
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
                    refreshGroupsView();
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
        baselines.remove(file);
        modelUpdateMarshaller.accept(() -> {
            databaseContext.getDatabase().removeEntries(entries, EntriesEventSource.SHARED);
            refreshGroupsView();
        });
    }

    /// The directory-structure group materializes its subgroups from the entries; after
    /// structural changes the groups panel must recompute (TexGroup precedent).
    private void refreshGroupsView() {
        databaseContext.getMetaData().groupsBinding().invalidate();
    }

    /// Only entries still in the database: removed entries stay cataloged until their file is
    /// rewritten (see [#handleLocalRemoval]).
    private List<BibEntry> entriesOf(Path file) {
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

    private static SequencedMap<String, BibEntry> byCitationKey(List<BibEntry> entries) {
        SequencedMap<String, BibEntry> byKey = new LinkedHashMap<>();
        entries.forEach(entry -> byKey.putIfAbsent(entry.getCitationKey().orElse(""), entry));
        return byKey;
    }

    private static boolean entriesMatch(List<BibEntry> staged, List<BibEntry> parsed) {
        return staged.size() == parsed.size()
                && IntStream.range(0, staged.size()).allMatch(i -> sameContent(staged.get(i), parsed.get(i)));
    }

    /// Live entries carry the PDF link this synchronizer maintains; freshly parsed ones do not.
    private static boolean sameContent(BibEntry live, BibEntry parsed) {
        return live.getType().equals(parsed.getType()) && fieldsWithoutFile(live).equals(fieldsWithoutFile(parsed));
    }

    private static Map<Field, String> fieldsWithoutFile(BibEntry entry) {
        return entry.getFieldMap().entrySet().stream()
                    .filter(field -> StandardField.FILE != field.getKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Optional<List<BibEntry>> parse(Path file) {
        try {
            ParserResult parserResult = MarkdownSidecar.hasMarkdownExtension(file)
                                        ? markdownSidecar.read(file)
                                        : importer.importDatabase(file);
            if (parserResult.isInvalid()) {
                return Optional.empty();
            }
            currentHash(file).ifPresent(fingerprint -> lastSeenFingerprints.put(file.toAbsolutePath().normalize(), fingerprint));
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
        String baseName = FileUtil.getBaseName(pdf);
        return SIDECAR_EXTENSIONS.stream()
                                 .map(extension -> entriesOf(pdf.resolveSibling(baseName + "." + extension)))
                                 .filter(entries -> !entries.isEmpty())
                                 .map(List::getFirst)
                                 .findFirst();
    }

    private Optional<Path> findPairedPdf(Path yamlFile) {
        return Optional.of(yamlFile.resolveSibling(FileUtil.getBaseName(yamlFile) + ".pdf")).filter(Files::exists);
    }

    private boolean consumeSelfEcho(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        if (!lastWrittenFingerprints.containsKey(normalized)) {
            return false;
        }
        return currentHash(file).map(current -> lastWrittenFingerprints.remove(normalized, current)).orElse(false);
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

    private static boolean isSidecar(Path file) {
        return SIDECAR_EXTENSIONS.contains(FileUtil.getFileExtension(file).orElse("").toLowerCase(Locale.ROOT));
    }

    private static boolean isPdf(Path file) {
        return PDF_EXTENSION.equals(FileUtil.getFileExtension(file).orElse("").toLowerCase(Locale.ROOT));
    }
}
