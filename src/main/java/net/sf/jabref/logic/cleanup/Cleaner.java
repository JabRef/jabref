package net.sf.jabref.logic.cleanup;

import java.util.List;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.model.entry.BibtexEntry;

public interface Cleaner {

    /**
     * Cleanup the entry.
     */
    List<FieldChange> cleanup(BibtexEntry entry);

}