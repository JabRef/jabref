package org.jabref.gui.collab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.value.ChangeListener;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.Notifications;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileSnapshot;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import com.dlsc.gemsfx.infocenter.NotificationAction;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseChangeMonitor implements FileUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseChangeMonitor.class);
    private static final int STABLE_FILE_ATTEMPTS = 20;
    private static final long STABLE_FILE_INTERVAL_MILLIS = 250;

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

    private final ChangeListener<Boolean> synchronizingListener = (_, _, enabled) -> onSynchronizingChanged(enabled);

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
            if (database.getLocation() == DatabaseLocation.LOCAL) {
                preferences.getLibraryPreferences().autoSaveProperty().addListener(synchronizingListener);
            }
        });

        addListener(this::notifyExternalChanges);
    }

    void notifyExternalChanges(List<DatabaseChange> changes) {
        Optional.ofNullable(activeNotification).ifPresent(ExternalLibraryChangeNotification::remove);

        ExternalLibraryChangeNotification notification = new ExternalLibraryChangeNotification(changes);
        dialogService.notify(notification);
        activeNotification = notification;
    }

    private void clearActiveNotification(ExternalLibraryChangeNotification notification) {
        if (activeNotification == notification) {
            activeNotification = null;
        }
    }

    private class ExternalLibraryChangeNotification extends Notifications.FileNotification {
        public ExternalLibraryChangeNotification(List<DatabaseChange> changes) {
            super(Localization.lang("External changes detected"),
                    Localization.lang("The library has been modified by another program.") + "\n" +
                            database.getDatabasePath()
                                    .map(Path::toString)
                                    .orElse(""));
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
                    applyResolvedChanges(
                            databaseChangesResolverDialog.getResolvedChanges(),
                            databaseChangesResolverDialog.resolvedChangesMatchDisk());

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

    /// Synchronizing (silently merging external changes) is tied to the autosave preference: both together keep a
    /// local library and its file the same in both directions.
    private boolean isSynchronizing() {
        return database.getLocation() == DatabaseLocation.LOCAL && preferences.getLibraryPreferences().shouldAutoSave();
    }

    /// Synchronization switched on for an open library needs a baseline right away: as long as the library is
    /// unmodified, it still matches its file. Otherwise the next save establishes the baseline.
    private void onSynchronizingChanged(boolean enabled) {
        synchronized (database) {
            if (!enabled) {
                baseline = null;
                scanGeneration++;
            } else if (baseline == null && !libraryTab.isModified()) {
                baseline = captureBaseline();
            }
        }
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
            BackgroundTask.wrap(() -> scanner.scanForChanges(this::awaitStableFile))
                          .onSuccess(changes -> {
                              if (generation != scanGeneration) {
                                  LOGGER.debug("Discarding result of a scan overtaken by a newer file change");
                                  return;
                              }
                              // Sorting the changes on the FX thread right before applying them leaves no window
                              // for a user edit to slip in between classification and application
                              synchronize(scannedBaseline, scanner.triage(scannedBaseline, changes));
                          })
                          .onFailure(e -> LOGGER.error("Error while synchronizing with the library file", e))
                          .executeWith(taskExecutor);
            return;
        }
        BackgroundTask.wrap(() -> scanner.scanForChanges(this::awaitStableFile))
                      .onSuccess(changes -> {
                          if (!changes.isEmpty()) {
                              listeners.forEach(listener -> listener.databaseChanged(changes));
                          }
                      })
                      .onFailure(e -> LOGGER.error("Error while watching for changes", e))
                      .executeWith(taskExecutor);
    }

    /// Sync clients and editors may write the file in several steps. A file that is still growing must not be parsed:
    /// half of a library parses fine and would look like every later entry had been deleted. Waits (bounded) until
    /// size and modification time stop changing; the last snapshot seen becomes the known disk state.
    private void awaitStableFile() {
        Path path = monitoredPath.orElse(null);
        if (path == null) {
            return;
        }
        FileSnapshot last = FileSnapshot.read(path);
        for (int attempt = 0; attempt < STABLE_FILE_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(STABLE_FILE_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            FileSnapshot current = FileSnapshot.read(path);
            if (Objects.equals(current, last)) {
                break;
            }
            last = current;
        }
        synchronized (database) {
            knownDiskState = last;
        }
    }

    /// Applies what changed on disk only, and offers the review for what changed on both sides.
    private void synchronize(LibraryBaseline scannedBaseline, LibraryBaseline.Triage triage) {
        List<DatabaseChange> unresolved = new ArrayList<>(triage.bothSides());
        unresolved.addAll(triage.memoryOnly());
        if (!triage.diskOnly().isEmpty()) {
            applyResolvedChanges(triage.diskOnly(), unresolved.isEmpty() && !libraryTab.isModified());
            dialogService.notify(Localization.lang("Merged %0 change(s) from the library file", String.valueOf(triage.diskOnly().size())));
        }
        synchronized (database) {
            LibraryBaseline updated = captureBaseline();
            if (updated != null) {
                updated.keepUnresolved(scannedBaseline, unresolved);
            }
            baseline = updated;
        }
        if (!triage.bothSides().isEmpty()) {
            listeners.forEach(listener -> listener.databaseChanged(triage.bothSides()));
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
            libraryTab.markBaseChanged();
        }
    }

    public void addListener(DatabaseChangeListener listener) {
        listeners.add(listener);
    }

    public void unregister() {
        scanGeneration++;
        monitoredPath.ifPresent(path -> {
            fileMonitor.removeListener(path, this);
            if (database.getLocation() == DatabaseLocation.LOCAL) {
                preferences.getLibraryPreferences().autoSaveProperty().removeListener(synchronizingListener);
            }
        });
    }
}
