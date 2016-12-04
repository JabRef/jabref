package net.sf.jabref.model.groups;

import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;

/**
 * This group contains all entries. Always. At any time!
 */
public class AllEntriesGroup extends AbstractGroup {

    public static final String ID = "AllEntriesGroup:";


    public AllEntriesGroup(String name) {
        super(name, GroupHierarchyType.INDEPENDENT);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AllEntriesGroup(getName());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AllEntriesGroup;
    }

    @Override
    public String toString() {
        return AllEntriesGroup.ID;
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
        // TODO Auto-generated method stub
        return super.hashCode();
    }
}
