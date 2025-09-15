package org.jabref.logic.git.merge;

import java.io.IOException;

import org.jabref.logic.git.model.MergePlan;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface GitSemanticMergePlanner {

    /**
     * Applies semantic computeMergePlan of remote into local, based on base version.
     * Assumes conflicts have already been resolved (if any).
     *
     * @param base The common ancestor version
     * @param local The current local version (to be updated)
     * @param remote The incoming remote version (can be resolved or raw)
     * @return MergePlan object containing computeMergePlan plan
     */
    MergePlan computeMergePlan(@Nullable BibDatabaseContext base,
                               BibDatabaseContext local,
                               BibDatabaseContext remote) throws IOException;
}
