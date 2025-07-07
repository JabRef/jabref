package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.JabRefException;
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
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

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
    private final GitHandler gitHandler;

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

        // TODO: Validate that the .bib file is inside the Git repository earlier in the workflow.
        // This check might be better placed before calling performSemanticMerge.
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

        // 3. If there are conflicts, prompt user to resolve them via GUI
        BibDatabaseContext effectiveRemote = remote;
        if (!conflicts.isEmpty()) {
            List<BibEntry> resolvedRemoteEntries = new ArrayList<>();

//            for (ThreeWayEntryConflict conflict : conflicts) {
//                // Uses a GUI dialog to let the user merge entries interactively
//                BibEntry resolvedEntry = this.conflictResolver.resolveConflict(conflict, prefs, dialogService);
//                resolvedRemoteEntries.add(resolvedEntry);
//            }
//            // Replace conflicted entries in remote with user-resolved ones
//            effectiveRemote = GitMergeUtil.replaceEntries(remote, resolvedRemoteEntries);
        }

        //  4. Apply resolved remote (either original or conflict-resolved) to local
        MergePlan plan = SemanticConflictDetector.extractMergePlan(base, effectiveRemote);
        SemanticMerger.applyMergePlan(local, plan);

        // 5. Write back merged result
        GitFileWriter.write(bibFilePath, local, importFormatPreferences);

        return MergeResult.success();
    }

    // WIP
    // TODO: add test
    public void push(Path bibFilePath) throws GitAPIException, IOException {
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

