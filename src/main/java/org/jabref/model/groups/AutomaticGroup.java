package org.jabref.model.groups;

import java.util.Set;

import javafx.collections.ObservableList;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.TreeCollector;

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

    public ObservableList<GroupTreeNode> createSubgroups(ObservableList<BibEntry> entries) {
        // TODO: Propagate changes to entry list (however: there is no flatMap and collect as TransformationList)
        return entries.stream()
                .flatMap(entry -> createSubgroups(entry).stream())
                .collect(TreeCollector.mergeIntoTree(GroupTreeNode::isSameGroupAs));
    }
}
