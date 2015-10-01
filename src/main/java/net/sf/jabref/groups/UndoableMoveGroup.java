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

import net.sf.jabref.logic.l10n.Localization;

/**
 * @author jzieren
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
class UndoableMoveGroup extends AbstractUndoableEdit {

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

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": "
                + Localization.lang("move group");
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": "
                + Localization.lang("move group");
    }

    @Override
    public void undo() {
        super.undo();
        GroupTreeNode cursor = m_groupsRootHandle
                .getDescendant(m_pathToNewParent);
        cursor = (GroupTreeNode) cursor.getChildAt(m_newChildIndex);
        m_groupsRootHandle.getDescendant(m_pathToOldParent).insert(cursor,
                m_oldChildIndex);
        m_groupSelector.revalidateGroups();
    }

    @Override
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
