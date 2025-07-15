package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitBibParser;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.git.io.GitRevisionLocator;
import org.jabref.logic.git.io.RevisionTriple;
import org.jabref.logic.git.merge.MergePlan;
import org.jabref.logic.git.merge.SemanticMerger;
import org.jabref.logic.git.model.MergeResult;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.status.SyncStatus;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GitSyncService currently serves as an orchestrator for Git pull/push logic.
 * if (hasConflict)
 *     → UI merge;
 * else
 *     → autoMerge := local + remoteDiff
 */
public class GitSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);

    private static final boolean AMEND = true;
    private final ImportFormatPreferences importFormatPreferences;
    private final GitHandler gitHandler;
    private final GitConflictResolverStrategy gitConflictResolverStrategy;

    public GitSyncService(ImportFormatPreferences importFormatPreferences, GitHandler gitHandler, GitConflictResolverStrategy gitConflictResolverStrategy) {
        this.importFormatPreferences = importFormatPreferences;
        this.gitHandler = gitHandler;
        this.gitConflictResolverStrategy = gitConflictResolverStrategy;
    }

    public MergeResult fetchAndMerge(Path bibFilePath) throws GitAPIException, IOException, JabRefException {
        Optional<GitHandler> maybeHandler = GitHandler.fromAnyPath(bibFilePath);
        if (maybeHandler.isEmpty()) {
            LOGGER.warn("Pull aborted: The file is not inside a Git repository.");
            return MergeResult.failure();
        }

        GitStatusSnapshot status = GitStatusChecker.checkStatus(bibFilePath);

        if (!status.tracking()) {
            LOGGER.warn("Pull aborted: The file is not under Git version control.");
            return MergeResult.failure();
        }

        if (status.conflict()) {
            LOGGER.warn("Pull aborted: Local repository has unresolved merge conflicts.");
            return MergeResult.failure();
        }

        if (status.syncStatus() == SyncStatus.UP_TO_DATE || status.syncStatus() == SyncStatus.AHEAD) {
            LOGGER.info("Pull skipped: Local branch is already up to date with remote.");
            return MergeResult.success();
        }

        // Status is BEHIND or DIVERGED
        try (Git git = gitHandler.open()) {
            // 1. Fetch latest remote branch
            gitHandler.fetchOnCurrentBranch();

            // 2. Locate base / local / remote commits
            GitRevisionLocator locator = new GitRevisionLocator();
            RevisionTriple triple = locator.locateMergeCommits(git);

            // 3. Perform semantic merge
            MergeResult result = performSemanticMerge(git, triple.base(), triple.local(), triple.remote(), bibFilePath);

            // 4. Auto-commit merge result if successful
            if (result.isSuccessful()) {
                gitHandler.createCommitOnCurrentBranch("Auto-merged by JabRef", !AMEND);
            }

            return result;
        }
    }

    public MergeResult performSemanticMerge(Git git,
                                            RevCommit baseCommit,
                                            RevCommit localCommit,
                                            RevCommit remoteCommit,
                                            Path bibFilePath) throws IOException, JabRefException {

        Path bibPath = bibFilePath.toRealPath();
        Path workTree = git.getRepository().getWorkTree().toPath().toRealPath();
        Path relativePath;

        if (!bibPath.startsWith(workTree)) {
            throw new IllegalStateException("Given .bib file is not inside repository");
        }
        relativePath = workTree.relativize(bibPath);

        // 1. Load three versions
        String baseContent = GitFileReader.readFileFromCommit(git, baseCommit, relativePath);
        String localContent = GitFileReader.readFileFromCommit(git, localCommit, relativePath);
        String remoteContent = GitFileReader.readFileFromCommit(git, remoteCommit, relativePath);

        BibDatabaseContext base = GitBibParser.parseBibFromGit(baseContent, importFormatPreferences);
        BibDatabaseContext local = GitBibParser.parseBibFromGit(localContent, importFormatPreferences);
        BibDatabaseContext remote = GitBibParser.parseBibFromGit(remoteContent, importFormatPreferences);

        // 2. Conflict detection
        List<ThreeWayEntryConflict> conflicts = SemanticConflictDetector.detectConflicts(base, local, remote);

        BibDatabaseContext effectiveRemote;
        if (conflicts.isEmpty()) {
            effectiveRemote = remote;
        } else {
            // 3. If there are conflicts, ask strategy to resolve
            Optional<BibDatabaseContext> maybeRemote = gitConflictResolverStrategy.resolveConflicts(conflicts, remote);
            if (maybeRemote.isEmpty()) {
                LOGGER.warn("Merge aborted: Conflict resolution was canceled or denied.");
                return MergeResult.failure();
            }
            effectiveRemote = maybeRemote.get();
        }

        //  4. Apply resolved remote (either original or conflict-resolved) to local
        MergePlan plan = SemanticConflictDetector.extractMergePlan(base, effectiveRemote);
        SemanticMerger.applyMergePlan(local, plan);

        // 5. Write back merged result
        GitFileWriter.write(bibFilePath, local, importFormatPreferences);

        return MergeResult.success();
    }

    public void push(Path bibFilePath) throws GitAPIException, IOException, JabRefException {
        GitStatusSnapshot status = GitStatusChecker.checkStatus(bibFilePath);

        if (!status.tracking()) {
            LOGGER.warn("Push aborted: file is not tracked by Git");
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

                    MergeResult mergeResult = performSemanticMerge(git, triple.base(), triple.local(), triple.remote(), bibFilePath);

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
}

