package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;

import javafx.application.Platform;
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
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.model.database.BibDatabaseContext;

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
    private final StateManager stateManager;
    private final GitHandlerRegistry handlerRegistry;
    private final ObjectProperty<SyncStatus> syncStatus = new SimpleObjectProperty<>(SyncStatus.UNTRACKED);
    private final BooleanProperty isTracking = new SimpleBooleanProperty(false);
    private final BooleanProperty conflictDetected = new SimpleBooleanProperty(false);
    private final StringProperty lastPulledCommit = new SimpleStringProperty("");
    private final BooleanProperty hasRemoteConfigured = new SimpleBooleanProperty(false);

    public GitStatusViewModel(StateManager stateManager, GitHandlerRegistry handlerRegistry) {
        this.stateManager = stateManager;
        this.handlerRegistry = handlerRegistry;

        stateManager.activeDatabaseProperty().addListener((obs, oldDb, newDb) -> {
            if ((newDb != null) && newDb.isPresent() && newDb.get().getDatabasePath().isPresent()) {
                Path path = newDb.get().getDatabasePath().get();
                refresh(path);
                updateRemoteStatus(path);
            } else {
                reset();
                hasRemoteConfigured.set(false);
            }
        });

        stateManager.getActiveDatabase()
                    .flatMap(BibDatabaseContext::getDatabasePath)
                    .ifPresent(path -> {
                        refresh(path);
                        updateRemoteStatus(path);
                    });
    }

    protected void updateStatusFromContext(BibDatabaseContext context) {
        Path path = context.getDatabasePath().orElse(null);
        if (path == null) {
            reset();
            return;
        }

        refresh(path);
    }

    public void refresh(Path path) {
        handlerRegistry.fromAnyPath(path).ifPresentOrElse(handler -> {
            GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(path);
            setTracking(snapshot.tracking());
            setSyncStatus(snapshot.syncStatus());
            setConflictDetected(snapshot.conflict());
            snapshot.lastPulledCommit().ifPresent(this::setLastPulledCommit);
        }, this::reset);
    }

    public void reset() {
        setSyncStatus(SyncStatus.UNTRACKED);
        setTracking(false);
        setConflictDetected(false);
        setLastPulledCommit("");
    }

    public BooleanProperty hasRemoteConfiguredProperty() {
        return hasRemoteConfigured;
    }

    public void updateRemoteStatus(Path databasePath) {
        Platform.runLater(() -> {
            GitHandler handler = handlerRegistry.get(databasePath.getParent());
            boolean hasRemote = handler != null && handler.hasRemote("origin");
            hasRemoteConfigured.set(hasRemote);
        });
    }

    public Optional<Path> currentBibPath() {
        return stateManager.getActiveDatabase().flatMap(BibDatabaseContext::getDatabasePath);
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

    public static GitStatusViewModel fromPathAndContext(StateManager stateManager, GitHandlerRegistry handlerRegistry, Path path, BibDatabaseContext context) {
        GitStatusViewModel viewModel = new GitStatusViewModel(stateManager, handlerRegistry);
        viewModel.refresh(path);
        return viewModel;
    }
}
