package org.jabref.logic.git.status;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.io.GitRevisionLocator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to determine the status of a Git repository from any given path inside it.
 * If no repository is found, it returns a {@link GitStatusSnapshot} with tracking = false.
 * Otherwise, it returns a full snapshot including tracking status, sync status, and conflict state.
 */
public class GitStatusChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitStatusChecker.class);

    public static GitStatusSnapshot checkStatus(Path anyPathInsideRepo) {
        Optional<GitHandler> maybeHandler = GitHandler.fromAnyPath(anyPathInsideRepo);

        if (maybeHandler.isEmpty()) {
            return new GitStatusSnapshot(
                    false,
                    SyncStatus.UNTRACKED,
                    false,
                    false,
                    Optional.empty()
            );
        }
        GitHandler handler = maybeHandler.get();

        try (Git git = Git.open(handler.getRepositoryPathAsFile())) {
            Repository repo = git.getRepository();
            Status status = git.status().call();
            boolean hasConflict = !status.getConflicting().isEmpty();
            boolean hasUncommittedChanges = !status.isClean();

            ObjectId localHead = repo.resolve("HEAD");
            String trackingBranch = new BranchConfig(repo.getConfig(), repo.getBranch()).getTrackingBranch();
            ObjectId remoteHead = trackingBranch != null ? repo.resolve(trackingBranch) : null;

            SyncStatus syncStatus = determineSyncStatus(repo, localHead, remoteHead);

            return new GitStatusSnapshot(
                    true,
                    syncStatus,
                    hasConflict,
                    hasUncommittedChanges,
                    Optional.ofNullable(localHead).map(ObjectId::getName)
            );
        } catch (IOException | GitAPIException e) {
            LOGGER.warn("Failed to check Git status", e);
            return new GitStatusSnapshot(
                    true,
                    SyncStatus.UNKNOWN,
                    false,
                    false,
                    Optional.empty()
            );
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

        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit localCommit = walk.parseCommit(localHead);
            RevCommit remoteCommit = walk.parseCommit(remoteHead);
            RevCommit mergeBase = GitRevisionLocator.findMergeBase(repo, localCommit, remoteCommit);

            boolean ahead = !localCommit.equals(mergeBase);
            boolean behind = !remoteCommit.equals(mergeBase);

            if (ahead && behind) {
                return SyncStatus.DIVERGED;
            } else if (ahead) {
                return SyncStatus.AHEAD;
            } else if (behind) {
                return SyncStatus.BEHIND;
            } else {
                LOGGER.debug("Could not determine git sync status. All commits differ or mergeBase is null.");
                return SyncStatus.UNKNOWN;
            }
        }
    }
}
