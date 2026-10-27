package org.jabref.logic.git.status;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.io.GitRevisionLocator;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitExceptionUtil;
import org.jabref.logic.l10n.Localization;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// This class is used to determine the status of a Git repository from any given path inside it.
/// If no repository is found, it returns a [GitStatusSnapshot] with tracking = false.
/// Otherwise, it returns a full snapshot including tracking status, sync status, and conflict state.
public class GitStatusChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitStatusChecker.class);

    public static GitStatusSnapshot checkStatus(GitHandler gitHandler) {
        try {
            return checkStatusOrThrow(gitHandler);
        } catch (JabRefException e) {
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

    public static GitStatusSnapshot checkStatusOrThrow(GitHandler gitHandler) throws JabRefException {
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
                // [impl->req~git.commit.remote-independent~1]
                syncStatus = determineSyncStatusWithoutRemoteHead(gitHandler);
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
        } catch (IOException | GitAPIException | JGitInternalException e) {
            throw translateStatusFailure(e);
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

    public static GitStatusSnapshot checkStatusAndFetch(GitHandler gitHandler) throws JabRefException {
        gitHandler.fetchOnCurrentBranch();
        return checkStatusOrThrow(gitHandler);
    }

    /// The remote is only consulted to tell "remote exists but is empty" apart from "cannot tell".
    /// Any failure to reach it (no `origin`, unsupported or unreachable URI, ...) must not affect the
    /// local working-tree result the caller relies on, so it degrades to [SyncStatus#UNKNOWN].
    private static SyncStatus determineSyncStatusWithoutRemoteHead(GitHandler gitHandler) {
        if (!gitHandler.hasRemote("origin")) {
            LOGGER.debug("No origin remote configured -> UNKNOWN");
            return SyncStatus.UNKNOWN;
        }
        try {
            if (isRemoteEmpty(gitHandler)) {
                LOGGER.debug("Remote has NO heads -> REMOTE_EMPTY");
                return SyncStatus.REMOTE_EMPTY;
            }
            LOGGER.debug("Remote is NOT empty but remoteHead unresolved -> UNKNOWN");
            return SyncStatus.UNKNOWN;
        } catch (IOException | GitAPIException | JGitInternalException e) {
            LOGGER.warn("Could not query remote origin", e);
            return SyncStatus.UNKNOWN;
        }
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

    public static boolean isRemoteEmpty(GitHandler gitHandler) throws IOException, GitAPIException {
        try (Git git = Git.open(gitHandler.getRepositoryPathAsFile())) {
            LsRemoteCommand lsRemoteCommand = git.lsRemote()
                                                 .setRemote("origin")
                                                 .setHeads(true);
            gitHandler.getCredentialsProvider().ifPresent(lsRemoteCommand::setCredentialsProvider);
            Iterable<Ref> heads = lsRemoteCommand.call();
            boolean empty = (heads == null) || !heads.iterator().hasNext();
            if (empty) {
                LOGGER.debug("ls-remote: origin has NO heads.");
            } else {
                LOGGER.debug("ls-remote: origin has heads.");
            }
            return empty;
        }
    }

    private static JabRefException translateStatusFailure(Exception exception) {
        LOGGER.warn("Failed to check Git status", exception);
        if (GitExceptionUtil.isLockFailure(exception)) {
            return new JabRefException(
                    "Failed to check Git status because the repository is locked",
                    Localization.lang("The Git repository is locked. Close other Git, JabRef, or IDE processes and try again."),
                    exception);
        }
        if (GitExceptionUtil.isMissingObjectFailure(exception)) {
            return new JabRefException(
                    "Failed to check Git status because the repository is incomplete or corrupted",
                    Localization.lang("The local Git repository is incomplete or corrupted. Remove the broken .git directory in that folder or choose another folder, then try again."),
                    exception);
        }
        return new JabRefException(
                "Failed to check Git status",
                Localization.lang("Could not read the Git repository status. Please check the repository and try again."),
                exception);
    }
}
