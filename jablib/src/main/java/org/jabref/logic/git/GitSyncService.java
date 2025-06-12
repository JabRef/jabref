package org.jabref.logic.git;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.git.util.GitBibParser;
import org.jabref.logic.git.util.GitFileReader;
import org.jabref.logic.git.util.SemanticConflictDetector;
import org.jabref.logic.git.util.SemanticMerger;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitSyncService {
    private final ImportFormatPreferences importFormatPreferences;

    public GitSyncService(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public MergeResult performSemanticMerge(Git git,
                                            RevCommit baseCommit,
                                            RevCommit localCommit,
                                            RevCommit remoteCommit,
                                            Path bibFilePath,
                                            ImportFormatPreferences importFormatPreferences) throws Exception {

        // 1. Load three versions
        String baseContent = GitFileReader.readFileFromCommit(git, baseCommit, bibFilePath);
        String localContent = GitFileReader.readFileFromCommit(git, localCommit, bibFilePath);
        String remoteContent = GitFileReader.readFileFromCommit(git, remoteCommit, bibFilePath);

        BibDatabaseContext base = GitBibParser.parseBibFromGit(baseContent, importFormatPreferences);
        BibDatabaseContext local = GitBibParser.parseBibFromGit(localContent, importFormatPreferences);
        BibDatabaseContext remote = GitBibParser.parseBibFromGit(remoteContent, importFormatPreferences);

        // 2. Conflict detection
        List<BibEntryDiff> conflicts = SemanticConflictDetector.detectConflicts(base, local, remote);

        if (!conflicts.isEmpty()) {
            return MergeResult.conflictsFound(conflicts); // UI-resolvable
        }

        // 3. Apply remote patch to local
        SemanticMerger.applyRemotePatchToDatabase(base, local, remote);

        // 4. Write back merged result
//        try {
//            GitFileWriter.write(bibFilePath, local, importFormatPreferences);
//        } catch (Exception e) {
//            return MergeResult.failure("Failed to write merged file: " + e.getMessage());
//        }

        return MergeResult.success();
    }
}

