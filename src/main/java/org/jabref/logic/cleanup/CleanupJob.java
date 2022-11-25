package org.jabref.logic.cleanup;

import java.util.List;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface CleanupJob {

    /**
     * Cleanup the entry.
     */
    List<FieldChange> cleanup(BibEntry entry);
}
