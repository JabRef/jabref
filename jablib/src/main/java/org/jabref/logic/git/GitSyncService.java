package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.io.GitRevisionLocator;
import org.jabref.logic.git.io.RevisionTriple;
import org.jabref.logic.git.merge.execution.MergeBookkeeper;
import org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer;
import org.jabref.logic.git.model.BookkeepingResult;
import org.jabref.logic.git.model.MergeAnalysis;
import org.jabref.logic.git.model.MergePlan;
import org.jabref.logic.git.model.PullPlan;
import org.jabref.logic.git.model.PushResult;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.status.SyncStatus;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

/// GitSyncService currently serves as an orchestrator for Git pull/push logic (Not responsible for writing.).
///
/// if (hasConflict)
///     → UI merge;
/// else
///     → autoMerge := local + remoteDiff
public class GitSyncService {
    private final ImportFormatPreferences importFormatPreferences;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final MergeBookkeeper bookkeeper;

    public GitSyncService(ImportFormatPreferences importFormatPreferences, GitHandlerRegistry gitHandlerRegistry, MergeBookkeeper bookkeeper) {
        this.importFormatPreferences = importFormatPreferences;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.bookkeeper = bookkeeper;
    }

    public static GitSyncService create(ImportFormatPreferences importFormatPreferences, GitHandlerRegistry registry) {
        return new GitSyncService(
                importFormatPreferences,
                registry,
                new MergeBookkeeper(registry)
        );
    }

    ///  compute merge inputs/outputs for GUI
    public Optional<PullPlan> prepareMerge(BibDatabaseContext localDatabaseContext, Path bibFilePath) throws GitAPIException, IOException, JabRefException {
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
            return Optional.empty();
        }
        if (status.syncStatus() == SyncStatus.AHEAD) {
            return Optional.empty();
        }

        try (Git git = gitHandler.open()) {
            // 1. locate base / local / remote commits
            GitRevisionLocator locator = new GitRevisionLocator();
            RevisionTriple triple = locator.locateMergeCommits(git);
            Optional<RevCommit> baseCommitOpt = triple.base();
            RevCommit remoteCommit = triple.remote();
            RevCommit localHead = triple.local();

            // 2. load 3 versions (base/remote from Git; local from active DB)
            Path bibPath = bibFilePath.toRealPath();
            Path workTree = git.getRepository().getWorkTree().toPath().toRealPath();
            Path relativePath;

            if (!bibPath.startsWith(workTree)) {
                throw new IllegalStateException("Given .bib file is not inside repository");
            }
            relativePath = workTree.relativize(bibPath);

            BibDatabaseContext base;
            if (baseCommitOpt.isPresent()) {
                Optional<String> baseContent = GitFileReader.readFileFromCommit(git, baseCommitOpt.get(), relativePath);
                base = baseContent.isEmpty() ? BibDatabaseContext.empty() : BibDatabaseContext.of(baseContent.get(), importFormatPreferences);
            } else {
                base = BibDatabaseContext.empty();
            }

            Optional<String> remoteContent = GitFileReader.readFileFromCommit(git, remoteCommit, relativePath);
            BibDatabaseContext remote = remoteContent.isEmpty() ? BibDatabaseContext.empty() : BibDatabaseContext.of(remoteContent.get(), importFormatPreferences);
            BibDatabaseContext local = localDatabaseContext;

            // 2. compute conflicts & auto plan
            MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(base, local, remote);
            List<ThreeWayEntryConflict> conflicts = analysis.conflicts();
            MergePlan autoPlan = analysis.autoPlan();

            // 5) return computation (GUI will apply & save, then finalize)
            return Optional.of(PullPlan.of(status.syncStatus(), baseCommitOpt, localHead, remoteCommit, autoPlan, conflicts));
        }
    }

    /// Phase-2: finalize after GUI saved the file with applied plan.
    ///
    /// Responsibilities:
    /// - (Re)open repo, stage bib file
    /// - For BEHIND: fast-forward or create commit consistent with GUI-saved tree
    /// - For DIVERGED: merge commit with parents (localHead, remote)
    ///
    /// Preconditions:
    /// - The bib file on disk already reflects: local + autoPlan (+ resolvedPlan)
    /// - No uncommitted unrelated changes
    public BookkeepingResult finalizeMerge(Path bibFilePath,
                                           PullPlan computation) throws GitAPIException, IOException, JabRefException {
        return bookkeeper.resultRecord(bibFilePath, computation);
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
                throw new JabRefException("Push aborted: Local branch is behind or has diverged from remote. Please pull first.");
            }
            default -> {
                throw new JabRefException("Push aborted: Unsupported sync status.");
            }
        }
    }
}

