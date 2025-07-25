package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.status.SyncStatus;
import org.jabref.model.database.BibDatabaseContext;

/**
 * ViewModel that holds current Git sync status for the open .bib database.
 * It maintains the state of the GitHandler bound to the current file path, including:
 * - Whether the current file is inside a Git repository
 * - Whether the file is tracked by Git
 * - Whether there are unresolved merge conflicts
 * - The current sync status (e.g., UP_TO_DATE, DIVERGED, etc.)
 */
public class GitStatusViewModel extends AbstractViewModel {
    private final StateManager stateManager;
    private final ObjectProperty<BibDatabaseContext> databaseContext = new SimpleObjectProperty<>();
    private final ObjectProperty<SyncStatus> syncStatus = new SimpleObjectProperty<>(SyncStatus.UNTRACKED);
    private final BooleanProperty isTracking = new SimpleBooleanProperty(false);
    private final BooleanProperty conflictDetected = new SimpleBooleanProperty(false);
    private final StringProperty lastPulledCommit = new SimpleStringProperty("");
    private Optional<GitHandler> activeHandler = Optional.empty();

    public GitStatusViewModel(StateManager stateManager, Path bibFilePath) {
        this.stateManager = stateManager;
        stateManager.activeDatabaseProperty().addListener((obs, oldDb, newDb) -> {
            if (newDb != null && newDb.isPresent() && newDb.get().getDatabasePath().isPresent()) {
                BibDatabaseContext ctx = newDb.get();
                databaseContext.set(ctx);
                updateStatusFromContext(ctx);
            } else {
                reset();
            }
        });

        stateManager.getActiveDatabase().ifPresent(ctx -> {
            databaseContext.set(ctx);
            updateStatusFromContext(ctx);
        });
    }

    protected void updateStatusFromContext(BibDatabaseContext context) {
        Path path = context.getDatabasePath().orElse(null);
        if (path == null) {
            reset();
            return;
        }

        Optional<GitHandler> maybeHandler = GitHandler.fromAnyPath(path);
        if (maybeHandler.isEmpty()) {
            reset();
            return;
        }

        this.activeHandler = maybeHandler;

        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(path);
        setTracking(snapshot.tracking());
        setSyncStatus(snapshot.syncStatus());
        setConflictDetected(snapshot.conflict());
        snapshot.lastPulledCommit().ifPresent(this::setLastPulledCommit);
    }

    /**
     * Clears all internal state to defaults.
     * Should be called when switching projects or Git context is lost
     */
    public void reset() {
        setSyncStatus(SyncStatus.UNTRACKED);
        setTracking(false);
        setConflictDetected(false);
        setLastPulledCommit("");
    }

    public Optional<BibDatabaseContext> getDatabaseContext() {
        return Optional.ofNullable(databaseContext.get());
    }

    public Optional<Path> getCurrentBibFile() {
        return getDatabaseContext()
                .flatMap(BibDatabaseContext::getDatabasePath);
    }

    public ObjectProperty<SyncStatus> syncStatusProperty() {
        return syncStatus;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus.get();
    }

    public void setSyncStatus(SyncStatus status) {
        this.syncStatus.set(status);
    }

    public BooleanProperty isTrackingProperty() {
        return isTracking;
    }

    public boolean isTracking() {
        return isTracking.get();
    }

    public void setTracking(boolean tracking) {
        this.isTracking.set(tracking);
    }

    public BooleanProperty conflictDetectedProperty() {
        return conflictDetected;
    }

    public boolean isConflictDetected() {
        return conflictDetected.get();
    }

    public void setConflictDetected(boolean conflict) {
        this.conflictDetected.set(conflict);
    }

    public StringProperty lastPulledCommitProperty() {
        return lastPulledCommit;
    }

    public String getLastPulledCommit() {
        return lastPulledCommit.get();
    }

    public void setLastPulledCommit(String commitHash) {
        this.lastPulledCommit.set(commitHash);
    }

    public Optional<GitHandler> getActiveHandler() {
        return activeHandler;
    }
}
