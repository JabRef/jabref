package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.undo.AbstractUndoableJabRefEdit;
import org.jabref.model.groups.GroupTreeNode;

public class UndoableModifySubtree extends AbstractUndoableJabRefEdit {

    /**
     * A backup of the groups before the modification
     */
    private final GroupTreeNode m_groupRoot;

    private final GroupTreeNode m_subtreeBackup;

    /**
     * The path to the global groups root node
     */
    private final List<Integer> m_subtreeRootPath;

    /**
     * This holds the new subtree (the root's modified children) to allow redo.
     */
    private final List<GroupTreeNode> m_modifiedSubtree = new ArrayList<>();

    private final String m_name;

    /**
     * @param subtree The root node of the subtree that was modified (this node may not be modified, it is just used as a convenience handle).
     */
    public UndoableModifySubtree(GroupTreeNodeViewModel groupRoot,
                                 GroupTreeNodeViewModel subtree, String name) {
        m_subtreeBackup = subtree.getNode().copySubtree();
        m_groupRoot = groupRoot.getNode();
        m_subtreeRootPath = subtree.getNode().getIndexedPathFromRoot();
        m_name = name;
    }

    @Override
    public String getPresentationName() {
        return m_name;
    }

    @Override
    public void undo() {
        super.undo();
        // remember modified children for redo
        m_modifiedSubtree.clear();
        // get node to edit
        final GroupTreeNode subtreeRoot = m_groupRoot.getDescendant(m_subtreeRootPath).get(); // TODO: NULL
        m_modifiedSubtree.addAll(subtreeRoot.getChildren());
        // keep subtree handle, but restore everything else from backup
        subtreeRoot.removeAllChildren();
        for (GroupTreeNode child : m_subtreeBackup.getChildren()) {
            child.copySubtree().moveTo(subtreeRoot);
        }
    }

    @Override
    public void redo() {
        super.redo();
        final GroupTreeNode subtreeRoot = m_groupRoot.getDescendant(m_subtreeRootPath).get(); // TODO: NULL
        subtreeRoot.removeAllChildren();
        for (GroupTreeNode modifiedNode : m_modifiedSubtree) {
            modifiedNode.moveTo(subtreeRoot);
        }
    }
}
