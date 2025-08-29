package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;

public class GitSemanticMergeExecutorImpl implements GitSemanticMergeExecutor {

    private final ImportFormatPreferences importFormatPreferences;

    public GitSemanticMergeExecutorImpl(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    /// @param local This is the BibDatabaseContext shown in the library tab
    @Override
    public MergePlan merge(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote, Path bibFilePath) throws IOException {
        // 1. extract merge plan from base -> remote
        MergePlan plan = SemanticConflictDetector.extractMergePlan(base, local, remote);

        // 2. apply remote changes to local
        // TODO: Remove all of this - and include MergePlan in the PullResult and let caller do the "dirty" work of updating the UI and the local file
        //   HOWEVER, the git data structure needs to be fine - "we" need to create a merge commit in the logic.git part somewhere - this has to be in line (or part of) with the logic of org.jabref.logic.git.GitSyncService.fetchAndMerge
        BibDatabaseContext working = new BibDatabaseContext(new BibDatabase(), new MetaData());
        for (BibEntry entry : local.getDatabase().getEntries()) {
            working.getDatabase().insertEntry(new BibEntry(entry));
        }
        // TODO: The merge plan should be applied to "local" directly
        SemanticMerger.applyMergePlan(working, plan);

        // 3. write back merged content
        // Idea: Maybe re-use {@link org.jabref.gui.exporter.SaveDatabaseAction}, because this also has UI notifications - HOWEVER, that is UI code, and we are in the logic layer
        // Someone needs to be responsible for writing - to keep "git view" and "JabRef view" in sync
        //  "git view": If someone opens "git gui" or executes "git status", there should be now changes shown. Therefore, the file on disk should be the same
        GitFileWriter.write(bibFilePath, working, importFormatPreferences);

        return plan;
    }
}
