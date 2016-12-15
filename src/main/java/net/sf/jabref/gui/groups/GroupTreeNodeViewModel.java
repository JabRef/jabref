package net.sf.jabref.gui.groups;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.undo.CountingUndoManager;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.AllEntriesGroup;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupEntryChanger;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.KeywordGroup;
import net.sf.jabref.model.groups.SearchGroup;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GroupTreeNodeViewModel implements Transferable, TreeNode {

    private static final Log LOGGER = LogFactory.getLog(GroupTreeNodeViewModel.class);

    private static final Icon GROUP_REFINING_ICON = IconTheme.JabRefIcon.GROUP_REFINING.getSmallIcon();
    private static final Icon GROUP_INCLUDING_ICON = IconTheme.JabRefIcon.GROUP_INCLUDING.getSmallIcon();
    private static final Icon GROUP_REGULAR_ICON = null;

    public static final DataFlavor FLAVOR;
    private static final DataFlavor[] FLAVORS;

    static {
        DataFlavor df = null;
        try {
            df = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class="+GroupTreeNode.class.getCanonicalName());
        } catch (ClassNotFoundException e) {
            LOGGER.error("Creating DataFlavor failed. This should not happen.", e);
        }
        FLAVOR = df;
        FLAVORS = new DataFlavor[] {GroupTreeNodeViewModel.FLAVOR};
    }

    private final GroupTreeNode node;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GroupTreeNodeViewModel{");
        sb.append("node=").append(node);
        sb.append('}');
        return sb.toString();
    }

    public GroupTreeNodeViewModel(GroupTreeNode node) {
        this.node = node;
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
        if(! (child instanceof GroupTreeNodeViewModel)) {
            return -1;
        }

        GroupTreeNodeViewModel childViewModel = (GroupTreeNodeViewModel)child;
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
    public Enumeration children() {
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

    /** Collapse this node and all its children. */
    public void collapseSubtree(JTree tree) {
        tree.collapsePath(this.getTreePath());

        for(GroupTreeNodeViewModel child : getChildren()) {
            child.collapseSubtree(tree);
        }
    }

    /** Expand this node and all its children. */
    public void expandSubtree(JTree tree) {
        tree.expandPath(this.getTreePath());

        for(GroupTreeNodeViewModel child : getChildren()) {
            child.expandSubtree(tree);
        }
    }

    public List<GroupTreeNodeViewModel> getChildren() {
        List<GroupTreeNodeViewModel> children = new ArrayList<>();
        for(GroupTreeNode child : node.getChildren()) {
            children.add(new GroupTreeNodeViewModel(child));
        }
        return children;
    }

    protected boolean printInItalics() {
        return Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC) &&  node.getGroup().isDynamic();
    }

    public String getText() {
        AbstractGroup group = node.getGroup();
        StringBuilder sb = new StringBuilder(60);
        sb.append(group.getName());

        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_NUMBER_OF_ELEMENTS)
                && (JabRefGUI.getMainFrame() != null)) {
            BasePanel currentBasePanel = JabRefGUI.getMainFrame().getCurrentBasePanel();
            if (currentBasePanel != null) {
                sb.append(" [").append(node.numberOfMatches(currentBasePanel.getDatabase().getEntries())).append(']');
            }
        }

        return sb.toString();
    }

    public String getDescription() {
        AbstractGroup group = node.getGroup();
        String shortDescription = "";
        boolean showDynamic = Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC);
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

    public Icon getIcon() {
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_ICONS)) {
            switch (node.getGroup().getHierarchicalContext()) {
            case REFINING:
                return GROUP_REFINING_ICON;
            case INCLUDING:
                return GROUP_INCLUDING_ICON;
            default:
                return GROUP_REGULAR_ICON;
            }
        } else {
            return null;
        }
    }

    public TreePath getTreePath() {
        List<GroupTreeNode> pathToNode = node.getPathFromRoot();
        return new TreePath(pathToNode.stream().map(GroupTreeNodeViewModel::new).toArray());
    }

    public boolean canAddEntries(List<BibEntry> entries) {
        return getNode().getGroup() instanceof GroupEntryChanger && !getNode().getGroup().containsAll(entries);
    }

    public boolean canRemoveEntries(List<BibEntry> entries) {
        return getNode().getGroup() instanceof GroupEntryChanger && getNode().getGroup().containsAny(entries);
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

    public Optional<MoveGroupChange> moveUp() {
        final GroupTreeNode parent = node.getParent().get();
        // TODO: Null!
        final int index = parent.getIndexOfChild(getNode()).get();
        if (index > 0) {
            getNode().moveTo(parent, index - 1);
            return Optional.of(new MoveGroupChange(parent, index, parent, index - 1));
        }
        return Optional.empty();
    }

    public Optional<MoveGroupChange> moveDown() {
        final GroupTreeNode parent = node.getParent().get();
        // TODO: Null!
        final int index = parent.getIndexOfChild(node).get();
        if (index < (parent.getNumberOfChildren() - 1)) {
            node.moveTo(parent, index + 1);
            return Optional.of(new MoveGroupChange(parent, index, parent, index + 1));
        }
        return Optional.empty();
    }

    public Optional<MoveGroupChange> moveLeft() {
        final GroupTreeNode parent = node.getParent().get(); // TODO: Null!
        final Optional<GroupTreeNode> grandParent = parent.getParent();
        final int index = node.getPositionInParent();

        if (! grandParent.isPresent()) {
            return Optional.empty();
        }
        final int indexOfParent = grandParent.get().getIndexOfChild(parent).get();
        node.moveTo(grandParent.get(), indexOfParent + 1);
        return Optional.of(new MoveGroupChange(parent, index, grandParent.get(), indexOfParent + 1));
    }

    public Optional<MoveGroupChange> moveRight() {
        final GroupTreeNode previousSibling = node.getPreviousSibling().get(); // TODO: Null
        final GroupTreeNode parent = node.getParent().get(); // TODO: Null!
        final int index = node.getPositionInParent();

        if (previousSibling == null) {
            return Optional.empty();
        }

        node.moveTo(previousSibling);
        return Optional.of(new MoveGroupChange(parent, index, previousSibling, previousSibling.getNumberOfChildren()));
    }

    /**
     * Adds the given entries to this node's group.
     */
    public List<FieldChange> addEntriesToGroup(List<BibEntry> entries) {
        if(node.getGroup() instanceof GroupEntryChanger) {
            return ((GroupEntryChanger)node.getGroup()).add(entries);
        }
        else {
            return Collections.emptyList();
        }
    }

    /**
     * Removes the given entries from this node's group.
     */
    public List<FieldChange> removeEntriesFromGroup(List<BibEntry> entries) {
        if(node.getGroup() instanceof GroupEntryChanger) {
            return ((GroupEntryChanger)node.getGroup()).remove(entries);
        }
        else {
            return Collections.emptyList();
        }
    }

    public void subscribeToDescendantChanged(Consumer<GroupTreeNodeViewModel> subscriber) {
        getNode().subscribeToDescendantChanged(node -> subscriber.accept(new GroupTreeNodeViewModel(node)));
    }
}
