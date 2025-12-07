package org.jabref.logic.git.status;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.io.GitRevisionLocator;
import org.jabref.logic.git.preferences.GitPreferences;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to determine the status of a Git repository from any given path inside it.
 * If no repository is found, it returns a {@link GitStatusSnapshot} with tracking = false.
 * Otherwise, it returns a full snapshot including tracking status, sync status, and conflict state.
 */
public class GitStatusChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitStatusChecker.class);

    public static GitStatusSnapshot checkStatus(GitHandler gitHandler) {
        try (Git git = Git.open(gitHandler.getRepositoryPathAsFile())) {
            Repository repo = git.getRepository();
            Status status = git.status().call();
            boolean hasConflict = !status.getConflicting().isEmpty();
            boolean hasUncommittedChanges = !status.isClean();

            ObjectId localHead = repo.resolve("HEAD");
            String trackingBranch = new BranchConfig(repo.getConfig(), repo.getBranch()).getTrackingBranch();
            ObjectId remoteHead = trackingBranch != null ? repo.resolve(trackingBranch) : null;

            SyncStatus syncStatus;

            if (remoteHead == null) {
                boolean remoteEmpty = isRemoteEmpty(gitHandler);
                if (remoteEmpty) {
                    LOGGER.debug("Remote has NO heads -> REMOTE_EMPTY");
                    syncStatus = SyncStatus.REMOTE_EMPTY;
                } else {
                    LOGGER.debug("Remote is NOT empty but remoteHead unresolved -> UNKNOWN");
                    syncStatus = SyncStatus.UNKNOWN;
                }
            } else {
                syncStatus = determineSyncStatus(repo, localHead, remoteHead);
            }

            return new GitStatusSnapshot(
                    GitStatusSnapshot.TRACKING,
                    syncStatus,
                    hasConflict,
                    hasUncommittedChanges,
                    Optional.ofNullable(localHead).map(ObjectId::getName)
            );
        } catch (IOException | GitAPIException e) {
            LOGGER.warn("Failed to check Git status", e);
            return new GitStatusSnapshot(
                    GitStatusSnapshot.TRACKING,
                    SyncStatus.UNKNOWN,
                    !GitStatusSnapshot.CONFLICT,
                    !GitStatusSnapshot.UNCOMMITTED,
                    Optional.empty()
            );
        }
    }

    public static GitStatusSnapshot checkStatus(Path anyPathInsideRepo, GitPreferences gitPreferences) {
        return GitHandler.fromAnyPath(anyPathInsideRepo, gitPreferences)
                         .map(GitStatusChecker::checkStatus)
                         .orElse(new GitStatusSnapshot(
                                 !GitStatusSnapshot.TRACKING,
                                 SyncStatus.UNTRACKED,
                                 !GitStatusSnapshot.CONFLICT,
                                 !GitStatusSnapshot.UNCOMMITTED,
                                 Optional.empty()
                         ));
    }

    public static GitStatusSnapshot checkStatusAndFetch(GitHandler gitHandler) throws IOException, JabRefException {
        gitHandler.fetchOnCurrentBranch();
        return checkStatus(gitHandler);
    }

    private static SyncStatus determineSyncStatus(Repository repo, ObjectId localHead, ObjectId remoteHead) throws IOException {
        if (localHead == null || remoteHead == null) {
            LOGGER.debug("localHead or remoteHead null");
            return SyncStatus.UNKNOWN;
        }

        if (localHead.equals(remoteHead)) {
            return SyncStatus.UP_TO_DATE;
        }

        boolean remoteInLocal = GitRevisionLocator.isAncestor(repo, remoteHead, localHead);
        boolean localInRemote = GitRevisionLocator.isAncestor(repo, localHead, remoteHead);

        if (remoteInLocal && localInRemote) {
            return SyncStatus.UP_TO_DATE;
        } else if (remoteInLocal) {
            return SyncStatus.AHEAD;
        } else if (localInRemote) {
            return SyncStatus.BEHIND;
        } else {
            return SyncStatus.DIVERGED;
        }
    }

    public static boolean isRemoteEmpty(GitHandler gitHandler) {
        try (Git git = Git.open(gitHandler.getRepositoryPathAsFile())) {
            Iterable<Ref> heads = git.lsRemote()
                                     .setRemote("origin")
                                     .setHeads(true)
                                     .call();
            boolean empty = (heads == null) || !heads.iterator().hasNext();
            if (empty) {
                LOGGER.debug("ls-remote: origin has NO heads.");
            } else {
                LOGGER.debug("ls-remote: origin has heads.");
            }
            return empty;
        } catch (IOException | GitAPIException e) {
            LOGGER.debug("ls-remote failed when checking remote emptiness; assume NOT empty.", e);
            return false;
        }
    }
}
