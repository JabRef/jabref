package org.jabref.logic.cleanup;

import java.util.List;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

public class EmptyEntryCleanup implements CleanupJob {
    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        return null;
    }
}
