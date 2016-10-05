package net.sf.jabref.model.cleanup;

import java.util.List;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface CleanupJob {

    /**
     * Cleanup the entry.
     */
    List<FieldChange> cleanup(BibEntry entry);

}
