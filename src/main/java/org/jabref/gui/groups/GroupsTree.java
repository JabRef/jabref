package org.jabref.gui.groups;

import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jabref.model.groups.GroupTreeNode;

public class GroupsTree extends JTree {

    private final GroupTreeCellRenderer localCellRenderer = new GroupTreeCellRenderer();

    /**
     * @param groupSelector the parent UI component
     */
    public GroupsTree(GroupSelector groupSelector) {
        setCellRenderer(localCellRenderer);
        setFocusable(false);
        setToggleClickCount(0);
        ToolTipManager.sharedInstance().registerComponent(this);
        setShowsRootHandles(false);
        getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        this.setFocusable(true);
    }

    /** Returns the first selected node, or null if nothing is selected. */
    private GroupTreeNodeViewModel getSelectedNode() {
        TreePath selectionPath = getSelectionPath();
        return selectionPath == null ? null : (GroupTreeNodeViewModel) selectionPath.getLastPathComponent();
    }

    /**
     * Refresh paths that may have become invalid due to node movements within
     * the tree. This method creates new paths to the last path components
     * (which must still exist) of the specified paths.
     *
     * @param paths
     *            Paths that may have become invalid.
     * @return Refreshed paths that are all valid.
     */
    public Enumeration<TreePath> refreshPaths(Enumeration<TreePath> paths) {
        if (paths == null) {
            return new Vector<TreePath>().elements();
        }

        Vector<TreePath> freshPaths = new Vector<>();
        while (paths.hasMoreElements()) {
            freshPaths.add(((GroupTreeNodeViewModel) paths.nextElement().getLastPathComponent()).getTreePath());
        }
        return freshPaths.elements();
    }

    /**
     * Refresh paths that may have become invalid due to node movements within
     * the tree. This method creates new paths to the last path components
     * (which must still exist) of the specified paths.
     *
     * @param paths
     *            Paths that may have become invalid.
     * @return Refreshed paths that are all valid.
     */
    public TreePath[] refreshPaths(TreePath[] paths) {
        TreePath[] freshPaths = new TreePath[paths.length];
        for (int i = 0; i < paths.length; ++i) {
            freshPaths[i] = ((GroupTreeNodeViewModel) paths[i].getLastPathComponent()).getTreePath();
        }
        return freshPaths;
    }

    /**
     * Highlights the specified groups in red
     **/
    public void setOverlappingGroups(List<GroupTreeNode> nodes) {
        Objects.requireNonNull(nodes);
        localCellRenderer.setOverlappingGroups(nodes);
        repaint();
    }

    /**
     * Highlights the specified groups by underlining
     **/
    public void setMatchingGroups(List<GroupTreeNode> nodes) {
        Objects.requireNonNull(nodes);
        localCellRenderer.setMatchingGroups(nodes);
        repaint();
    }

    /** Highlights the specified cell or disables highlight if cell == null */
    public void setHighlightBorderCell(GroupTreeNodeViewModel node) {
        localCellRenderer.setHighlightBorderCell(node);
        repaint();
    }

    /** Sort immediate children of the specified node alphabetically. */
    public void sort(GroupTreeNodeViewModel node, boolean recursive) {
        node.sortChildrenByName(recursive);
    }

    /**
     * Returns true if the node specified by path has at least one descendant
     * that is currently expanded.
     */
    public boolean hasExpandedDescendant(TreePath path) {
        GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) path.getLastPathComponent();
        for (GroupTreeNodeViewModel child : node.getChildren()) {
            if (child.isLeaf()) {
                continue; // don't care about this case
            }
            TreePath pathToChild = path.pathByAddingChild(child);
            if (isExpanded(pathToChild) || hasExpandedDescendant(pathToChild)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the node specified by path has at least one descendant
     * that is currently collapsed.
     */
    public boolean hasCollapsedDescendant(TreePath path) {
        GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) path.getLastPathComponent();
        for (GroupTreeNodeViewModel child : node.getChildren()) {
            if (child.isLeaf()) {
                continue; // don't care about this case
            }
            TreePath pathToChild = path.pathByAddingChild(child);
            if (isCollapsed(pathToChild) || hasCollapsedDescendant(pathToChild)) {
                return true;
            }
        }
        return false;
    }
}
