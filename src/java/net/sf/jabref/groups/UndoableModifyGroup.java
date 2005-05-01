/*
 Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref.groups;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.Globals;

public class UndoableModifyGroup extends AbstractUndoableEdit {
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

    public String getUndoPresentationName() {
        return Globals.lang("Undo") + ": " 
            + Globals.lang("modify group");
    }

    public String getRedoPresentationName() {
        return Globals.lang("Redo") + ": " 
            + Globals.lang("modify group");
    }

    public void undo() {
        super.undo();
        m_groupsRootHandle.getDescendant(m_pathToNode).setGroup(
                m_oldGroupBackup.deepCopy());
        m_groupSelector.revalidateGroups();
    }

    public void redo() {
        super.redo();
        m_groupsRootHandle.getDescendant(m_pathToNode).setGroup(
                m_newGroupBackup.deepCopy());
        m_groupSelector.revalidateGroups();
    }
}
