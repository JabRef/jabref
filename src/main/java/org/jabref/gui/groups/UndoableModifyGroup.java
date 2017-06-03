package org.jabref.gui.groups;

import java.util.List;

import org.jabref.gui.undo.AbstractUndoableJabRefEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;

class UndoableModifyGroup extends AbstractUndoableJabRefEdit {

    private final GroupSidePane groupSidePane;
    private final AbstractGroup m_oldGroupBackup;
    private final AbstractGroup m_newGroupBackup;
    private final GroupTreeNode m_groupsRootHandle;
    private final List<Integer> m_pathToNode;


    /**
     * @param node
     *            The node which still contains the old group.
     * @param newGroup
     *            The new group to replace the one currently stored in <b>node
     *            </b>.
     */
    public UndoableModifyGroup(GroupSidePane gs, GroupTreeNodeViewModel groupsRoot,
                               GroupTreeNodeViewModel node, AbstractGroup newGroup) {
        groupSidePane = gs;
        m_oldGroupBackup = node.getNode().getGroup().deepCopy();
        m_newGroupBackup = newGroup.deepCopy();
        m_pathToNode = node.getNode().getIndexedPathFromRoot();
        m_groupsRootHandle = groupsRoot.getNode();
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("modify group");
    }

    @Override
    public void undo() {
        super.undo();
        //TODO: NULL
        m_groupsRootHandle.getDescendant(m_pathToNode).get().setGroup(
                m_oldGroupBackup.deepCopy());
    }

    @Override
    public void redo() {
        super.redo();
        m_groupsRootHandle.getDescendant(m_pathToNode).get().setGroup(
                m_newGroupBackup.deepCopy());
    }
}
