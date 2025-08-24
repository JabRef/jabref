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
import org.jabref.logic.git.model.MergeResult;
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

    public MergeResult fetchAndMerge(BibDatabaseContext localDatabaseContext, Path bibFilePath) throws GitAPIException, IOException, JabRefException {
        Optional<Path> repoRoot = GitHandler.findRepositoryRoot(bibFilePath);
        if (repoRoot.isEmpty()) {
            LOGGER.warn("Path is not inside a Git repository.");
            return MergeResult.failure();
        }
        GitHandler gitHandler = gitHandlerRegistry.get(repoRoot.get());

        GitStatusSnapshot status = GitStatusChecker.checkStatus(gitHandler);

        if (!status.tracking()) {
            LOGGER.warn("Pull aborted: The file is not under Git version control.");
            return MergeResult.failure();
        }

        if (status.conflict()) {
            LOGGER.warn("Pull aborted: Local repository has unresolved merge conflicts.");
            return MergeResult.failure();
        }

        if (status.uncommittedChanges()) {
            LOGGER.warn("Pull aborted: Local changes have not been committed.");
            return MergeResult.failure();
        }

        if (status.syncStatus() == SyncStatus.UP_TO_DATE || status.syncStatus() == SyncStatus.AHEAD) {
            LOGGER.info("Pull skipped: Local branch is already up to date with remote.");
            return MergeResult.success();
        }

        try (Git git = gitHandler.open()) {
            // 1. Fetch latest remote branch
            gitHandler.fetchOnCurrentBranch();

            // 2. Locate base / local / remote commits
            GitRevisionLocator locator = new GitRevisionLocator();
            RevisionTriple triple = locator.locateMergeCommits(git);

            // 3. Perform semantic merge
            MergeResult result = performSemanticMerge(git, triple.base(), triple.remote(), localDatabaseContext, bibFilePath);

            // 4. Auto-commit merge result if successful
            // TODO: Allow user customization of auto-merge commit message (e.g. conventional commits)
            if (result.isSuccessful()) {
                gitHandler.createCommitOnCurrentBranch("Auto-merged by JabRef", !AMEND);
            }

            return result;
        }
    }

    public MergeResult performSemanticMerge(Git git,
                                            Optional<RevCommit> baseCommitOpt,
                                            RevCommit remoteCommit,
                                            BibDatabaseContext localDatabaseContext,
                                            Path bibFilePath) throws IOException, JabRefException {

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

        BibDatabaseContext effectiveRemote;
        if (conflicts.isEmpty()) {
            effectiveRemote = remote;
        } else {
            // 3. If there are conflicts, ask strategy to resolve
            List<BibEntry> resolved = gitConflictResolverStrategy.resolveConflicts(conflicts);
            if (resolved.isEmpty()) {
                LOGGER.warn("Merge aborted: Conflict resolution was canceled or denied.");
                return MergeResult.failure();
            }
            effectiveRemote = GitMergeUtil.replaceEntries(remote, resolved);
        }

        // 4. Apply resolved remote (either original or conflict-resolved) to local
        MergeResult result = mergeExecutor.merge(base, local, effectiveRemote, bibFilePath);

        return result;
    }

    public void push(BibDatabaseContext localDatabaseContext, Path bibFilePath) throws GitAPIException, IOException, JabRefException {
        Optional<Path> repoRoot = GitHandler.findRepositoryRoot(bibFilePath);

        if (repoRoot.isEmpty()) {
            LOGGER.warn("Path is not inside a Git repository");
            return;
        }
        GitHandler gitHandler = gitHandlerRegistry.get(repoRoot.get());

        GitStatusSnapshot status = GitStatusChecker.checkStatus(gitHandler);

        if (!status.tracking()) {
            LOGGER.warn("Push aborted: File is not tracked by Git");
            return;
        }

        if (status.uncommittedChanges()) {
            LOGGER.warn("Pull aborted: Local changes have not been committed.");
            return;
        }

        switch (status.syncStatus()) {
            case UP_TO_DATE -> {
                boolean committed = gitHandler.createCommitOnCurrentBranch("Changes committed by JabRef", !AMEND);
                if (committed) {
                    gitHandler.pushCommitsToRemoteRepository();
                } else {
                    LOGGER.info("No changes to commit — skipping push");
                }
            }

            case AHEAD -> {
                gitHandler.pushCommitsToRemoteRepository();
            }

            case BEHIND -> {
                LOGGER.warn("Push aborted: Local branch is behind remote. Please pull first.");
            }

            case DIVERGED -> {
                try (Git git = gitHandler.open()) {
                    GitRevisionLocator locator = new GitRevisionLocator();
                    RevisionTriple triple = locator.locateMergeCommits(git);

                    MergeResult mergeResult = performSemanticMerge(git, triple.base(), triple.remote(), localDatabaseContext, bibFilePath);

                    if (!mergeResult.isSuccessful()) {
                        LOGGER.warn("Semantic merge failed — aborting push");
                        return;
                    }

                    boolean committed = gitHandler.createCommitOnCurrentBranch("Merged changes", !AMEND);

                    if (committed) {
                        gitHandler.pushCommitsToRemoteRepository();
                    } else {
                        LOGGER.info("Nothing to commit after semantic merge — skipping push");
                    }
                }
            }

            case CONFLICT -> {
                LOGGER.warn("Push aborted: Local repository has unresolved merge conflicts.");
            }

            case UNTRACKED, UNKNOWN -> {
                LOGGER.warn("Push aborted: Untracked or unknown Git status.");
            }
        }
    }

    public boolean commitLocalChanges(Path bibFilePath, String commitMessage, boolean amend) throws IOException, GitAPIException, JabRefException {
        Optional<Path> repoRootOpt = GitHandler.findRepositoryRoot(bibFilePath);
        if (repoRootOpt.isEmpty()) {
            throw new JabRefException("Commit aborted: Path is not inside a Git repository.");
        }
        GitHandler gitHandler = gitHandlerRegistry.get(repoRootOpt.get());

        GitStatusSnapshot status = GitStatusChecker.checkStatus(gitHandler);
        if (!status.tracking()) {
            throw new JabRefException("Commit aborted: The file is not under Git version control.");
        }
        if (status.conflict()) {
            throw new JabRefException("Commit aborted: Local repository has unresolved merge conflicts.");
        }

        String message = commitMessage != null && !commitMessage.isBlank()
                ? commitMessage
                : "Commit changes";

        return gitHandler.createCommitOnCurrentBranch(message, amend);
    }
}

