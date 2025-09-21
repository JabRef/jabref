package org.jabref.gui.collab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.controlsfx.control.action.Action;
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
    private final LibraryTab.DatabaseNotification notificationPane;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private LibraryTab saveState;

    public DatabaseChangeMonitor(BibDatabaseContext database,
                                 FileUpdateMonitor fileMonitor,
                                 TaskExecutor taskExecutor,
                                 DialogService dialogService,
                                 GuiPreferences preferences,
                                 LibraryTab.DatabaseNotification notificationPane,
                                 UndoManager undoManager,
                                 StateManager stateManager) {
        this.database = database;
        this.fileMonitor = fileMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.notificationPane = notificationPane;
        this.undoManager = undoManager;
        this.stateManager = stateManager;

        this.listeners = new ArrayList<>();

        this.database.getDatabasePath().ifPresent(path -> {
            try {
                fileMonitor.addListenerForFile(path, this);
            } catch (IOException e) {
                LOGGER.error("Error while trying to monitor {}", path, e);
            }
        });

        addListener(this::notifyOnChange);
    }

    private void notifyOnChange(List<DatabaseChange> changes) {
        // The changes come from {@link org.jabref.gui.collab.DatabaseChangeList.compareAndGetChanges}
        notificationPane.notify(
                IconTheme.JabRefIcons.SAVE.getGraphicNode(),
                Localization.lang("The library has been modified by another program."),
                List.of(new Action(Localization.lang("Dismiss changes"), _ -> notificationPane.hide()),
                        new Action(Localization.lang("Review changes"), _ -> {
                            DatabaseChangesResolverDialog databaseChangesResolverDialog = new DatabaseChangesResolverDialog(changes, database, Localization.lang("External Changes Resolver"));
                            Optional<Boolean> areAllChangesResolved = dialogService.showCustomDialogAndWait(databaseChangesResolverDialog);
                            saveState = stateManager.activeTabProperty().get().get();
                            final NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Merged external changes"));
                            changes.stream().filter(DatabaseChange::isAccepted).forEach(change -> change.applyChange(compoundEdit));
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
                            notificationPane.hide();
                        })),
                Duration.ZERO);
    }

    @Override
    public void fileUpdated() {
        synchronized (database) {
            // File on disk has changed, thus look for notable changes and notify listeners in case there are such changes
            ChangeScanner scanner = new ChangeScanner(database, dialogService, preferences);
            BackgroundTask.wrap(scanner::scanForChanges)
                          .onSuccess(changes -> {
                              if (!changes.isEmpty()) {
                                  listeners.forEach(listener -> listener.databaseChanged(changes));
                              }
                          })
                          .onFailure(e -> LOGGER.error("Error while watching for changes", e))
                          .executeWith(taskExecutor);
        }
    }

    public void addListener(DatabaseChangeListener listener) {
        listeners.add(listener);
    }

    public void unregister() {
        database.getDatabasePath().ifPresent(file -> fileMonitor.removeListener(file, this));
    }
}
