package net.sf.jabref.groups;

import javax.swing.undo.AbstractUndoableEdit;

/**
 * @author zieren
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class UndoableMoveGroup extends AbstractUndoableEdit {
    private final GroupSelector m_groupSelector;
    private final GroupTreeNode m_groupsRootHandle;
    private final int[] m_pathToNewParent;
    private final int m_newChildIndex;
    private final int[] m_pathToOldParent;
    private final int m_oldChildIndex;

    /**
     * @param moveNode
     *            The node which is being moved. At the time of construction of
     *            this object, it must not have moved yet.
     * @param newParent
     *            The new parent node to which <b>moveNode </b> will be moved.
     * @param newChildIndex
     *            The child index at <b>newParent </b> to which <b>moveNode </b>
     *            will be moved.
     */
    public UndoableMoveGroup(GroupSelector gs, GroupTreeNode groupsRoot,
            GroupTreeNode moveNode, GroupTreeNode newParent, int newChildIndex) {
        m_groupSelector = gs;
        m_groupsRootHandle = groupsRoot;
        m_pathToNewParent = newParent.getIndexedPath();
        m_newChildIndex = newChildIndex;
        m_pathToOldParent = ((GroupTreeNode) moveNode.getParent())
                .getIndexedPath();
        m_oldChildIndex = moveNode.getParent().getIndex(moveNode);
    }

    public String getUndoPresentationName() {
        return "Undo: move group";
    }

    public String getRedoPresentationName() {
        return "Redo: move group";
    }

    public void undo() {
        super.undo();
        GroupTreeNode cursor = m_groupsRootHandle
                .getDescendant(m_pathToNewParent);
        cursor = (GroupTreeNode) cursor.getChildAt(m_newChildIndex);
        m_groupsRootHandle.getDescendant(m_pathToOldParent).insert(cursor,
                m_oldChildIndex);
        m_groupSelector.revalidateGroups();
    }

    public void redo() {
        super.redo();
        GroupTreeNode cursor = m_groupsRootHandle
                .getDescendant(m_pathToOldParent);
        cursor = (GroupTreeNode) cursor.getChildAt(m_oldChildIndex);
        m_groupsRootHandle.getDescendant(m_pathToNewParent).insert(cursor,
                m_newChildIndex);
        m_groupSelector.revalidateGroups();
    }
}
