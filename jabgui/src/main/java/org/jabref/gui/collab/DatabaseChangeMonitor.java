package org.jabref.gui.collab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.Notifications;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.sync.LibraryBaseline;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.ConflictedCopies;
import org.jabref.logic.util.io.FileSnapshot;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.event.MetaDataChangedEvent;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import com.dlsc.gemsfx.infocenter.NotificationAction;
import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseChangeMonitor implements FileUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseChangeMonitor.class);
    private static final int STABLE_FILE_ATTEMPTS = 20;
    private static final long STABLE_FILE_INTERVAL_MILLIS = 250;
    /// A writer pausing between two chunks must not pass as finished: the file has to look the same this many times in a row
    private static final int STABLE_FILE_CONFIRMATIONS = 2;

    private final BibDatabaseContext database;
    private final FileUpdateMonitor fileMonitor;
    private final List<DatabaseChangeListener> listeners;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final LibraryTab libraryTab;
    private final Optional<Path> monitoredPath;
    private boolean changeDetectionSuspended;
    @Nullable private ExternalLibraryChangeNotification activeNotification;

    /// State of the monitored file as of the last scan or the last point where the in-memory library was known to
    /// match the disk (library load, successful save, all external changes merged). Guarded by
    /// `synchronized (database)`; `null` when unknown, in which case the next event triggers a full scan.
    @Nullable private FileSnapshot knownDiskState;

    /// The library as of the last point where it was known to match the disk, for telling external changes apart
    /// from unsaved in-memory edits. Guarded like [#knownDiskState]; `null` while synchronizing is off, in which case
    /// every external change is offered for review.
    @Nullable private LibraryBaseline baseline;

    /// Counts started scans and invalidations, so that a scan overtaken by a later one (whose result reflects the
    /// newer file state), by a save, by switching synchronization off, or by closing the tab discards its result
    /// instead of applying stale content.
    private volatile int scanGeneration;

    private final ChangeListener<Boolean> synchronizingListener = (_, _, _) -> onSynchronizingChanged(isSynchronizing());
    private final ChangeListener<Boolean> mergingCopiesListener = (_, _, _) -> mergeConflictedCopies(baseline);

    /// Conflicted copies already merged, with the state they had at that time; a copy is written once by the sync
    /// client, so the same file is not merged again on every scan.
    private final Map<Path, FileSnapshot> mergedConflictedCopies = new HashMap<>();

    public DatabaseChangeMonitor(BibDatabaseContext database,
                                 FileUpdateMonitor fileMonitor,
                                 TaskExecutor taskExecutor,
                                 DialogService dialogService,
                                 GuiPreferences preferences,
                                 UndoManager undoManager,
                                 StateManager stateManager,
                                 LibraryTab libraryTab) {
        this.database = database;
        this.fileMonitor = fileMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.libraryTab = libraryTab;
        this.monitoredPath = this.database.getDatabasePath();

        this.listeners = new ArrayList<>();

        monitoredPath.ifPresent(path -> {
            knownDiskState = FileSnapshot.read(path);
            baseline = captureBaseline();
            try {
                fileMonitor.addListenerForFile(path, this);
            } catch (IOException e) {
                LOGGER.error("Error while trying to monitor {}", path, e);
            }
            // Registered once per monitor; a tab replacing its monitor calls unregister() on the old one first, which
            // removes this listener again, so the preference never accumulates listeners
            if (database.getLocation() == DatabaseLocation.LOCAL) {
                preferences.getLibraryPreferences().synchronizeWithFileProperty().addListener(synchronizingListener);
                preferences.getLibraryPreferences().mergeConflictedCopiesProperty().addListener(mergingCopiesListener);
                database.getMetaData().registerListener(this);
            }
        });

        addListener(this::notifyExternalChanges);
        mergeConflictedCopies(baseline);
    }

    void notifyExternalChanges(List<DatabaseChange> changes) {
        notifyExternalChanges(changes, Localization.lang("The library has been modified by another program.") + "\n" +
                database.getDatabasePath().map(Path::toString).orElse(""));
    }

    private void notifyExternalChanges(List<DatabaseChange> changes, String description) {
        Optional.ofNullable(activeNotification).ifPresent(ExternalLibraryChangeNotification::remove);

        ExternalLibraryChangeNotification notification = new ExternalLibraryChangeNotification(changes, description, true, _ -> {
        });
        dialogService.notify(notification);
        activeNotification = notification;
    }

    private void clearActiveNotification(ExternalLibraryChangeNotification notification) {
        if (activeNotification == notification) {
            activeNotification = null;
        }
    }

    private class ExternalLibraryChangeNotification extends Notifications.FileNotification {
        /// @param fromLibraryFile whether the changes come from the library file itself; only then can a fully accepted review leave the library matching its file and therefore clean
        /// @param afterReview     run once all changes have been reviewed and applied, with whether every change was accepted
        public ExternalLibraryChangeNotification(List<DatabaseChange> changes, String description, boolean fromLibraryFile, Consumer<Boolean> afterReview) {
            super(Localization.lang("External changes detected"), description);
            setOnClick(_ -> OnClickBehaviour.NONE);

            NotificationAction<Path> dismissAction = new NotificationAction<>(Localization.lang("Dismiss changes"), _ -> {
                clearActiveNotification(this);
                return OnClickBehaviour.REMOVE;
            });

            NotificationAction<Path> reviewAction = new NotificationAction<>(Localization.lang("Review changes"), _ -> {
                DatabaseChangesResolverDialog databaseChangesResolverDialog = new DatabaseChangesResolverDialog(
                        changes,
                        database,
                        Localization.lang("External Changes Resolver"));
                Optional<Boolean> areAllChangesResolved = dialogService.showCustomDialogAndWait(databaseChangesResolverDialog);
                if (areAllChangesResolved.orElse(false)) {
                    completeReview(databaseChangesResolverDialog.getResolvedChanges(),
                            fromLibraryFile && databaseChangesResolverDialog.resolvedChangesMatchDisk(), afterReview);
                    clearActiveNotification(this);
                    return OnClickBehaviour.REMOVE;
                }

                return OnClickBehaviour.NONE;
            });

            getActions().addAll(dismissAction, reviewAction);
        }
    }

    @Override
    public void fileUpdated() {
        synchronized (database) {
            if (changeDetectionSuspended) {
                return;
            }
            scanIfFileChanged();
        }
    }

    /// Defers handling of file events while keeping the watcher registered, so that events arriving during JabRef's
    /// own save are neither acted upon (the file may be half-written) nor lost.
    public void suspendChangeDetection() {
        synchronized (database) {
            changeDetectionSuspended = true;
        }
    }

    // [impl->req~ux.external-library-changes.after-save~1]
    public void resumeChangeDetection() {
        synchronized (database) {
            changeDetectionSuspended = false;
            scanIfFileChanged();
        }
    }

    /// Records the given file state as consistent with the in-memory library. To be called whenever the two are known
    /// to match (successful save, all external changes merged), so that watcher events reflecting that very state do
    /// not trigger a scan.
    ///
    /// @param diskState the matching on-disk state, ideally as reported by the writer that committed it (captured right after the commit, this leaves no window in which a concurrent write could be mistaken for the consistent state); `null` to read the current state from the file instead, which is subject to such a window — its worst case is a delayed notification, never a lost update, since lost-update protection lives in [org.jabref.logic.exporter.AtomicFileOutputStream]
    public void markConsistentWithDisk(@Nullable FileSnapshot diskState) {
        synchronized (database) {
            if (diskState != null) {
                knownDiskState = diskState;
            } else {
                monitoredPath.ifPresent(path -> knownDiskState = FileSnapshot.read(path));
            }
            baseline = captureBaseline();
            scanGeneration++;
        }
    }

    /// Synchronizing (silently merging external changes) is decided by the library itself, or, if it does not, by the
    /// global preference.
    private boolean isSynchronizing() {
        return database.getLocation() == DatabaseLocation.LOCAL
                && database.getMetaData().getSynchronizeWithFile().orElseGet(() -> preferences.getLibraryPreferences().shouldSynchronizeWithFile());
    }

    /// Merging conflicted copies is decided the same way, and only while synchronizing at all.
    private boolean isMergingConflictedCopies() {
        return isSynchronizing()
                && database.getMetaData().getMergeConflictedCopies().orElseGet(() -> preferences.getLibraryPreferences().shouldMergeConflictedCopies());
    }

    /// The library's own setting is part of its metadata, which the library properties dialog changes.
    @Subscribe
    public void listen(MetaDataChangedEvent event) {
        onSynchronizingChanged(isSynchronizing());
    }

    /// Synchronization switched on for an open library needs a baseline right away: as long as the library is
    /// unmodified, it still matches its file. Otherwise the next save establishes the baseline.
    private void onSynchronizingChanged(boolean enabled) {
        boolean captured = false;
        synchronized (database) {
            if (!enabled) {
                if (baseline != null) {
                    baseline = null;
                    scanGeneration++;
                }
            } else if (baseline == null && (!libraryTab.isModified() || !undoManager.canUndo())) {
                // A modified tab without an undoable step was only dirtied by a setting, such as the one just
                // switched on; its entries still match the file
                baseline = captureBaseline();
                captured = baseline != null;
            }
        }
        if (captured) {
            // A review offered while synchronization was off holds changes computed against an older state; from now
            // on the scan decides, so the pending review is withdrawn and the file is looked at again
            Optional.ofNullable(activeNotification).ifPresent(ExternalLibraryChangeNotification::remove);
            activeNotification = null;
            synchronized (database) {
                scanForChanges();
            }
        }
        mergeConflictedCopies(baseline);
    }

    private @Nullable LibraryBaseline captureBaseline() {
        if (!isSynchronizing()) {
            return null;
        }
        return LibraryBaseline.of(database, preferences.getCitationKeyPatternPreferences().getKeyPatterns());
    }

    /// A full scan parses the whole library file, so it is skipped when size and modification time show that the file
    /// has not actually changed since the last known-consistent state — e.g. for events caused by JabRef's own save.
    private void scanIfFileChanged() {
        FileSnapshot currentState = monitoredPath.map(FileSnapshot::read).orElse(null);
        if (currentState != null && currentState.equals(knownDiskState)) {
            return;
        }
        knownDiskState = currentState;
        scanForChanges();
    }

    /// Looks for notable changes of the file on disk compared to the in-memory library and notifies listeners in case
    /// there are such changes.
    private void scanForChanges() {
        ChangeScanner scanner = new ChangeScanner(database, dialogService, preferences, stateManager);
        LibraryBaseline scannedBaseline = baseline;
        int generation = ++scanGeneration;
        if (scannedBaseline != null && isSynchronizing()) {
            // [impl->req~ux.external-library-changes.synchronize~1]
            BackgroundTask.wrap(() -> scanner.scanForChanges(() -> awaitStableLibraryFile(generation)))
                          .onSuccess(changes -> changes.ifPresent(scanned -> onScannedForSynchronization(generation, scanner, scannedBaseline, scanned)))
                          .onFailure(e -> LOGGER.error("Error while synchronizing with the library file", e))
                          .executeWith(taskExecutor);
            return;
        }
        BackgroundTask.wrap(() -> scanner.scanForChanges(() -> awaitStableLibraryFile(generation)))
                      .onSuccess(changes -> changes.filter(scanned -> !scanned.isEmpty())
                                                   .ifPresent(scanned -> offerReview(generation, scanned)))
                      .onFailure(e -> LOGGER.error("Error while watching for changes", e))
                      .executeWith(taskExecutor);
    }

    /// A scan overtaken by a newer file change, a save, or the tab closing must not replace the current review either.
    private void offerReview(int generation, List<DatabaseChange> changes) {
        synchronized (database) {
            if (generation != scanGeneration) {
                LOGGER.debug("Discarding review of a scan overtaken by a newer file change");
                return;
            }
            listeners.forEach(listener -> listener.databaseChanged(changes));
        }
    }

    /// Sorting the changes on the FX thread right before applying them leaves no window for a user edit to slip in
    /// between classification and application.
    private void onScannedForSynchronization(int generation, ChangeScanner scanner, LibraryBaseline scannedBaseline, List<DatabaseChange> changes) {
        // The lock keeps a file event from starting a newer scan between the check and the application
        synchronized (database) {
            if (generation != scanGeneration) {
                LOGGER.debug("Discarding result of a scan overtaken by a newer file change");
                return;
            }
            synchronize(scannedBaseline, scanner.triage(scannedBaseline, changes));
        }
    }

    /// Sync clients and editors may write a file in several steps. A file that is still growing must not be parsed:
    /// half of a library parses fine and would look like every later entry had been deleted. Waits (bounded) until
    /// size and modification time have stopped changing for a while.
    ///
    /// @return the settled state, or empty when the file kept changing for the whole wait or its attributes could
    /// not be read; either way it must not be parsed. The writer's next events start a fresh scan.
    /// @throws InterruptedException when the wait was interrupted; the file may still be incomplete, so the caller must not parse it
    private static Optional<FileSnapshot> awaitStableFile(Path path) throws InterruptedException {
        FileSnapshot last = FileSnapshot.read(path);
        int unchanged = 0;
        for (int attempt = 0; attempt < STABLE_FILE_ATTEMPTS && unchanged < STABLE_FILE_CONFIRMATIONS; attempt++) {
            Thread.sleep(STABLE_FILE_INTERVAL_MILLIS);
            FileSnapshot current = FileSnapshot.read(path);
            unchanged = Objects.equals(current, last) ? unchanged + 1 : 0;
            last = current;
        }
        if (unchanged < STABLE_FILE_CONFIRMATIONS) {
            LOGGER.debug("{} kept changing; the scan is abandoned", path);
            return Optional.empty();
        }
        return Optional.ofNullable(last);
    }

    /// The state seen becomes the known disk state, so that the events of the write just waited for do not trigger
    /// another scan; only for the current scan, since a scan already overtaken will not apply what it sees, and
    /// recording it would make the next event look handled.
    ///
    /// @return `false` when the wait was interrupted or the file did not settle; the scan must not go on
    private boolean awaitStableLibraryFile(int generation) {
        Path path = monitoredPath.orElse(null);
        if (path == null) {
            return true;
        }
        try {
            Optional<FileSnapshot> state = awaitStableFile(path);
            if (state.isEmpty()) {
                return false;
            }
            synchronized (database) {
                if (generation == scanGeneration) {
                    knownDiskState = state.get();
                }
            }
            return true;
        } catch (InterruptedException e) {
            LOGGER.debug("Interrupted while waiting for {} to stop changing; the scan is abandoned", path, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /// Applies what changed on disk only, and offers the review for what changed on both sides.
    private void synchronize(LibraryBaseline scannedBaseline, ChangeTriage.Triage triage) {
        synchronize(scannedBaseline, triage, Localization.lang("Merged %0 change(s) from the library file", String.valueOf(triage.diskOnly().size())), null);
    }

    /// @param conflictedCopy the merged file when it is a conflicted copy rather than the library file itself; its changes needing review are announced as such, and the copy is offered for deletion once nothing of it is left to review
    private void synchronize(LibraryBaseline scannedBaseline, ChangeTriage.Triage triage, String mergedMessage, @Nullable Path conflictedCopy) {
        List<DatabaseChange> unresolved = new ArrayList<>(triage.bothSides());
        unresolved.addAll(triage.memoryOnly());
        // A conflicted copy is never the file the library must match, so the library stays marked as changed
        boolean matchesDisk = conflictedCopy == null && unresolved.isEmpty() && !libraryTab.isModified();
        if (!triage.diskOnly().isEmpty()) {
            applyResolvedChanges(triage.diskOnly(), matchesDisk);
            dialogService.notify(mergedMessage);
        }
        synchronized (database) {
            LibraryBaseline updated = captureBaseline();
            if (updated != null) {
                ChangeTriage.keepUnresolved(updated, scannedBaseline, unresolved);
            }
            baseline = updated;
        }
        if (conflictedCopy == null) {
            if (!triage.bothSides().isEmpty()) {
                listeners.forEach(listener -> listener.databaseChanged(triage.bothSides()));
            }
        } else if (!triage.bothSides().isEmpty()) {
            // Shown next to, not instead of, a pending review of the library file itself
            dialogService.notify(new ExternalLibraryChangeNotification(triage.bothSides(),
                    Localization.lang("The conflicted copy '%0' contains changes that need review.", conflictedCopy.getFileName().toString()),
                    false,
                    everythingAccepted -> dialogService.notify(new ConflictedCopyMergedNotification(conflictedCopy, everythingAccepted))));
        } else {
            dialogService.notify(new ConflictedCopyMergedNotification(conflictedCopy, true));
        }
        mergeConflictedCopies(scannedBaseline);
    }

    /// Merges the copies a sync client left next to the library file (see [ConflictedCopies]) the same way as the
    /// library file itself: against the baseline, with a review only for real conflicts. Runs after each scan, since
    /// a client writes the copy together with the library file, when the library is opened, and when synchronization
    /// is switched on.
    ///
    /// Copies are merged one after the other, but all against the same baseline: like the library file, every copy is
    /// a complete snapshot taken from that baseline, so an entry one copy added is not deleted in the next copy, it
    /// merely is not there yet.
    /// [impl->req~ux.external-library-changes.conflicted-copies~1]
    private void mergeConflictedCopies(@Nullable LibraryBaseline scannedBaseline) {
        Path path = monitoredPath.orElse(null);
        if (path == null || scannedBaseline == null || !isMergingConflictedCopies()) {
            return;
        }
        Optional<Path> next = ConflictedCopies.find(path).stream()
                                              .filter(copy -> !Objects.equals(FileSnapshot.read(copy), mergedConflictedCopies.get(copy)))
                                              .findFirst();
        if (next.isEmpty()) {
            return;
        }
        Path copy = next.get();
        // Claimed before the scan so that a scan triggered in the meantime does not merge the same copy twice;
        // released again if the merge does not go through, so that a later attempt can retry
        FileSnapshot claimed = FileSnapshot.read(copy);
        mergedConflictedCopies.put(copy, claimed);
        ChangeScanner scanner = new ChangeScanner(database, dialogService, preferences, stateManager);
        // No new generation: merging a copy must not cancel the library scan it may run alongside
        int generation = scanGeneration;
        BackgroundTask.wrap(() -> scanCopy(scanner, copy))
                      .onSuccess(scanned -> {
                          synchronized (database) {
                              if (scanned.isEmpty() || generation != scanGeneration || !isMergingConflictedCopies()) {
                                  mergedConflictedCopies.remove(copy);
                                  return;
                              }
                              // The state that was parsed, so that the copy is neither merged again nor deleted in another state
                              mergedConflictedCopies.put(copy, scanned.get().state());
                              ChangeTriage.Triage triage = scanner.triage(scannedBaseline, scanned.get().changes());
                              synchronize(scannedBaseline, triage,
                                      Localization.lang("Merged %0 change(s) from the conflicted copy '%1'", String.valueOf(triage.diskOnly().size()), copy.getFileName().toString()),
                                      copy);
                          }
                      })
                      .onFailure(e -> {
                          LOGGER.error("Error while merging conflicted copy {}", copy, e);
                          mergedConflictedCopies.remove(copy);
                          dialogService.notify(Localization.lang("Could not read the conflicted copy '%0'.", copy.getFileName().toString()));
                      })
                      .executeWith(taskExecutor);
    }

    private record ScannedCopy(FileSnapshot state, List<DatabaseChange> changes) {
    }

    /// Parses the copy once it has settled and only if it is still in that state afterwards; empty when it kept
    /// changing or was interrupted, in which case the sync client's next write starts over.
    private static Optional<ScannedCopy> scanCopy(ChangeScanner scanner, Path copy) throws IOException {
        Optional<FileSnapshot> settled;
        try {
            settled = awaitStableFile(copy);
        } catch (InterruptedException e) {
            LOGGER.debug("Interrupted while waiting for {} to stop changing; the merge is abandoned", copy, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        if (settled.isEmpty()) {
            return Optional.empty();
        }
        List<DatabaseChange> changes = scanner.scanFile(copy);
        if (!settled.get().matches(copy)) {
            LOGGER.debug("{} changed while being parsed; the merge is abandoned", copy);
            return Optional.empty();
        }
        return Optional.of(new ScannedCopy(settled.get(), changes));
    }

    /// Nothing of the copy is left to look at, so it only clutters the folder; deleting stays the user's call, and a
    /// change the user rejected in the review lives on in the copy only.
    @NullMarked
    private class ConflictedCopyMergedNotification extends Notifications.FileNotification {
        ConflictedCopyMergedNotification(Path copy, boolean everythingApplied) {
            super(Localization.lang("Conflicted copy merged"),
                    everythingApplied
                    ? Localization.lang("All changes of '%0' are in the library.", copy.getFileName().toString())
                    : Localization.lang("Review of '%0' is complete. The changes you rejected exist only in the copy.", copy.getFileName().toString()));
            setOnClick(_ -> OnClickBehaviour.REMOVE);
            // The sync client may replace the copy while the notification is showing; only the merged state is deleted
            FileSnapshot mergedState = mergedConflictedCopies.get(copy);
            getActions().add(new NotificationAction<>(Localization.lang("Delete copy"), _ -> {
                if (mergedState == null || !mergedState.matches(copy)) {
                    dialogService.notify(Localization.lang("'%0' has changed since it was merged and was not deleted.", copy.getFileName().toString()));
                    return OnClickBehaviour.REMOVE;
                }
                try {
                    Files.deleteIfExists(copy);
                } catch (IOException e) {
                    LOGGER.error("Could not delete conflicted copy {}", copy, e);
                    dialogService.notify(Localization.lang("Could not delete '%0'.", copy.getFileName().toString()));
                }
                return OnClickBehaviour.REMOVE;
            }));
        }
    }

    @Nullable LibraryBaseline getBaseline() {
        return baseline;
    }

    /// Applies what the review accepted, rebases, and tells the caller whether everything was accepted.
    ///
    /// @param resolved    the reviewed changes, accepted or not
    /// @param matchesDisk whether the library now matches its file, see [#applyResolvedChanges]
    void completeReview(List<DatabaseChange> resolved, boolean matchesDisk, Consumer<Boolean> afterReview) {
        applyResolvedChanges(resolved, matchesDisk);
        rebaseAfterReview(resolved);
        afterReview.accept(resolved.stream().allMatch(DatabaseChange::isAccepted));
    }

    /// After a review, the accepted changes are in memory and must not count as a divergence anymore; the rejected
    /// ones keep their ancestor, so that the next scan reports them again instead of taking memory for the ancestor.
    private void rebaseAfterReview(List<DatabaseChange> resolved) {
        synchronized (database) {
            LibraryBaseline previous = baseline;
            LibraryBaseline updated = captureBaseline();
            if (updated != null && previous != null) {
                ChangeTriage.keepUnresolved(updated, previous, resolved.stream().filter(change -> !change.isAccepted()).toList());
            }
            baseline = updated;
        }
    }

    /// Applies the accepted external changes and updates the library's dirty state.
    ///
    /// @param resolvedChanges          the externally resolved changes to apply to the in-memory database
    /// @param resolvedChangesMatchDisk `true` if the accepted result now matches the file on disk, so the library can be marked clean; `false` if the resolved result differs from disk and still needs saving
    void applyResolvedChanges(List<DatabaseChange> resolvedChanges, boolean resolvedChangesMatchDisk) {
        undoManager.addEdit(Localization.lang("Merged external changes"), edit ->
                resolvedChanges.stream()
                               .filter(DatabaseChange::isAccepted)
                               .forEach(change -> change.applyChange(edit)));

        if (resolvedChangesMatchDisk) {
            libraryTab.resetChangedProperties();
        } else {
            // Nothing on the stack describes a denied change - denying one records nothing - but the
            // file no longer matches what the user chose to keep, so the library still needs saving.
            undoManager.markChanged();
        }
    }

    public void addListener(DatabaseChangeListener listener) {
        listeners.add(listener);
    }

    public void unregister() {
        scanGeneration++;
        monitoredPath.ifPresent(path -> {
            fileMonitor.removeListener(path, this);
            // Unconditionally: the library may have been converted to a shared one since the listener was added,
            // and removing a listener that was never added is a no-op
            preferences.getLibraryPreferences().synchronizeWithFileProperty().removeListener(synchronizingListener);
            preferences.getLibraryPreferences().mergeConflictedCopiesProperty().removeListener(mergingCopiesListener);
            database.getMetaData().unregisterListener(this);
        });
    }
}
