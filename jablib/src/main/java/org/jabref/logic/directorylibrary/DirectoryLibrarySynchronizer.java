package org.jabref.logic.directorylibrary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.exporter.HayagrivaEntryWriter;
import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.merge.execution.GitMergeApplier;
import org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer;
import org.jabref.logic.git.model.MergeAnalysis;
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
/// per file; [#flush] forces them, and shutdown flushes implicitly.
///
/// The library is additionally mirrored into a single `<root>/<root-name>.bib` file so plain
/// BibTeX consumers (and collaborators without this feature) can read and edit the library as
/// one file. Every model change refreshes the mirror (same debounce); a copy of the last
/// written mirror is kept under `.jabref/mirror-base.bib` as the merge base. External edits of
/// the mirror — live or while JabRef was closed — are three-way merged into the library with
/// the git-sync semantic merge ([SemanticMergeAnalyzer]); auto-mergeable changes apply as
/// local changes (so the sidecar write-back persists them), true conflicts go to the injected
/// [GitConflictResolverStrategy], and a cancelled resolution keeps the library's state.
// [impl->req~directory-library.inbound-sync~2]
// [impl->req~directory-library.write-back~2]
// [impl->req~directory-library.bib-mirror~1]
@NullMarked
public class DirectoryLibrarySynchronizer implements FileAlterationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryLibrarySynchronizer.class);

    /// Two poll cycles of [DirectoryMonitor] (1 s each), so a rename's create event can arrive
    /// in the poll cycle after its delete event.
    private static final Duration RENAME_GRACE = Duration.ofMillis(2500);

    private static final Set<String> SIDECAR_EXTENSIONS = Set.of("yml", "yaml", MarkdownSidecar.MARKDOWN_EXTENSION);
    private static final String PDF_EXTENSION = "pdf";

    /// Collects keystroke-level bursts into one write per file. Trailing edge: every change
    /// event re-arms the timer, so the write fires once typing pauses and always persists the
    /// latest state.
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
    private final HayagrivaEntryWriter entryWriter = new HayagrivaEntryWriter();
    private final Set<Path> dirtyFiles = new LinkedHashSet<>();
    private final Consumer<Path> fileDisposer;
    private final Function<BibEntry, Optional<String>> fileNameGenerator;
    private final Supplier<String> mirrorSerializer;
    private final Function<String, Optional<BibDatabaseContext>> bibParser;
    private final GitConflictResolverStrategy conflictResolver;
    private boolean mirrorDirty;
    private @Nullable ScheduledFuture<?> scheduledWrite;

    private @Nullable FileAlterationObserver observer;
    private @Nullable DirectoryMonitor directoryMonitor;

    private record StagedDeletion(List<BibEntry> entries, Instant expiry) {
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
        this.catalog = catalog;
        this.pdfEntryFactory = pdfEntryFactory;
        this.fileDisposer = fileDisposer;
        this.fileNameGenerator = fileNameGenerator;
        this.mirrorSerializer = mirrorSerializer;
        this.bibParser = bibParser;
        this.conflictResolver = conflictResolver;
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
                FileFilterUtils.suffixFileFilter(".pdf", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".bib", IOCase.INSENSITIVE));
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
        flush();
        syncExecutor.shutdown();
    }

    /// Writes all pending sidecar changes now (they are otherwise debounced).
    public synchronized void flush() {
        writeDirtyFiles();
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

    @Subscribe
    public void listen(EntryChangedEvent event) {
        // Regardless of the source — user edit or inbound sync — the model changed, so the
        // .bib mirror is stale
        markMirrorDirty();
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
        markMirrorDirty();
        if (!isUserChange(event)) {
            return;
        }
        List<BibEntry> entries = List.copyOf(event.getBibEntries());
        syncExecutor.execute(() -> entries.forEach(this::handleLocalChange));
    }

    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        markMirrorDirty();
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
        Path file = catalog.sourceOf(entry)
                           .map(DirectoryLibraryCatalog.EntrySource::yamlFile)
                           .orElseGet(() -> assignSidecar(entry));
        dirtyFiles.add(file);
        scheduleWrite();
    }

    synchronized void handleLocalRemoval(List<BibEntry> entries) {
        Set<Path> affectedFiles = new LinkedHashSet<>();
        for (BibEntry entry : entries) {
            catalog.sourceOf(entry).ifPresent(source -> {
                affectedFiles.add(source.yamlFile());
                catalog.removeEntry(entry);
            });
        }
        for (Path file : affectedFiles) {
            if (catalog.entryIdsIn(file).isEmpty()) {
                dirtyFiles.remove(file);
                if (Files.exists(file)) {
                    fileDisposer.accept(file);
                }
            } else {
                dirtyFiles.add(file);
                scheduleWrite();
            }
        }
    }

    /// The first user change of an entry without a source materializes its sidecar — a Markdown
    /// sidecar (see [MarkdownSidecar]): next to the entry's PDF (sharing the base name, per the
    /// pairing convention), or named after the citation key for entries without a file.
    private Path assignSidecar(BibEntry entry) {
        Optional<Path> pairedFile = entry.getFiles().stream()
                                         .filter(linkedFile -> !linkedFile.isOnlineLink())
                                         .findFirst()
                                         .map(linkedFile -> root.resolve(linkedFile.getLink()).normalize());
        Path sidecar;
        if (pairedFile.isPresent() && pairedFile.get().startsWith(root)) {
            Path parent = pairedFile.get().getParent();
            sidecar = parent.resolve(FileUtil.getBaseName(pairedFile.get()) + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
        } else {
            String baseName = entry.getCitationKey().filter(key -> !key.isBlank()).orElse("entry");
            sidecar = root.resolve(baseName + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
            int counter = 1;
            while (Files.exists(sidecar)) {
                sidecar = root.resolve(baseName + "-" + counter++ + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
            }
        }
        catalog.register(entry, sidecar, entry.getCitationKey().orElse(""));
        return sidecar;
    }

    private synchronized void scheduleWrite() {
        if (scheduledWrite != null) {
            scheduledWrite.cancel(false);
        }
        scheduledWrite = syncExecutor.schedule(this::writeDirtyFiles, WRITE_DEBOUNCE.toMillis(), TimeUnit.MILLISECONDS);
    }

    private synchronized void writeDirtyFiles() {
        scheduledWrite = null;
        List<Path> files = List.copyOf(dirtyFiles);
        dirtyFiles.clear();
        files.forEach(this::writeFile);
        if (mirrorDirty) {
            mirrorDirty = false;
            writeMirror();
        }
    }

    /// The library's `.bib` mirror: the whole library as one BibTeX file, named after the
    /// library root, inside it.
    public Path getMirrorFile() {
        Path name = root.getFileName();
        return root.resolve((name == null ? "library" : name.toString()) + ".bib");
    }

    /// The snapshot of the mirror as this application last wrote it — the base of the
    /// three-way merge when the mirror is changed externally.
    private Path mirrorBaseFile() {
        return root.resolve(".jabref").resolve("mirror-base.bib");
    }

    private boolean isMirror(Path file) {
        return file.toAbsolutePath().normalize().equals(getMirrorFile().toAbsolutePath().normalize());
    }

    private synchronized void markMirrorDirty() {
        mirrorDirty = true;
        scheduleWrite();
    }

    /// Brings mirror and library together after opening: creates a missing mirror, merges an
    /// externally changed one (changed while this application was not watching), and adopts a
    /// pre-existing `.bib` (no recorded base) by importing it against an empty base — which can
    /// only add or conflict, never delete library content.
    public void initializeMirror() {
        syncExecutor.execute(this::doInitializeMirror);
    }

    synchronized void doInitializeMirror() {
        Path mirror = getMirrorFile();
        if (!Files.exists(mirror)) {
            mirrorDirty = true;
            writeDirtyFiles();
            return;
        }
        try {
            if (Files.exists(mirrorBaseFile())
                    && hash(Files.readAllBytes(mirror)).equals(hash(Files.readAllBytes(mirrorBaseFile())))) {
                return;
            }
        } catch (IOException e) {
            LOGGER.warn("Could not compare mirror {} with its base", mirror, e);
            return;
        }
        syncExecutor.execute(this::mergeExternalMirror);
    }

    private void handleMirrorChanged(Path file) {
        if (consumeSelfEcho(file)) {
            return;
        }
        // Runs as its own task, NOT under this object's monitor: conflict resolution blocks on
        // the GUI thread, and the GUI thread meanwhile posts entry events into synchronized
        // methods of this class — holding the monitor here would deadlock.
        syncExecutor.execute(this::mergeExternalMirror);
    }

    /// Three-way merge of an externally modified mirror into the library: base = the mirror as
    /// last written (empty when unknown), local = the library, remote = the mirror's current
    /// content. The auto-plan and resolved conflicts are applied as local changes, so the
    /// regular write-back persists them into the sidecars; afterwards the mirror is rewritten
    /// from the merged library state.
    void mergeExternalMirror() {
        Optional<BibDatabaseContext> remote = readBibContext(getMirrorFile());
        if (remote.isEmpty()) {
            LOGGER.warn("Not applying unparseable mirror {}", getMirrorFile());
            return;
        }
        BibDatabaseContext base = readBibContext(mirrorBaseFile()).orElseGet(BibDatabaseContext::new);
        MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(base, databaseContext, remote.get());
        if (!analysis.autoPlan().isEmpty()) {
            modelUpdateMarshaller.accept(() -> {
                GitMergeApplier.applyAutoPlan(databaseContext, analysis.autoPlan());
                refreshGroupsView();
            });
        }
        if (!analysis.conflicts().isEmpty()) {
            List<BibEntry> resolved = conflictResolver.resolveConflicts(analysis.conflicts());
            if (resolved.isEmpty()) {
                LOGGER.info("Conflict resolution cancelled — keeping the library's state for {} conflicting entries", analysis.conflicts().size());
            } else {
                modelUpdateMarshaller.accept(() -> {
                    GitMergeApplier.applyResolved(databaseContext, resolved);
                    refreshGroupsView();
                });
            }
        }
        // The merged state (or, on cancel, the library's state) becomes the new mirror + base
        markMirrorDirty();
    }

    private Optional<BibDatabaseContext> readBibContext(Path file) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return bibParser.apply(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.warn("Could not read {}", file, e);
            return Optional.empty();
        }
    }

    private void writeMirror() {
        Path mirror = getMirrorFile();
        try {
            byte[] content = mirrorSerializer.get().getBytes(StandardCharsets.UTF_8);
            recordWrittenFile(mirror, content);
            Path temporary = mirror.resolveSibling(mirror.getFileName() + ".jabref-write.tmp");
            Files.write(temporary, content);
            Files.move(temporary, mirror, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Files.createDirectories(mirrorBaseFile().getParent());
            Files.write(mirrorBaseFile(), content);
        } catch (IOException e) {
            LOGGER.error("Could not write mirror {}", mirror, e);
        }
    }

    /// Renames the sidecar (and its equally named PDF) to the base name the filename pattern
    /// generates for the entry — kept in sync as a pair, per the pairing convention. Occupied
    /// target names and pattern failures leave the current name untouched. Never touches other
    /// files.
    // [impl->req~directory-library.pattern-rename~1]
    private Path applyFileNamePattern(Path file, BibEntry entry) {
        Optional<String> generated = fileNameGenerator.apply(entry).map(String::trim).filter(not(String::isEmpty));
        if (generated.isEmpty() || generated.get().equals(FileUtil.getBaseName(file))) {
            return file;
        }
        Path directory = file.getParent();
        if (directory == null) {
            return file;
        }
        String oldBaseName = FileUtil.getBaseName(file);
        String extension = FileUtil.getFileExtension(file).orElse("yml");
        Path newSidecar = directory.resolve(generated.get() + "." + extension);
        Path oldPdf = directory.resolve(oldBaseName + ".pdf");
        Path newPdf = directory.resolve(generated.get() + ".pdf");
        if (Files.exists(newSidecar) || (Files.exists(oldPdf) && Files.exists(newPdf))) {
            return file;
        }
        try {
            if (Files.exists(file)) {
                Files.move(file, newSidecar);
            }
            catalog.relocateFile(file, newSidecar);
            if (Files.exists(oldPdf)) {
                Files.move(oldPdf, newPdf);
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
        } catch (IOException e) {
            LOGGER.warn("Could not rename {} to the configured pattern", file, e);
            return file;
        }
    }

    private void writeFile(Path file) {
        List<BibEntry> entries = entriesOf(file);
        if (entries.isEmpty()) {
            return;
        }
        if (entries.size() == 1) {
            // The user's rename rule: a single-entry sidecar and its paired PDF share the base
            // name generated by the configured filename pattern; multi-entry files have no
            // single generating entry and keep their name
            file = applyFileNamePattern(file, entries.getFirst());
        }
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
        try {
            String existingDocument = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
            String document = MarkdownSidecar.hasMarkdownExtension(file)
                              ? markdownSidecar.merge(existingDocument, keyedEntries)
                              : entryWriter.mergeIntoDocument(existingDocument, keyedEntries);
            byte[] content = document.getBytes(StandardCharsets.UTF_8);
            recordWrittenFile(file, content);
            // Written atomically: the polling watcher (or another process) must never see a
            // half-written sidecar
            Path temporary = file.resolveSibling(file.getFileName() + ".jabref-write.tmp");
            Files.write(temporary, content);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            keyedEntries.forEach(keyedEntry -> catalog.updateHayagrivaKey(keyedEntry.entry(), keyedEntry.targetKey()));
        } catch (IOException e) {
            LOGGER.error("Could not write sidecar {}", file, e);
        }
    }

    synchronized void handleFileCreated(Path file) {
        commitExpiredStagedDeletions();
        if (isMirror(file)) {
            handleMirrorChanged(file);
        } else if (isSidecar(file)) {
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
        if (isMirror(file)) {
            handleMirrorChanged(file);
            return;
        }
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
        if (isMirror(file)) {
            // The mirror is derived state — recreate it
            markMirrorDirty();
            return;
        }
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
            modelUpdateMarshaller.accept(this::refreshGroupsView);
            LOGGER.debug("Detected move {} -> {}", movedFrom.get(), file);
            return;
        }

        newEntries.forEach(entry -> catalog.register(entry, file, entry.getCitationKey().orElse("")));
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
