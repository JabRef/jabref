package org.jabref.logic.directorylibrary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.HayagrivaImporter;
import org.jabref.logic.util.DirectoryMonitor;
import org.jabref.logic.util.StandardFileType;
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
import org.jabref.model.groups.event.GroupUpdatedEvent;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

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

/// Keeps an open directory library in sync with its files. This class owns the inbound
/// direction (file system to [BibDatabaseContext]) and the lifecycle: it is registered as a
/// [FileAlterationListener] with the polling [DirectoryMonitor], serializes all event handling
/// on a single "directory-sync" executor, and marshals model mutations through the injected
/// `modelUpdateMarshaller` (the GUI passes the JavaFX thread executor). The outbound direction
/// is delegated: entry events (relayed through the [org.jabref.logic.util.CoarseChangeFilter]
/// installed by [BibDatabaseContext#attachDirectorySynchronizer]) mark files pending in
/// [PendingWrites], which writes them through [SidecarWriteBack] and [BibMirror]; [#flush]
/// forces those writes and reports the files that could not be written.
///
/// All database mutations use [EntriesEventSource#SHARED] so that the write-back ignores them
/// (same echo-prevention policy as the shared-SQL synchronizer). Conversely, the write-back
/// registers a fingerprint of its own writes ([TrackedFiles]), which this class swallows
/// instead of re-importing. An external edit of a sidecar is applied field-wise against the
/// content last read or written, so in-memory edits of other fields survive.
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

    /// In precedence order when several sidecars share a base name.
    static final List<String> SIDECAR_EXTENSIONS = List.of("yml", "yaml", MarkdownSidecar.MARKDOWN_EXTENSION);

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryLibrarySynchronizer.class);

    /// Two poll cycles of [DirectoryMonitor], so a rename's create event can arrive in the poll
    /// cycle after its delete event.
    private static final Duration RENAME_GRACE = DirectoryMonitor.POLL_INTERVAL.multipliedBy(2).plusMillis(500);

    private static final String PDF_EXTENSION = "pdf";

    private final BibDatabaseContext databaseContext;
    private final PdfEntryFactory pdfEntryFactory;
    private final Path root;
    private final Consumer<Runnable> modelUpdateMarshaller;
    private final Clock clock;
    private final HayagrivaImporter importer = new HayagrivaImporter();
    private final MarkdownSidecar markdownSidecar = new MarkdownSidecar();
    private final ScheduledExecutorService syncExecutor;
    private final TrackedFiles files;
    private final SidecarWriteBack writeBack;
    private final BibMirror mirror;
    private final PendingWrites pendingWrites;

    private final Map<Path, StagedDeletion> stagedDeletions = new HashMap<>();

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
                                        Supplier<String> mirrorSerializer,
                                        Function<String, Optional<BibDatabaseContext>> bibParser,
                                        GitConflictResolverStrategy conflictResolver,
                                        Consumer<Runnable> modelUpdateMarshaller) {
        this(databaseContext, catalog, pdfEntryFactory, fileDisposer, fileNameGenerator, mirrorSerializer, bibParser, conflictResolver, modelUpdateMarshaller, Clock.systemUTC());
    }

    DirectoryLibrarySynchronizer(BibDatabaseContext databaseContext,
                                 DirectoryLibraryCatalog catalog,
                                 PdfEntryFactory pdfEntryFactory,
                                 Consumer<Path> fileDisposer,
                                 Function<BibEntry, Optional<String>> fileNameGenerator,
                                 Supplier<String> mirrorSerializer,
                                 Function<String, Optional<BibDatabaseContext>> bibParser,
                                 GitConflictResolverStrategy conflictResolver,
                                 Consumer<Runnable> modelUpdateMarshaller,
                                 Clock clock) {
        this.databaseContext = databaseContext;
        this.pdfEntryFactory = pdfEntryFactory;
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

        this.files = new TrackedFiles(databaseContext, catalog);
        this.writeBack = new SidecarWriteBack(files, root, modelUpdateMarshaller, fileDisposer, fileNameGenerator, this::handleFileChanged);
        this.pendingWrites = new PendingWrites(this, syncExecutor, this::writePendingFile);
        this.mirror = new BibMirror(this, root, databaseContext, files, syncExecutor, modelUpdateMarshaller,
                mirrorSerializer, bibParser, conflictResolver, this::refreshGroupsView, pendingWrites::schedule);
    }

    private boolean writePendingFile(Path file, boolean immediate) throws IOException {
        return mirror.is(file) ? mirror.write(immediate) : writeBack.write(file, immediate);
    }

    public void startWatching(DirectoryMonitor monitor) {
        IOFileFilter relevantFiles = FileFilterUtils.or(
                FileFilterUtils.directoryFileFilter(),
                FileFilterUtils.suffixFileFilter(".yml", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".yaml", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".md", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".pdf", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".bib", IOCase.INSENSITIVE));
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

    /// See [BibMirror#initialize].
    public void initializeMirror() {
        mirror.initialize();
    }

    /// The library's `.bib` mirror file.
    public Path getMirrorFile() {
        return mirror.file();
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

    /// Writes all pending changes (sidecars and mirror) now; they are otherwise debounced.
    ///
    /// @return the files whose changes could not be written; they stay pending
    public List<Path> flush() {
        return pendingWrites.flush();
    }

    /// Registers the fingerprint of a file this application just wrote itself, so the next
    /// change event for it is recognized as a self-echo and not re-imported. Consumed on match.
    public synchronized void recordWrittenFile(Path file, byte[] content) {
        files.recordWritten(file, content);
    }

    synchronized void takeBaseline() {
        files.takeBaseline();
    }

    /// The sidecar an entry is written to (tests).
    Path sidecarOf(BibEntry entry) {
        return files.catalog().sourceOf(entry).map(DirectoryLibraryCatalog.EntrySource::yamlFile).orElseThrow();
    }

    /// Waits until every event queued so far has been handled (tests).
    void awaitPendingEvents() throws InterruptedException, ExecutionException {
        syncExecutor.submit(() -> { }).get();
    }

    void doInitializeMirror() {
        mirror.doInitialize();
    }

    void mergeExternalMirror() {
        mirror.merge();
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
        // Regardless of the source — user edit or inbound sync — the model changed, so the
        // .bib mirror is stale
        mirror.markDirty();
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
        mirror.markDirty();
        if (!isUserChange(event)) {
            return;
        }
        List<BibEntry> entries = List.copyOf(event.getBibEntries());
        syncExecutor.execute(() -> entries.forEach(this::handleLocalChange));
    }

    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        mirror.markDirty();
        if (!isUserChange(event)) {
            return;
        }
        List<BibEntry> entries = List.copyOf(event.getBibEntries());
        syncExecutor.execute(() -> handleLocalRemoval(entries));
    }

    /// Groups (and other library settings) live only in the mirror's metadata block.
    @Subscribe
    public void listen(MetaDataChangedEvent event) {
        mirror.markDirty();
    }

    /// Group tree edits (add, rename, remove) are posted as group events, not metadata events.
    @Subscribe
    public void listen(GroupUpdatedEvent event) {
        mirror.markDirty();
    }

    private static boolean isUserChange(EntriesEvent event) {
        return event.getEntriesEventSource() == EntriesEventSource.LOCAL
                || event.getEntriesEventSource() == EntriesEventSource.UNDO;
    }

    synchronized void handleLocalChange(BibEntry entry) {
        pendingWrites.schedule(writeBack.fileFor(entry));
    }

    synchronized void handleLocalRemoval(List<BibEntry> entries) {
        writeBack.filesOf(entries).forEach(pendingWrites::schedule);
    }

    synchronized void handleFileCreated(Path file) {
        commitExpiredStagedDeletions();
        if (mirror.is(file)) {
            mirror.handleChanged(file);
        } else if (isSidecar(file)) {
            if (files.consumeSelfEcho(file)) {
                return;
            }
            importFile(file);
        } else if (isPdf(file)) {
            handlePdfCreated(file);
        }
    }

    synchronized void handleFileChanged(Path file) {
        commitExpiredStagedDeletions();
        if (mirror.is(file)) {
            mirror.handleChanged(file);
            return;
        }
        if (!isSidecar(file) || files.consumeSelfEcho(file)) {
            return;
        }
        List<BibEntry> knownEntries = files.entriesOf(file);
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
        if (mirror.is(file)) {
            // The mirror is derived state — recreate it
            mirror.markDirty();
            return;
        }
        if (isSidecar(file)) {
            List<BibEntry> entries = files.entriesOf(file);
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
        if (!files.entriesOf(file).isEmpty()) {
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
                           files.relocate(movedFrom, file);
                           modelUpdateMarshaller.accept(this::refreshGroupsView);
                           LOGGER.debug("Detected move {} -> {}", movedFrom, file);
                       }, () -> insertNewEntries(file, newEntries));
    }

    private void insertNewEntries(Path file, List<BibEntry> newEntries) {
        newEntries.forEach(entry -> files.catalog().register(entry, file, entry.getCitationKey().orElseThrow()));
        // Safe without event source: the entry is not yet inserted, so no listeners see this
        findPairedPdf(file).ifPresent(pdf -> newEntries.getFirst()
                                                       .addFile(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName())));
        modelUpdateMarshaller.accept(() -> {
            databaseContext.getDatabase().insertEntries(newEntries, EntriesEventSource.SHARED);
            refreshGroupsView();
        });
    }

    private void applyChangedFile(Path file, List<BibEntry> knownEntries, List<BibEntry> parsedEntries) {
        DirectoryLibraryCatalog catalog = files.catalog();
        SequencedMap<String, BibEntry> knownByKey = byCitationKey(knownEntries);
        SequencedMap<String, BibEntry> parsedByKey = byCitationKey(parsedEntries);

        List<BibEntry> toInsert = new ArrayList<>();
        List<BibEntry> toRemove = new ArrayList<>();
        List<Runnable> fieldUpdates = new ArrayList<>();

        Map<String, BibEntry> baseline = files.baseline(file);
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
        files.setBaseline(file, TrackedFiles.copiesByKey(parsedEntries));
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

    private void handlePdfCreated(Path pdf) {
        findSidecarEntry(pdf).ifPresentOrElse(entry -> {
            if (entry.getFiles().isEmpty()) {
                List<LinkedFile> linkedFiles = List.of(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName()));
                modelUpdateMarshaller.accept(() ->
                        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(linkedFiles), EntriesEventSource.SHARED));
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
            boolean isStub = files.catalog().sourceOf(entry).isEmpty();
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
        files.forget(file);
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
            files.recordSeen(file);
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
                                 .map(extension -> files.entriesOf(pdf.resolveSibling(baseName + "." + extension)))
                                 .filter(entries -> !entries.isEmpty())
                                 .map(List::getFirst)
                                 .findFirst();
    }

    private Optional<Path> findPairedPdf(Path yamlFile) {
        return Optional.of(yamlFile.resolveSibling(FileUtil.getBaseName(yamlFile) + ".pdf")).filter(Files::exists);
    }

    private static boolean isSidecar(Path file) {
        return SIDECAR_EXTENSIONS.contains(FileUtil.getFileExtension(file).orElse("").toLowerCase(Locale.ROOT));
    }

    private static boolean isPdf(Path file) {
        return PDF_EXTENSION.equals(FileUtil.getFileExtension(file).orElse("").toLowerCase(Locale.ROOT));
    }
}
