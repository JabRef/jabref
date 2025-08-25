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

    @Override
    public MergePlan merge(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote, Path bibFilePath) throws IOException {
        // 1. extract merge plan from base -> remote
        MergePlan plan = SemanticConflictDetector.extractMergePlan(base, local, remote);

        BibDatabaseContext working = new BibDatabaseContext(new BibDatabase(), new MetaData());
        for (BibEntry entry : local.getDatabase().getEntries()) {
            working.getDatabase().insertEntry(new BibEntry(entry));
        }

        // 2. apply remote changes to local
        SemanticMerger.applyMergePlan(working, plan);

        // 3. write back merged content
        GitFileWriter.write(bibFilePath, working, importFormatPreferences);

        return plan;
    }
}
