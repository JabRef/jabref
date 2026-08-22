package org.jabref.gui.collab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.Notifications;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import com.dlsc.gemsfx.infocenter.NotificationAction;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseChangeMonitor implements FileUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseChangeMonitor.class);

    private final BibDatabaseContext database;
    private final FileUpdateMonitor fileMonitor;
    private final List<DatabaseChangeListener> listeners;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final Optional<Path> monitoredPath;
    private LibraryTab saveState;
    private boolean changeDetectionSuspended;

    /// Size and modification time of the monitored file as of the last scan or the last point where the in-memory
    /// library was known to match the disk (library load, successful save, all external changes merged). Guarded by
    /// `synchronized (database)`; `null` when unknown, in which case the next event triggers a full scan.
    @Nullable private FileState knownDiskState;

    private record FileState(long size, FileTime lastModified) {
        @Nullable
        static FileState read(Path file) {
            try {
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                return new FileState(attributes.size(), attributes.lastModifiedTime());
            } catch (IOException exception) {
                LOGGER.debug("Could not read attributes of {}", file, exception);
                return null;
            }
        }
    }

    public DatabaseChangeMonitor(BibDatabaseContext database,
                                 FileUpdateMonitor fileMonitor,
                                 TaskExecutor taskExecutor,
                                 DialogService dialogService,
                                 GuiPreferences preferences,
                                 UndoManager undoManager,
                                 StateManager stateManager) {
        this.database = database;
        this.fileMonitor = fileMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.monitoredPath = this.database.getDatabasePath();

        this.listeners = new ArrayList<>();

        monitoredPath.ifPresent(path -> {
            knownDiskState = FileState.read(path);
            try {
                fileMonitor.addListenerForFile(path, this);
            } catch (IOException e) {
                LOGGER.error("Error while trying to monitor {}", path, e);
            }
        });

        addListener(changes -> dialogService.notify(new ExternalLibraryChangeNotification(changes)));
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
                remove();
                return OnClickBehaviour.REMOVE;
            });

            NotificationAction<Path> reviewAction = new NotificationAction<>(Localization.lang("Review changes"), _ -> {
                DatabaseChangesResolverDialog databaseChangesResolverDialog = new DatabaseChangesResolverDialog(
                        changes,
                        database,
                        Localization.lang("External Changes Resolver"));
                Optional<Boolean> areAllChangesResolved = dialogService.showCustomDialogAndWait(databaseChangesResolverDialog);
                saveState = stateManager.activeTabProperty().get().get();

                final NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Merged external changes"));
                changes.stream()
                       .filter(DatabaseChange::isAccepted)
                       .forEach(change -> change.applyChange(compoundEdit));
                compoundEdit.end();
                undoManager.addEdit(compoundEdit);

                if (areAllChangesResolved.get()) {
                    if (databaseChangesResolverDialog.areAllChangesAccepted()) {
                        // In case all changes of the file on disk are merged into the current in-memory file, the file on disk does not differ from the in-memory file
                        saveState.resetChangedProperties();
                    } else {
                        saveState.markBaseChanged();
                    }
                }

                remove();
                return OnClickBehaviour.REMOVE;
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

    /// Records the current file state as consistent with the in-memory library. To be called whenever the two are
    /// known to match (successful save, all external changes merged), so that watcher events reflecting that very
    /// state do not trigger a scan. A concurrent write between the point of consistency and this call is recorded as
    /// consistent and its notification missed — a tiny race accepted here, since lost-update protection lives in
    /// [org.jabref.logic.exporter.AtomicFileOutputStream] and not in this notification path.
    public void markConsistentWithDisk() {
        synchronized (database) {
            monitoredPath.ifPresent(path -> knownDiskState = FileState.read(path));
        }
    }

    /// A full scan parses the whole library file, so it is skipped when size and modification time show that the file
    /// has not actually changed since the last known-consistent state — e.g. for events caused by JabRef's own save.
    private void scanIfFileChanged() {
        FileState currentState = monitoredPath.map(FileState::read).orElse(null);
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
        BackgroundTask.wrap(scanner::scanForChanges)
                      .onSuccess(changes -> {
                          if (!changes.isEmpty()) {
                              listeners.forEach(listener -> listener.databaseChanged(changes));
                          }
                      })
                      .onFailure(e -> LOGGER.error("Error while watching for changes", e))
                      .executeWith(taskExecutor);
    }

    public void addListener(DatabaseChangeListener listener) {
        listeners.add(listener);
    }

    public void unregister() {
        monitoredPath.ifPresent(path -> fileMonitor.removeListener(path, this));
    }
}
