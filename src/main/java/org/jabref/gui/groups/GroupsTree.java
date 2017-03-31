package org.jabref.gui.groups;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreeSelectionModel;

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

    /** Highlights the specified cell or disables highlight if cell == null */
    public void setHighlightBorderCell(GroupTreeNodeViewModel node) {
        localCellRenderer.setHighlightBorderCell(node);
        repaint();
    }

    /** Sort immediate children of the specified node alphabetically. */
    public void sort(GroupTreeNodeViewModel node, boolean recursive) {
        node.sortChildrenByName(recursive);
    }
}
