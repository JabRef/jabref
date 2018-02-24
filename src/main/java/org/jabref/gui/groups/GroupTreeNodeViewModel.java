package org.jabref.gui.groups;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
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

public class GroupTreeNodeViewModel implements Transferable, TreeNode {

    private static final DataFlavor FLAVOR;
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupTreeNodeViewModel.class);
    private static final DataFlavor[] FLAVORS;

    static {
        DataFlavor df = null;
        try {
            df = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=" + GroupTreeNode.class.getCanonicalName());
        } catch (ClassNotFoundException e) {
            LOGGER.error("Creating DataFlavor failed. This should not happen.", e);
        }
        FLAVOR = df;
        FLAVORS = new DataFlavor[] {GroupTreeNodeViewModel.FLAVOR};
    }

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

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return GroupTreeNodeViewModel.FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor someFlavor) {
        return someFlavor.equals(GroupTreeNodeViewModel.FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor someFlavor)
            throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(someFlavor)) {
            throw new UnsupportedFlavorException(someFlavor);
        }
        return this;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return node.getChildAt(childIndex).map(GroupTreeNodeViewModel::new).orElse(null);
    }

    @Override
    public int getChildCount() {
        return node.getNumberOfChildren();
    }

    @Override
    public TreeNode getParent() {
        return node.getParent().map(GroupTreeNodeViewModel::new).orElse(null);
    }

    @Override
    public int getIndex(TreeNode child) {
        if (!(child instanceof GroupTreeNodeViewModel)) {
            return -1;
        }

        GroupTreeNodeViewModel childViewModel = (GroupTreeNodeViewModel) child;
        return node.getIndexOfChild(childViewModel.getNode()).orElse(-1);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return node.isLeaf();
    }

    @Override
    public Enumeration<GroupTreeNodeViewModel> children() {
        Iterable<GroupTreeNode> children = node.getChildren();
        return new Enumeration<GroupTreeNodeViewModel>() {

            @Override
            public boolean hasMoreElements() {
                return children.iterator().hasNext();
            }

            @Override
            public GroupTreeNodeViewModel nextElement() {
                return new GroupTreeNodeViewModel(children.iterator().next());
            }
        };
    }

    public GroupTreeNode getNode() {
        return node;
    }

    /** Expand this node and all its children. */
    public void expandSubtree(JTree tree) {
        tree.expandPath(this.getTreePath());

        for (GroupTreeNodeViewModel child : getChildren()) {
            child.expandSubtree(tree);
        }
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

    public TreePath getTreePath() {
        List<GroupTreeNode> pathToNode = node.getPathFromRoot();
        return new TreePath(pathToNode.stream().map(GroupTreeNodeViewModel::new).toArray());
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

        UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(this,
                new GroupTreeNodeViewModel(newNode), UndoableAddOrRemoveGroup.ADD_NODE);
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
