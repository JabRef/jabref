/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.groups;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.logic.l10n.Localization;

class UndoableModifyGroup extends AbstractUndoableEdit {

    private final GroupSelector m_groupSelector;
    private final AbstractGroup m_oldGroupBackup;
    private final AbstractGroup m_newGroupBackup;
    private final GroupTreeNode m_groupsRootHandle;
    private final int[] m_pathToNode;


    /**
     * @param node
     *            The node which still contains the old group.
     * @param newGroup
     *            The new group to replace the one currently stored in <b>node
     *            </b>.
     */
    public UndoableModifyGroup(GroupSelector gs, GroupTreeNode groupsRoot,
            GroupTreeNode node, AbstractGroup newGroup) {
        m_groupSelector = gs;
        m_oldGroupBackup = node.getGroup().deepCopy();
        m_newGroupBackup = newGroup.deepCopy();
        m_pathToNode = node.getIndexedPath();
        m_groupsRootHandle = groupsRoot;
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": "
                + Localization.lang("modify group");
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": "
                + Localization.lang("modify group");
    }

    @Override
    public void undo() {
        super.undo();
        m_groupsRootHandle.getDescendant(m_pathToNode).setGroup(
                m_oldGroupBackup.deepCopy());
        m_groupSelector.revalidateGroups();
    }

    @Override
    public void redo() {
        super.redo();
        m_groupsRootHandle.getDescendant(m_pathToNode).setGroup(
                m_newGroupBackup.deepCopy());
        m_groupSelector.revalidateGroups();
    }
}
