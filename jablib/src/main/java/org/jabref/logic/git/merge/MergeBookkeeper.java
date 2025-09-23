package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.model.FinalizeResult;
import org.jabref.logic.git.model.PullPlan;

import org.eclipse.jgit.api.errors.GitAPIException;

public interface MergeBookkeeper {
    /**
     * Record the GUI-produced merge result into Git history.
     * Creates the right commit shape based on the merge graph:
     *  - BEHIND: fast-forward if content equals remote;
     *            otherwise create a new commit on top of `remote`.
     *  - DIVERGED: create a merge commit with parents [localHead, remote].
     *
     * Preconditions:
     *  - GUI has already saved the final .bib file to disk.
     *  - No unrelated unstaged changes (defensive check recommended).
     */
    FinalizeResult resultRecord(Path bibFilePath, PullPlan computation) throws IOException, GitAPIException, JabRefException;
}
