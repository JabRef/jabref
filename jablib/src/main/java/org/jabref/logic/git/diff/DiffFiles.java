package org.jabref.logic.git.diff;

public sealed interface DiffFiles permits LineDiffFiles, EntryDiffFiles {
    String fileName();
}
