package org.jabref.gui.groups;

import java.util.List;

import org.jabref.gui.undo.AbstractUndoableJabRefEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.GroupTreeNode;

public class UndoableAddOrRemoveGroup extends AbstractUndoableJabRefEdit {

    /**
     * Adding of a single node (group).
     */
    public static final int ADD_NODE = 0;

    /**
     * Removal of a single node. Children, if any, are kept.
     */
    public static final int REMOVE_NODE_KEEP_CHILDREN = 1;

    /**
     * Removal of a node and all of its children.
     */
    public static final int REMOVE_NODE_AND_CHILDREN = 2;

    /**
     * The root of the global groups tree
     */
    private final GroupTreeNodeViewModel m_groupsRootHandle;

    /**
     * The subtree that was added or removed
     */
    private final GroupTreeNode m_subtreeBackup;

    /**
     * In case of removing a node but keeping all of its children, the number of children has to be stored.
     */
    private final int m_subtreeRootChildCount;

    /**
     * The path to the edited subtree's root node
     */
    private final List<Integer> m_pathToNode;

    /**
     * The type of the editing (ADD_NODE, REMOVE_NODE_KEEP_CHILDREN, REMOVE_NODE_AND_CHILDREN)
     */
    private final int m_editType;

    /**
     * Creates an object that can undo/redo an edit event.
     *
     * @param groupsRoot
     *            The global groups root.
     * @param editType
     *            The type of editing (ADD_NODE, REMOVE_NODE_KEEP_CHILDREN,
     *            REMOVE_NODE_AND_CHILDREN)
     * @param editedNode
     *            The edited node (which was added or will be removed). The node
     *            must be a descendant of node <b>groupsRoot</b>! This means
     *            that, in case of adding, you first have to add it to the tree,
     *            then call this constructor. When removing, you first have to
     *            call this constructor, then remove the node.
     */
    public UndoableAddOrRemoveGroup(GroupTreeNodeViewModel groupsRoot,
                                    GroupTreeNodeViewModel editedNode, int editType) {
        m_groupsRootHandle = groupsRoot;
        m_editType = editType;
        m_subtreeRootChildCount = editedNode.getChildren().size();
        // storing a backup of the whole subtree is not required when children
        // are kept
        m_subtreeBackup = editType != UndoableAddOrRemoveGroup.REMOVE_NODE_KEEP_CHILDREN ?
                editedNode.getNode()
                          .copySubtree()
                : GroupTreeNode.fromGroup(editedNode.getNode().getGroup().deepCopy());
        // remember path to edited node. this cannot be stored as a reference,
        // because the reference itself might change. the method below is more
        // robust.
        m_pathToNode = editedNode.getNode().getIndexedPathFromRoot();
    }

    @Override
    public String getPresentationName() {
        switch (m_editType) {
            case ADD_NODE:
                return Localization.lang("Add group");
            case REMOVE_NODE_KEEP_CHILDREN:
                return Localization.lang("Keep subgroups)");
            case REMOVE_NODE_AND_CHILDREN:
                return Localization.lang("Also remove subgroups");
            default:
                break;
        }
        return "? (" + Localization.lang("unknown edit") + ")";
    }

    @Override
    public void undo() {
        super.undo();
        doOperation(true);
    }

    @Override
    public void redo() {
        super.redo();
        doOperation(false);
    }

    private void doOperation(boolean undo) {
        GroupTreeNode cursor = m_groupsRootHandle.getNode();
        final int childIndex = m_pathToNode.get(m_pathToNode.size() - 1);
        // traverse path up to but last element
        for (int i = 0; i < (m_pathToNode.size() - 1); ++i) {
            cursor = cursor.getChildAt(m_pathToNode.get(i)).get();
        }
        if (undo) {
            switch (m_editType) {
                case ADD_NODE:
                    cursor.removeChild(childIndex);
                    break;
                case REMOVE_NODE_KEEP_CHILDREN:
                    // move all children to newNode, then add newNode
                    GroupTreeNode newNode = m_subtreeBackup.copySubtree();
                    for (int i = childIndex; i < (childIndex
                            + m_subtreeRootChildCount); ++i) {
                        cursor.getChildAt(childIndex).get().moveTo(newNode);
                    }
                    newNode.moveTo(cursor, childIndex);
                    break;
                case REMOVE_NODE_AND_CHILDREN:
                    m_subtreeBackup.copySubtree().moveTo(cursor, childIndex);
                    break;
                default:
                    break;
            }
        } else { // redo
            switch (m_editType) {
                case ADD_NODE:
                    m_subtreeBackup.copySubtree().moveTo(cursor, childIndex);
                    break;
                case REMOVE_NODE_KEEP_CHILDREN:
                    // remove node, then insert all children
                    GroupTreeNode removedNode = cursor
                            .getChildAt(childIndex).get();
                    cursor.removeChild(childIndex);
                    while (removedNode.getNumberOfChildren() > 0) {
                        removedNode.getFirstChild().get().moveTo(cursor, childIndex);
                    }
                    break;
                case REMOVE_NODE_AND_CHILDREN:
                    cursor.removeChild(childIndex);
                    break;
                default:
                    break;
            }
        }
    }
}
