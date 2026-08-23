package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jabref.gui.undo.UndoManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.SearchGroup;

public class GroupTreeNodeViewModel {
    private final GroupTreeNode node;

    public GroupTreeNodeViewModel(GroupTreeNode node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "GroupTreeNodeViewModel{" + "node=" + node + '}';
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
        shortDescription = switch (group) {
            case ExplicitGroup explicitGroup ->
                    GroupDescriptions.getShortDescriptionExplicitGroup(explicitGroup);
            case KeywordGroup keywordGroup ->
                    GroupDescriptions.getShortDescriptionKeywordGroup(keywordGroup, showDynamic);
            case SearchGroup searchGroup ->
                    GroupDescriptions.getShortDescription(searchGroup, showDynamic);
            case null,
                 default ->
                    GroupDescriptions.getShortDescriptionAllEntriesGroup();
        };
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
        return (getNode().getPreviousSibling().isPresent())
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveDown() {
        return (getNode().getNextSibling().isPresent())
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveLeft() {
        return !(getNode().getGroup() instanceof AllEntriesGroup)
                // TODO: Null!
                && !(getNode().getParent().get().getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveRight() {
        return (getNode().getPreviousSibling().isPresent())
                && !(getNode().getGroup() instanceof AllEntriesGroup);
    }

    public void changeEntriesTo(List<BibEntry> entries, UndoManager undoManager) {
        AbstractGroup group = node.getGroup();

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

        // Removing and adding is one action to the user, so it is one undo step. Nothing is
        // pushed when neither list yields a change.
        undoManager.addEdit(Localization.lang("change entries of group"), edit -> {
            if (!toRemove.isEmpty()) {
                edit.addAll(removeEntriesFromGroup(toRemove));
            }
            if (!toAdd.isEmpty()) {
                edit.addAll(addEntriesToGroup(toAdd));
            }
        });
    }

    public List<FieldChange> removeEntriesFromGroup(List<BibEntry> entries) {
        return node.removeEntriesFromGroup(entries);
    }

    public boolean isAllEntriesGroup() {
        return getNode().getGroup() instanceof AllEntriesGroup;
    }

    /// Adds the given entries to this node's group.
    public List<FieldChange> addEntriesToGroup(List<BibEntry> entries) {
        return node.addEntriesToGroup(entries);
    }

    public void subscribeToDescendantChanged(Consumer<GroupTreeNodeViewModel> subscriber) {
        getNode().subscribeToDescendantChanged(node -> subscriber.accept(new GroupTreeNodeViewModel(node)));
    }
}
