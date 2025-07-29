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

import com.tobiasdiez.easybind.EasyBind;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// ViewModel that holds current Git sync status for the open .bib database.
/// It maintains the state of the GitHandler bound to the current file path, including:
///
/// <ul>
///   <li>Whether the current file is inside a Git repository</li>
///   <li>Whether the file is tracked by Git</li>
///   <li>Whether there are unresolved merge conflicts</li>
///   <li>The current sync status (e.g., {@code UP_TO_DATE}, {@code DIVERGED}, etc.)</li>
/// </ul>
public class GitStatusViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitStatusViewModel.class);
    private final StateManager stateManager;
    private final ObjectProperty<BibDatabaseContext> databaseContext = new SimpleObjectProperty<>();
    private final ObjectProperty<SyncStatus> syncStatus = new SimpleObjectProperty<>(SyncStatus.UNTRACKED);
    private final BooleanProperty isTracking = new SimpleBooleanProperty(false);
    private final BooleanProperty conflictDetected = new SimpleBooleanProperty(false);
    // "" denotes that no commit was pulled
    private final StringProperty lastPulledCommit = new SimpleStringProperty("");
    private @Nullable GitHandler activeHandler = null;

    public GitStatusViewModel(StateManager stateManager, Path bibFilePath) {
        this.stateManager = stateManager;
        EasyBind.subscribe(stateManager.activeDatabaseProperty(), newDb -> {
            if (newDb != null && newDb.isPresent() && newDb.get().getDatabasePath().isPresent()) {
                BibDatabaseContext databaseContext1 = newDb.get();
                databaseContext.set(databaseContext1);
                updateStatusFromContext(databaseContext1);
            } else {
                LOGGER.debug("No active database with path; resetting Git status.");
                reset();
            }
        });

        stateManager.getActiveDatabase().ifPresent(ctx -> {
            databaseContext.set(ctx);
            updateStatusFromContext(ctx);
        });
    }

    protected void updateStatusFromContext(BibDatabaseContext context) {
        Optional<Path> maybePath = context.getDatabasePath();
        if (maybePath.isEmpty()) {
            LOGGER.debug("No .bib file path available in database context; resetting Git status.");
            reset();
            return;
        }

        Path path = maybePath.get();

        Optional<GitHandler> maybeHandler = GitHandler.fromAnyPath(path);
        if (maybeHandler.isEmpty()) {
            LOGGER.debug("No Git repository found for path {}; resetting Git status.", path);
            reset();
            return;
        }
        this.activeHandler = maybeHandler.get();

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
        activeHandler = null;
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
        return Optional.ofNullable(activeHandler);
    }
}
