package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.git.util.GitBibParser;
import org.jabref.logic.git.util.GitFileReader;
import org.jabref.logic.git.util.GitFileWriter;
import org.jabref.logic.git.util.GitRevisionLocator;
import org.jabref.logic.git.util.MergePlan;
import org.jabref.logic.git.util.MergeResult;
import org.jabref.logic.git.util.RevisionTriple;
import org.jabref.logic.git.util.SemanticConflictDetector;
import org.jabref.logic.git.util.SemanticMerger;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrator for git sync service
 * if (hasConflict)
 *     → UI merge;
 * else
 *     → autoMerge := local + remoteDiff
 */
public class GitSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);

    private static final boolean AMEND = true;
    private final ImportFormatPreferences importFormatPreferences;
    private GitHandler gitHandler;

    public GitSyncService(ImportFormatPreferences importFormatPreferences, GitHandler gitHandler) {
        this.importFormatPreferences = importFormatPreferences;
        this.gitHandler = gitHandler;
    }

    /**
     * Called when user clicks Pull
     */
    public MergeResult fetchAndMerge(Path bibFilePath) throws GitAPIException, IOException, JabRefException {
        Git git = Git.open(bibFilePath.getParent().toFile());

        // 1. fetch latest remote branch
        gitHandler.fetchOnCurrentBranch();

        // 2. Locating the base / local / remote versions
        GitRevisionLocator locator = new GitRevisionLocator();
        RevisionTriple triple = locator.locateMergeCommits(git);

        // 3. Calling semantic merge logic
        MergeResult result = performSemanticMerge(git, triple.base(), triple.local(), triple.remote(), bibFilePath);

        // 4. Automatic merge
        if (result.isSuccessful()) {
            gitHandler.createCommitOnCurrentBranch("Auto-merged by JabRef", !AMEND);
        }

        return result;
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
        List<BibEntryDiff> conflicts = SemanticConflictDetector.detectConflicts(base, local, remote);

        if (!conflicts.isEmpty()) {
            // Currently only handles non-conflicting cases. In the future, it may:
            // - Store the current state along with 3 versions
            // - Return conflicts along with base/local/remote versions for each entry
            // - Invoke a UI merger (let the UI handle merging and return the result)
            return MergeResult.withConflicts(conflicts); // TODO: revisit the naming
        }

        // If the user returns a manually merged result, it should use: i.e.: MergeResult performSemanticMerge(..., BibDatabaseContext userResolvedResult)

        // 3. Apply remote patch to local
        MergePlan plan = SemanticConflictDetector.extractMergePlan(base, remote);
        SemanticMerger.applyMergePlan(local, plan);

        // 4. Write back merged result
        GitFileWriter.write(bibFilePath, local, importFormatPreferences);

        return MergeResult.success();
    }

    // WIP
    public void push(Path bibFilePath) throws GitAPIException, IOException {
        this.gitHandler = new GitHandler(bibFilePath.getParent());

        // 1. Auto-commit: commit if there are changes
        boolean committed = gitHandler.createCommitOnCurrentBranch("Changes committed by JabRef", !AMEND);

        // 2. push to remote
        if (committed) {
            gitHandler.pushCommitsToRemoteRepository();
        } else {
            LOGGER.info("No changes to commit — skipping push");
        }
    }
}

