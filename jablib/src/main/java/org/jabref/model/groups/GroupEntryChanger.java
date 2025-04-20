package org.jabref.model.groups;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

/**
 * Some groups can change entries so that they match (or no longer match) the group.
 * This functionality is encoded in this interface.
 */
public interface GroupEntryChanger {

    /**
     * Adds the specified entries to this group.
     *
     * @return If this group or one or more entries was/were modified as a
     * result of this operation, a list of changes is returned.
     */
    List<FieldChange> add(Collection<BibEntry> entriesToAdd);

    default List<FieldChange> add(BibEntry entryToAdd) {
        return add(Collections.singletonList(entryToAdd));
    }

    /**
     * Removes the specified entries from this group.
     *
     * @return If this group or one or more entries was/were modified as a
     * result of this operation, a list of changes is returned.
     */
    List<FieldChange> remove(List<BibEntry> entriesToRemove);

    default List<FieldChange> remove(BibEntry entryToAdd) {
        return remove(Collections.singletonList(entryToAdd));
    }
}
