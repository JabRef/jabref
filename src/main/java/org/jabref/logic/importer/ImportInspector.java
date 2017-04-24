package org.jabref.logic.importer;

import org.jabref.model.entry.BibEntry;

/**
 * An ImportInspector can be passed to a EntryFetcher and will receive entries
 * as they are fetched from somewhere.
 *
 * Currently there are two implementations: ImportInspectionDialog and
 * ImportInspectionCommandLine
 *
 */
public interface ImportInspector {

    /**
     * Notify the ImportInspector about the progress of the operation.
     *
     * The Inspector for instance could display a progress bar with the given
     * values.
     *
     * @param current
     *            A number that is related to the work already done.
     *
     * @param max
     *            A current estimate for the total amount of work to be done.
     */
    void setProgress(int current, int max);

    /**
     * Add the given entry to the list of entries managed by the inspector.
     *
     * @param entry
     *            The entry to add.
     */
    void addEntry(BibEntry entry);
}
