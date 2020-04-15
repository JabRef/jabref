package org.jabref.model.groups;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;

/**
 * This group contains all entries. Always. At any time!
 */
public class AllEntriesGroup extends AbstractGroup {

    public AllEntriesGroup(String name) {
        super(name, GroupHierarchyType.INDEPENDENT);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AllEntriesGroup(getName());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AllEntriesGroup && Objects.equals(((AllEntriesGroup) o).getName(), getName());
    }

    @Override
    public boolean contains(BibEntry entry) {
        return true;
    }

    @Override
    public boolean isDynamic() {
        // this is actually a special case; I define it as non-dynamic
        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
