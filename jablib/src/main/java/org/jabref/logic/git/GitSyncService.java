package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.io.GitRevisionLocator;
import org.jabref.logic.git.io.RevisionTriple;
import org.jabref.logic.git.merge.GitMergeUtil;
import org.jabref.logic.git.merge.GitSemanticMergeExecutor;
import org.jabref.logic.git.model.PullResult;
import org.jabref.logic.git.model.PushResult;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.status.SyncStatus;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// GitSyncService currently serves as an orchestrator for Git pull/push logic.
///
/// if (hasConflict)
///     → UI merge;
/// else
///     → autoMerge := local + remoteDiff
public class GitSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);

    private static final boolean AMEND = true;
    private final ImportFormatPreferences importFormatPreferences;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final GitConflictResolverStrategy gitConflictResolverStrategy;
    private final GitSemanticMergeExecutor mergeExecutor;

    public GitSyncService(ImportFormatPreferences importFormatPreferences, GitHandlerRegistry gitHandlerRegistry, GitConflictResolverStrategy gitConflictResolverStrategy, GitSemanticMergeExecutor mergeExecutor) {
        this.importFormatPreferences = importFormatPreferences;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.gitConflictResolverStrategy = gitConflictResolverStrategy;
        this.mergeExecutor = mergeExecutor;
    }

    public PullResult fetchAndMerge(BibDatabaseContext localDatabaseContext, Path bibFilePath) throws GitAPIException, IOException, JabRefException {
        Optional<Path> repoRoot = GitHandler.findRepositoryRoot(bibFilePath);
        if (repoRoot.isEmpty()) {
            throw new JabRefException("Pull aborted: Path is not inside a Git repository.");
        }
        GitHandler gitHandler = gitHandlerRegistry.get(repoRoot.get());

        gitHandler.fetchOnCurrentBranch();
        GitStatusSnapshot status = GitStatusChecker.checkStatus(gitHandler);

        if (!status.tracking()) {
            throw new JabRefException("Pull aborted: The file is not under Git version control.");
        }

        if (status.conflict()) {
            throw new JabRefException("Pull aborted: Local repository has unresolved merge conflicts.");
        }

        // Prevent rollback from overwriting the user's uncommitted changes
        if (status.uncommittedChanges()) {
            throw new JabRefException("Pull aborted: Local changes have not been committed.");
        }

        if (status.syncStatus() == SyncStatus.UP_TO_DATE) {
            return PullResult.noopUpToDate();
        }
        if (status.syncStatus() == SyncStatus.AHEAD) {
            return PullResult.noopAhead();
        }

        try (Git git = gitHandler.open()) {
            // 1. Locate base / local / remote commits
            GitRevisionLocator locator = new GitRevisionLocator();
            RevisionTriple triple = locator.locateMergeCommits(git);
            RevCommit remoteCommit = triple.remote();

            if (status.syncStatus() == SyncStatus.BEHIND) {
                gitHandler.fastForwardTo(remoteCommit);
                return PullResult.merged(List.of());
            }

            // 2. Perform semantic merge
            if (status.syncStatus() == SyncStatus.DIVERGED) {
                try (GitHandler.MergeGuard guard = gitHandler.beginSemanticMergeGuard(remoteCommit, bibFilePath)) {
                    PullResult result = performSemanticMerge(git, triple.base(), remoteCommit, localDatabaseContext, bibFilePath);

                    if (result.isSuccessful()) {
                        guard.commit("Auto-merged by JabRef");
                    }
                    return result;
                }
            }

            throw new JabRefException("Pull aborted: Unsupported sync status " + status.syncStatus());
        }
    }

    public PullResult performSemanticMerge(Git git,
                                           Optional<RevCommit> baseCommitOpt,
                                           RevCommit remoteCommit,
                                           BibDatabaseContext localDatabaseContext,
                                           Path bibFilePath) throws IOException, JabRefException, GitAPIException {

        Path bibPath = bibFilePath.toRealPath();
        Path workTree = git.getRepository().getWorkTree().toPath().toRealPath();
        Path relativePath;

        if (!bibPath.startsWith(workTree)) {
            throw new IllegalStateException("Given .bib file is not inside repository");
        }
        relativePath = workTree.relativize(bibPath);

        // 1. Load three versions
        BibDatabaseContext base;
        if (baseCommitOpt.isPresent()) {
            Optional<String> baseContent = GitFileReader.readFileFromCommit(git, baseCommitOpt.get(), relativePath);
            base = baseContent.isEmpty() ? BibDatabaseContext.empty() : BibDatabaseContext.of(baseContent.get(), importFormatPreferences);
        } else {
            base = new BibDatabaseContext();
        }

        Optional<String> remoteContent = GitFileReader.readFileFromCommit(git, remoteCommit, relativePath);
        BibDatabaseContext remote = remoteContent.isEmpty() ? BibDatabaseContext.empty() : BibDatabaseContext.of(remoteContent.get(), importFormatPreferences);
        BibDatabaseContext local = localDatabaseContext;

        // 2. Conflict detection
        List<ThreeWayEntryConflict> conflicts = SemanticConflictDetector.detectConflicts(base, local, remote);

        BibDatabaseContext localPrime;
        if (conflicts.isEmpty()) {
            localPrime = local;
            // No conflict: let logic write merged result to disk
            mergeExecutor.merge(base, localPrime, remote, bibFilePath);
        } else {
            // 3. If there are conflicts, ask strategy to resolve
            List<BibEntry> resolved = gitConflictResolverStrategy.resolveConflicts(conflicts);
            if (resolved.isEmpty()) {
                throw new JabRefException("Merge aborted: Conflict resolution was canceled or denied.");
            }
            localPrime = GitMergeUtil.replaceEntries(local, resolved);
        }

        return PullResult.merged(localPrime.getDatabase().getEntries());
    }

    public PushResult push(BibDatabaseContext localDatabaseContext, Path bibFilePath) throws GitAPIException, IOException, JabRefException {
        Optional<Path> repoRoot = GitHandler.findRepositoryRoot(bibFilePath);

        if (repoRoot.isEmpty()) {
            throw new JabRefException("Push aborted: Path is not inside a Git repository.");
        }
        GitHandler gitHandler = gitHandlerRegistry.get(repoRoot.get());

        gitHandler.fetchOnCurrentBranch();

        GitStatusSnapshot status = GitStatusChecker.checkStatus(gitHandler);

        if (!status.tracking()) {
            throw new JabRefException("Push aborted: The file is not under Git version control.");
        }

        if (status.uncommittedChanges()) {
            throw new JabRefException("Push aborted: Local changes have not been committed.");
        }

        SyncStatus sync = status.syncStatus();
        switch (sync) {
            case UP_TO_DATE -> {
                return PushResult.noopUpToDate();
            }

            case AHEAD -> {
                gitHandler.pushCommitsToRemoteRepository();
                return PushResult.pushed();
            }

            case CONFLICT -> {
                throw new JabRefException("Push aborted: Local repository has unresolved merge conflicts.");
            }

            case UNTRACKED -> {
                throw new JabRefException("Push aborted: Untracked file.");
            }

            case UNKNOWN -> {
                throw new JabRefException("Push aborted: Unknown Git status.");
            }

            case BEHIND,
                 DIVERGED -> {
                fetchAndMerge(localDatabaseContext, bibFilePath);
                status = GitStatusChecker.checkStatus(gitHandler);
                if (status.conflict()) {
                    throw new JabRefException("Push aborted: Merge left conflicts unresolved.");
                }
                if (status.uncommittedChanges()) {
                    throw new JabRefException("Push aborted: Merge produced uncommitted changes.");
                }

                SyncStatus after = status.syncStatus();
                if (after == SyncStatus.AHEAD) {
                    gitHandler.pushCommitsToRemoteRepository();
                    return PushResult.pushed();
                } else if (after == SyncStatus.UP_TO_DATE) {
                    return PushResult.noopUpToDate();
                } else {
                    throw new JabRefException("Push aborted: Repository not ahead after merge. Status: " + after);
                }
            }
            default -> {
                throw new JabRefException("Push aborted: Unsupported sync status.");
            }
        }
    }
}

