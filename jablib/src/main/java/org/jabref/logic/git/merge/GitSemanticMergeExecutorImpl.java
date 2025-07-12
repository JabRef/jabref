package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.git.model.MergeResult;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

public class GitSemanticMergeExecutorImpl implements GitSemanticMergeExecutor {

    private final ImportFormatPreferences importFormatPreferences;

    public GitSemanticMergeExecutorImpl(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public MergeResult merge(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote, Path bibFilePath) throws IOException, IOException {
        // 1. extract merge plan from base -> remote
        MergePlan plan = SemanticConflictDetector.extractMergePlan(base, remote);

        // 2. apply remote changes to local
        SemanticMerger.applyMergePlan(local, plan);

        // 3. write back merged content
        GitFileWriter.write(bibFilePath, local, importFormatPreferences);

        return MergeResult.success();
    }
}
