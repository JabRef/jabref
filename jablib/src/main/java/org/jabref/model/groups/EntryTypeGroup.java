package org.jabref.model.groups;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

public class EntryTypeGroup extends AbstractGroup {

    private final EntryType entryType;

    public EntryTypeGroup(String groupName, GroupHierarchyType context, EntryType entryType) {
        super(groupName, context);
        this.entryType = entryType;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EntryTypeGroup that = (EntryTypeGroup) o;
        return Objects.equals(getEntryType(), that.getEntryType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getEntryType());
    }

    @Override
    public boolean contains(BibEntry entry) {
        return getEntryType().equals(entry.getType());
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new EntryTypeGroup(getName(), getHierarchicalContext(), getEntryType());
    }
}
