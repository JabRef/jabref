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
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.status.SyncStatus;

/**
 * ViewModel that holds current Git sync status for the open .bib database.
 * 统一维护当前路径绑定的 GitHandler 状态，包括：
 * - 是否是 Git 仓库
 * - 当前是否被 Git 跟踪
 * - 是否存在冲突
 * - 当前同步状态（UP_TO_DATE、DIVERGED 等）
 */
public class GitStatusViewModel extends AbstractViewModel {
    private final Path currentBibFile;
    private final ObjectProperty<SyncStatus> syncStatus = new SimpleObjectProperty<>(SyncStatus.UNTRACKED);
    private final BooleanProperty isTracking = new SimpleBooleanProperty(false);
    private final BooleanProperty conflictDetected = new SimpleBooleanProperty(false);
    private final StringProperty lastPulledCommit = new SimpleStringProperty("");
    private GitHandler activeHandler = null;

    public GitStatusViewModel(Path bibFilePath) {
        this.currentBibFile = bibFilePath;
        updateStatusFromPath(bibFilePath);
    }

    /**
     * Try to detect Git repository status from the given file or folder path.
     *
     * @param fileOrFolderInRepo Any path (file or folder) assumed to be inside a Git repository
     */
    public void updateStatusFromPath(Path fileOrFolderInRepo) {
        Optional<GitHandler> maybeHandler = GitHandler.fromAnyPath(fileOrFolderInRepo);

        if (!maybeHandler.isPresent()) {
            reset();
            return;
        }

        GitHandler handler = maybeHandler.get();
        this.activeHandler = handler;

        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(fileOrFolderInRepo);

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

    public Path getCurrentBibFile() {
        return currentBibFile;
    }
}
