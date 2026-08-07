package org.jabref.model.groups;

import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

public class AutomaticEntryTypeGroup extends AutomaticGroup {

    public AutomaticEntryTypeGroup(String name, GroupHierarchyType context) {
        super(name, context);
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        EntryType type = entry.getType();
        return Set.of(new GroupTreeNode(new EntryTypeGroup(type.getName(),
                GroupHierarchyType.INDEPENDENT, type)));
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticEntryTypeGroup(getName(), getHierarchicalContext());
    }
}
