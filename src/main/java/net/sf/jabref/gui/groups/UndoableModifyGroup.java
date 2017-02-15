package net.sf.jabref.gui.groups;

import java.util.List;

import net.sf.jabref.gui.undo.AbstractUndoableJabRefEdit;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.GroupTreeNode;

class UndoableModifyGroup extends AbstractUndoableJabRefEdit {

    private final GroupSelector groupSelector;
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
    public UndoableModifyGroup(GroupSelector gs, GroupTreeNodeViewModel groupsRoot,
            GroupTreeNodeViewModel node, AbstractGroup newGroup) {
        groupSelector = gs;
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
        groupSelector.revalidateGroups();
    }

    @Override
    public void redo() {
        super.redo();
        m_groupsRootHandle.getDescendant(m_pathToNode).get().setGroup(
                m_newGroupBackup.deepCopy());
        groupSelector.revalidateGroups();
    }
}
