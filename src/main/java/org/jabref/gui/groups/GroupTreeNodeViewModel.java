package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.SearchGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupTreeNodeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupTreeNodeViewModel.class);

    private final GroupTreeNode node;

    public GroupTreeNodeViewModel(GroupTreeNode node) {
        this.node = node;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GroupTreeNodeViewModel{");
        sb.append("node=").append(node);
        sb.append('}');
        return sb.toString();
    }

    public GroupTreeNode getNode() {
        return node;
    }

    public List<GroupTreeNodeViewModel> getChildren() {
        List<GroupTreeNodeViewModel> children = new ArrayList<>();
        for (GroupTreeNode child : node.getChildren()) {
            children.add(new GroupTreeNodeViewModel(child));
        }
        return children;
    }

    protected boolean printInItalics() {
        return node.getGroup().isDynamic();
    }

    public String getDescription() {
        AbstractGroup group = node.getGroup();
        String shortDescription = "";
        boolean showDynamic = true;
        if (group instanceof ExplicitGroup) {
            shortDescription = GroupDescriptions.getShortDescriptionExplicitGroup((ExplicitGroup) group);
        } else if (group instanceof KeywordGroup) {
            shortDescription = GroupDescriptions.getShortDescriptionKeywordGroup((KeywordGroup) group, showDynamic);
        } else if (group instanceof SearchGroup) {
            shortDescription = GroupDescriptions.getShortDescription((SearchGroup) group, showDynamic);
        } else {
            shortDescription = GroupDescriptions.getShortDescriptionAllEntriesGroup();
        }
        return "<html>" + shortDescription + "</html>";
    }

    public boolean canAddEntries(List<BibEntry> entries) {
        return (getNode().getGroup() instanceof GroupEntryChanger) && !getNode().getGroup().containsAll(entries);
    }

    public boolean canRemoveEntries(List<BibEntry> entries) {
        return (getNode().getGroup() instanceof GroupEntryChanger) && getNode().getGroup().containsAny(entries);
    }

    public void sortChildrenByName(boolean recursive) {
        getNode().sortChildren(
                (node1, node2) -> node1.getGroup().getName().compareToIgnoreCase(node2.getGroup().getName()),
                recursive);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        GroupTreeNodeViewModel viewModel = (GroupTreeNodeViewModel) o;
        return node.equals(viewModel.node);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    public String getName() {
        return getNode().getGroup().getName();
    }

    public boolean canBeEdited() {
        return getNode().getGroup() instanceof AllEntriesGroup;
    }

    public boolean canMoveUp() {
        return (getNode().getPreviousSibling() != null)
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveDown() {
        return (getNode().getNextSibling() != null)
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveLeft() {
        return !(getNode().getGroup() instanceof AllEntriesGroup)
                // TODO: Null!
                && !(getNode().getParent().get().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveRight() {
        return (getNode().getPreviousSibling() != null)
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public void changeEntriesTo(List<BibEntry> entries, UndoManager undoManager) {
        AbstractGroup group = node.getGroup();
        List<FieldChange> changesRemove = new ArrayList<>();
        List<FieldChange> changesAdd = new ArrayList<>();

        // Sort entries into current members and non-members of the group
        // Current members will be removed
        // Current non-members will be added
        List<BibEntry> toRemove = new ArrayList<>(entries.size());
        List<BibEntry> toAdd = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            // Sort according to current state of the entries
            if (group.contains(entry)) {
                toRemove.add(entry);
            } else {
                toAdd.add(entry);
            }
        }

        // If there are entries to remove
        if (!toRemove.isEmpty()) {
            changesRemove = removeEntriesFromGroup(toRemove);
        }
        // If there are entries to add
        if (!toAdd.isEmpty()) {
            changesAdd = addEntriesToGroup(toAdd);
        }

        // Remember undo information
        if (!changesRemove.isEmpty()) {
            AbstractUndoableEdit undoRemove = UndoableChangeEntriesOfGroup.getUndoableEdit(this, changesRemove);
            if (!changesAdd.isEmpty() && (undoRemove != null)) {
                // we removed and added entries
                undoRemove.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(this, changesAdd));
            }
            undoManager.addEdit(undoRemove);
        } else if (!changesAdd.isEmpty()) {
            undoManager.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(this, changesAdd));
        }
    }

    public List<FieldChange> removeEntriesFromGroup(List<BibEntry> entries) {
        return node.removeEntriesFromGroup(entries);
    }

    public boolean isAllEntriesGroup() {
        return getNode().getGroup() instanceof AllEntriesGroup;
    }

    public void addNewGroup(AbstractGroup newGroup, CountingUndoManager undoManager) {
        GroupTreeNode newNode = GroupTreeNode.fromGroup(newGroup);
        this.getNode().addChild(newNode);

        UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(
                this,
                new GroupTreeNodeViewModel(newNode),
                UndoableAddOrRemoveGroup.ADD_NODE);
        undoManager.addEdit(undo);
    }

    /**
     * Adds the given entries to this node's group.
     */
    public List<FieldChange> addEntriesToGroup(List<BibEntry> entries) {
        return node.addEntriesToGroup(entries);
    }

    public void subscribeToDescendantChanged(Consumer<GroupTreeNodeViewModel> subscriber) {
        getNode().subscribeToDescendantChanged(node -> subscriber.accept(new GroupTreeNodeViewModel(node)));
    }
}
