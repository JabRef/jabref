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
import org.jabref.logic.git.merge.GitSemanticMergePlanner;
import org.jabref.logic.git.merge.MergeAnalysis;
import org.jabref.logic.git.merge.MergeBookkeeper;
import org.jabref.logic.git.merge.SemanticMergeAnalyzer;
import org.jabref.logic.git.model.FinalizeResult;
import org.jabref.logic.git.model.MergePlan;
import org.jabref.logic.git.model.PullComputation;
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

/// GitSyncService currently serves as an orchestrator for Git pull/push logic (不负责写入).
///
/// if (hasConflict)
///     → UI computeMergePlan;
/// else
///     → autoMerge := local + remoteDiff
///
/// - prepareMerge(): Perform only fetch/status/locate commits/read three databases/detect conflicts/generate automatic patches (MergePlan) and statistics; do not write to disk.
/// - finalizeMerge(): After the GUI has been successfully committed to disk, it is responsible for stage and commit.
public class GitSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);

    private static final boolean AMEND = true;
    private final ImportFormatPreferences importFormatPreferences;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final GitConflictResolverStrategy gitConflictResolverStrategy;
    private final GitSemanticMergePlanner mergePlanner;
    private final MergeBookkeeper bookkeeper;

    public GitSyncService(ImportFormatPreferences importFormatPreferences, GitHandlerRegistry gitHandlerRegistry, GitConflictResolverStrategy gitConflictResolverStrategy, GitSemanticMergePlanner mergeExecutor, MergeBookkeeper bookkeeper) {
        this.importFormatPreferences = importFormatPreferences;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.gitConflictResolverStrategy = gitConflictResolverStrategy;
        this.mergePlanner = mergeExecutor;
        this.bookkeeper = bookkeeper;
    }

    ///  compute computeMergePlan inputs/outputs for GUI
    public PullComputation prepareMerge(BibDatabaseContext localDatabaseContext, Path bibFilePath) throws GitAPIException, IOException, JabRefException {
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
            throw new JabRefException("Pull aborted: Local repository has unresolved computeMergePlan conflicts.");
        }

        // Prevent rollback from overwriting the user's uncommitted changes
        if (status.uncommittedChanges()) {
            throw new JabRefException("Pull aborted: Local changes have not been committed.");
        }

        if (status.syncStatus() == SyncStatus.UP_TO_DATE) {
            return PullComputation.noop();
        }
        if (status.syncStatus() == SyncStatus.AHEAD) {
            return PullComputation.noopAhead();
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
                base = new BibDatabaseContext();
            }

            Optional<String> remoteContent = GitFileReader.readFileFromCommit(git, remoteCommit, relativePath);
            BibDatabaseContext remote = remoteContent.isEmpty() ? BibDatabaseContext.empty() : BibDatabaseContext.of(remoteContent.get(), importFormatPreferences);
            BibDatabaseContext local = localDatabaseContext;

            // 2. compute conflicts & auto plan
            MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(base, local, remote);
            List<ThreeWayEntryConflict> conflicts = analysis.conflicts();
            MergePlan autoPlan = analysis.autoPlan();

            // 5) return computation (GUI will apply & save, then finalize)
            return PullComputation.of(status.syncStatus(), baseCommitOpt, remoteCommit, localHead, autoPlan, conflicts);
        }
    }

    /**
     * Phase-2: finalize after GUI saved the file with applied plan.
     *
     * Responsibilities:
     * - (Re)open repo, stage bib file
     * - For BEHIND: fast-forward or create commit consistent with GUI-saved tree
     * - For DIVERGED: create computeMergePlan commit with parents (localHead, remote)
     *
     * Preconditions:
     * - The bib file on disk already reflects: local + autoPlan (+ resolvedPlan)
     * - No uncommitted unrelated changes
     */
    public FinalizeResult finalizeMerge(Path bibFilePath,
                                        PullComputation computation) throws GitAPIException, IOException, JabRefException {
        return bookkeeper.resultRecord(bibFilePath, computation);
    }

    @Deprecated
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
            throw new JabRefException("Pull aborted: Local repository has unresolved computeMergePlan conflicts.");
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

            // 2. Perform semantic computeMergePlan
            if (status.syncStatus() == SyncStatus.DIVERGED) {
//                try (GitHandler.MergeGuard guard = gitHandler.beginSemanticMergeGuard(remoteCommit, bibFilePath)) {
//                    PullResult result = performSemanticMerge(git, triple.base(), remoteCommit, localDatabaseContext, bibFilePath);
//
//                    if (result.isSuccessful()) {
//                        guard.commit("Auto-merged by JabRef");
//                    }
//                    return result;
//                }
            }

            throw new JabRefException("Pull aborted: Unsupported sync status " + status.syncStatus());
        }
    }

    @Deprecated
    /// @param baseCommitOpt Optional base commit (empty if TODO)
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
        // At this stage, there are "patches" which are clear, and patches which are "unclear", because there are conflicting changes
        // if the set of "unclear" patches (i.e., conflicting changes), resolve them, which makes them to "clear" patches
        // Rough idea: Have a computeMergePlan plan containing "resolvedMergedPlan" and "conflictingChangs" - and turn the conflictingChagnes to resolvedMergePlan - "ConflicitingMergePlan" - and the (existing) MergePlan
        // TODO: 0908
        //  1. GitSemanticMergeExecutorImpl 只做 extractMergePlan，逻辑层停止写盘  GitSemanticMergeExecutor -> SemanticMergePlanner
        //  2. 冲突与自动补丁分离,确定MergePlan数据结构
        //  3. 改成GUI落盘
        // TODO: Git 状态机

        BibDatabaseContext localPrime;
        if (conflicts.isEmpty()) {
            localPrime = local;
//            // No conflict: let logic write merged result to disk
//            mergePlanner.computeMergePlan(base, localPrime, remote, bibFilePath);
        } else {
            // 3. If there are conflicts, ask strategy to resolve
            List<BibEntry> resolved = gitConflictResolverStrategy.resolveConflicts(conflicts);
            if (resolved.isEmpty()) {
                throw new JabRefException("Merge aborted: Conflict resolution was canceled or denied.");
            }
            // TODO: Replace this by dertermining a computeMergePlan plan
            localPrime = GitMergeUtil.replaceEntries(local, resolved);
        }

        // At this place, the computeMergePlan paln is clear - and does not have any conflicts (because if there are unresolved conflicts, whe throw a JabRefException)
        // We need to do a computeMergePlan commit
        // TODO: Create computeMergePlan commit - contents: current disk contents - parent: baseCommitOpt and remoteCommit

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
                throw new JabRefException("Push aborted: Local repository has unresolved computeMergePlan conflicts.");
            }

            case UNTRACKED -> {
                throw new JabRefException("Push aborted: Untracked file.");
            }

            case UNKNOWN -> {
                throw new JabRefException("Push aborted: Unknown Git status.");
            }

            case BEHIND, DIVERGED -> {
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
                    throw new JabRefException("Push aborted: Repository not ahead after computeMergePlan. Status: " + after);
                }
            }
            default -> {
                throw new JabRefException("Push aborted: Unsupported sync status.");
            }
        }
    }
}

