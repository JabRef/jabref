package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface GitSemanticMergeExecutor {

    /**
     * Applies semantic merge of remote into local, based on base version.
     * Assumes conflicts have already been resolved (if any).
     *
     * @param base        The common ancestor version
     * @param local       The current local version (to be updated)
     * @param remote      The incoming remote version (can be resolved or raw)
     * @param bibFilePath The path to the target bib file (used for write-back)
     * @return MergePlan object containing merge plan
     */
    MergePlan merge(@Nullable BibDatabaseContext base,
                    BibDatabaseContext local,
                    BibDatabaseContext remote,
                    Path bibFilePath) throws IOException;
}
