package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitBibParser;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.git.io.GitRevisionLocator;
import org.jabref.logic.git.io.RevisionTriple;
import org.jabref.logic.git.merge.GitMergeUtil;
import org.jabref.logic.git.merge.MergePlan;
import org.jabref.logic.git.merge.SemanticMerger;
import org.jabref.logic.git.model.MergeResult;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitPullViewModel extends AbstractViewModel {
    private final ImportFormatPreferences importFormatPreferences;
    private final GitConflictResolver conflictResolver;
    private final DialogService dialogService;

    public GitPullViewModel(ImportFormatPreferences importFormatPreferences,
                             GitConflictResolver conflictResolver,
                             DialogService dialogService) {
        this.importFormatPreferences = importFormatPreferences;
        this.conflictResolver = conflictResolver;
        this.dialogService = dialogService;
    }

    public MergeResult pull(Path bibFilePath) throws IOException, GitAPIException, JabRefException {
        // Open the Git repository from the parent folder of the .bib file
        Git git = Git.open(bibFilePath.getParent().toFile());

        // Fetch latest changes from remote
        // TODO: Temporary — GitHandler should be injected from GitStatusViewModel once centralized git status is implemented.
        GitHandler gitHandler = GitHandler.fromAnyPath(bibFilePath)
                                          .orElseThrow(() -> new IllegalStateException("Not inside a Git repository"));

        gitHandler.fetchOnCurrentBranch();

        // Determine the three-way merge base, local, and remote commits
        GitRevisionLocator locator = new GitRevisionLocator();
        RevisionTriple triple = locator.locateMergeCommits(git);

        RevCommit baseCommit = triple.base();
        RevCommit localCommit = triple.local();
        RevCommit remoteCommit = triple.remote();

        // Ensure file is inside the Git working tree
        Path bibPath = bibFilePath.toRealPath();
        Path workTree = git.getRepository().getWorkTree().toPath().toRealPath();
        if (!bibPath.startsWith(workTree)) {
            throw new IllegalStateException("Given .bib file is not inside repository");
        }
        Path relativePath = workTree.relativize(bibPath);

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
            for (ThreeWayEntryConflict conflict : conflicts) {
                // Ask user to resolve this conflict via GUI dialog
                Optional<BibEntry> maybeResolved = conflictResolver.resolveConflict(conflict);
                if (maybeResolved.isPresent()) {
                    resolvedRemoteEntries.add(maybeResolved.get());
                } else {
                    // User canceled the merge dialog → abort the whole merge
                    throw new JabRefException("Merge aborted: Not all conflicts were resolved by user.");
                }
            }
            // Replace original conflicting entries in remote with resolved versions
            effectiveRemote = GitMergeUtil.replaceEntries(remote, resolvedRemoteEntries);
        }

        // Extract merge plan and apply it to the local database
        MergePlan plan = SemanticConflictDetector.extractMergePlan(base, effectiveRemote);
        SemanticMerger.applyMergePlan(local, plan);

        // Save merged result to .bib file
        GitFileWriter.write(bibFilePath, local, importFormatPreferences);

        // Create Git commit for the merged result
        gitHandler.createCommitOnCurrentBranch("Auto-merged by JabRef", true);
        return MergeResult.success();
    }
}
