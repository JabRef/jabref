/*
All programs in this directory and subdirectories are published under the 
GNU General Public License as described below.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 2 of the License, or (at your option) 
any later version.

This program is distributed in the hope that it will be useful, but WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
more details.

You should have received a copy of the GNU General Public License along 
with this program; if not, write to the Free Software Foundation, Inc., 59 
Temple Place, Suite 330, Boston, MA 02111-1307 USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html
*/

package net.sf.jabref.groups;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.Globals;

/**
 * @author jzieren
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
        return Globals.lang("Undo") + ": " 
            + Globals.lang("move group");
    }

    public String getRedoPresentationName() {
        return Globals.lang("Redo") + ": " 
            + Globals.lang("move group");
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
