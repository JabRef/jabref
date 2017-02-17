package net.sf.jabref.model.groups;

import java.util.Set;

import net.sf.jabref.model.entry.BibEntry;

public abstract class AutomaticGroup extends AbstractGroup {
    public AutomaticGroup(String name, GroupHierarchyType context) {
        super(name, context);
    }

    @Override
    public boolean contains(BibEntry entry) {
        return false;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    public abstract Set<GroupTreeNode> createSubgroups(BibEntry entry);
}
