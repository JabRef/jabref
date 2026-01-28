package org.jabref.logic.cleanup;

import java.util.List;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface CleanupJob {
    /// Cleans up the given entry and returns the list of changes made.
    List<FieldChange> cleanup(BibEntry entry);
}
