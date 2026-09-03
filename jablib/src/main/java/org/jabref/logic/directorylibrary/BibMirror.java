package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jabref.logic.exporter.AtomicFileOutputStream;
import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.merge.execution.GitMergeApplier;
import org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer;
import org.jabref.logic.git.model.MergeAnalysis;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.DirectoryStructureGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The `.bib` mirror of a directory library: the whole library as one BibTeX file,
/// `<root>/<root-name>.bib`, so plain BibTeX consumers (and collaborators without this feature)
/// can read and edit the library as one file. Every model change refreshes the mirror
/// (debounced through [PendingWrites]); a copy of the last written mirror is kept under
/// `.jabref/mirror-base.bib` as the merge base. External edits of the mirror — live or while
/// JabRef was closed — are three-way merged into the library with the git-sync semantic merge
/// ([SemanticMergeAnalyzer]); auto-mergeable changes apply as local changes (so the sidecar
/// write-back persists them), true conflicts go to the injected [GitConflictResolverStrategy],
/// and a cancelled resolution keeps the library's state. The mirror's metadata block is also
/// where a directory library's user-defined groups survive a restart.
// [impl->req~directory-library.bib-mirror~2]
@NullMarked
public class BibMirror {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibMirror.class);

    private final Object lock;
    private final Path root;
    private final BibDatabaseContext databaseContext;
    private final TrackedFiles files;
    private final ScheduledExecutorService syncExecutor;
    private final Consumer<Runnable> modelUpdateMarshaller;
    private final Supplier<String> serializer;
    private final Function<String, Optional<BibDatabaseContext>> parser;
    private final GitConflictResolverStrategy conflictResolver;
    private final Runnable groupsViewRefresher;
    private final Consumer<Path> writeScheduler;

    /// @param serializer     serializes the live library to BibTeX; runs on the UI thread
    /// @param writeScheduler schedules the (debounced) write of the mirror file
    BibMirror(Object lock,
              Path root,
              BibDatabaseContext databaseContext,
              TrackedFiles files,
              ScheduledExecutorService syncExecutor,
              Consumer<Runnable> modelUpdateMarshaller,
              Supplier<String> serializer,
              Function<String, Optional<BibDatabaseContext>> parser,
              GitConflictResolverStrategy conflictResolver,
              Runnable groupsViewRefresher,
              Consumer<Path> writeScheduler) {
        this.lock = lock;
        this.root = root;
        this.databaseContext = databaseContext;
        this.files = files;
        this.syncExecutor = syncExecutor;
        this.modelUpdateMarshaller = modelUpdateMarshaller;
        this.serializer = serializer;
        this.parser = parser;
        this.conflictResolver = conflictResolver;
        this.groupsViewRefresher = groupsViewRefresher;
        this.writeScheduler = writeScheduler;
    }

    /// The mirror's file name for a library root: a filesystem root (`/`, `C:\\`) has no file
    /// name.
    public static String fileName(Path root) {
        return Optional.ofNullable(root.getFileName()).map(Path::toString).orElse("library") + ".bib";
    }

    /// The snapshot of the mirror as this application last wrote it — the base of the
    /// three-way merge when the mirror is changed externally.
    public static Path baseFile(Path root) {
        return root.resolve(".jabref").resolve("mirror-base.bib");
    }

    public Path file() {
        return root.resolve(fileName(root));
    }

    boolean is(Path file) {
        return file.toAbsolutePath().normalize().equals(file().toAbsolutePath().normalize());
    }

    /// Every model change — user edit or inbound — stales the mirror. Hops to the sync thread,
    /// so the UI thread never waits for the synchronizer's monitor while files are written.
    void markDirty() {
        syncExecutor.execute(() -> writeScheduler.accept(file()));
    }

    /// Brings mirror and library together after opening: creates a missing mirror, merges an
    /// externally changed one (changed while this application was not watching), and adopts a
    /// pre-existing `.bib` (no recorded base) by importing it against an empty base — which can
    /// only add or conflict, never delete library content.
    void initialize() {
        syncExecutor.execute(this::doInitialize);
    }

    void doInitialize() {
        synchronized (lock) {
            Path mirror = file();
            if (!Files.exists(mirror)) {
                writeScheduler.accept(mirror);
                return;
            }
            // The mirror's metadata is the only place user-defined groups of a directory
            // library survive a restart — the sidecars carry entries, not library metadata
            readBibContext(mirror).ifPresent(this::adoptUserGroups);
            try {
                if (Files.exists(baseFile(root)) && Files.mismatch(mirror, baseFile(root)) == -1L) {
                    return;
                }
            } catch (IOException e) {
                LOGGER.warn("Could not compare mirror {} with its base", mirror, e);
                return;
            }
        }
        syncExecutor.execute(this::merge);
    }

    /// The watcher saw the mirror change (or appear).
    void handleChanged(Path file) {
        if (files.consumeSelfEcho(file)) {
            return;
        }
        // Runs as its own task, NOT under the synchronizer's monitor: conflict resolution blocks
        // on the GUI thread, and the GUI thread meanwhile posts entry events into synchronized
        // methods of the synchronizer — holding the monitor here would deadlock.
        syncExecutor.execute(this::merge);
    }

    /// Three-way merge of an externally modified mirror into the library: base = the mirror as
    /// last written (empty when unknown), local = the library, remote = the mirror's current
    /// content. The auto-plan and resolved conflicts are applied as local changes, so the
    /// regular write-back persists them into the sidecars; afterwards the mirror is rewritten
    /// from the merged library state.
    void merge() {
        readBibContext(file()).ifPresentOrElse(this::merge,
                () -> LOGGER.warn("Not applying unparseable mirror {}", file()));
    }

    private void merge(BibDatabaseContext remote) {
        BibDatabaseContext base = readBibContext(baseFile(root)).orElseGet(BibDatabaseContext::new);
        MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(base, databaseContext, remote);
        if (!analysis.autoPlan().isEmpty()) {
            modelUpdateMarshaller.accept(() -> {
                GitMergeApplier.applyAutoPlan(databaseContext, analysis.autoPlan());
                groupsViewRefresher.run();
            });
        }
        if (!analysis.conflicts().isEmpty()) {
            List<BibEntry> resolved = conflictResolver.resolveConflicts(analysis.conflicts());
            if (resolved.isEmpty()) {
                LOGGER.info("Conflict resolution cancelled — keeping the library's state for {} conflicting entries", analysis.conflicts().size());
            } else {
                modelUpdateMarshaller.accept(() -> {
                    GitMergeApplier.applyResolved(databaseContext, resolved);
                    groupsViewRefresher.run();
                });
            }
        }
        // The merged state (or, on cancel, the library's state) becomes the new mirror + base
        markDirty();
    }

    /// Restores user-defined groups from the mirror's metadata into the freshly scanned
    /// context (whose tree only holds the automatic directory-structure group). The
    /// serialized directory-structure group itself is skipped — the scanner installs it with
    /// a live lookup, the parsed one would be an empty duplicate.
    private void adoptUserGroups(BibDatabaseContext remote) {
        remote.getMetaData().getGroups().ifPresent(remoteRoot ->
                databaseContext.getMetaData().getGroups().ifPresent(localRoot -> {
                    List<GroupTreeNode> adoptable = remoteRoot.getChildren().stream()
                                                              .filter(child -> !(child.getGroup() instanceof DirectoryStructureGroup))
                                                              .toList();
                    if (adoptable.isEmpty()) {
                        return;
                    }
                    modelUpdateMarshaller.accept(() -> {
                        adoptable.forEach(child -> child.moveTo(localRoot));
                        groupsViewRefresher.run();
                    });
                }));
    }

    private Optional<BibDatabaseContext> readBibContext(Path file) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return parser.apply(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.warn("Could not read {}", file, e);
            return Optional.empty();
        }
    }

    /// Serializing walks the live model, which only the UI thread may do safely: the debounced
    /// path serializes there and hands the bytes back to the sync thread, while a flush —
    /// called on the UI thread — does both inline.
    ///
    /// @return always `true`: the deferred path reports its failure by re-scheduling
    boolean write(boolean immediate) throws IOException {
        if (immediate) {
            writeContent(serializer.get());
            return true;
        }
        modelUpdateMarshaller.accept(() -> {
            String content = serializer.get();
            syncExecutor.execute(() -> {
                try {
                    writeContent(content);
                } catch (IOException e) {
                    LOGGER.error("Could not write mirror {}", file(), e);
                    writeScheduler.accept(file());
                }
            });
        });
        return true;
    }

    private void writeContent(String document) throws IOException {
        synchronized (lock) {
            Path mirror = file();
            byte[] content = document.getBytes(StandardCharsets.UTF_8);
            try (AtomicFileOutputStream output = new AtomicFileOutputStream(mirror, false)) {
                output.write(content);
            }
            files.recordWritten(mirror, content);
            Files.createDirectories(baseFile(root).getParent());
            Files.write(baseFile(root), content);
        }
    }
}
