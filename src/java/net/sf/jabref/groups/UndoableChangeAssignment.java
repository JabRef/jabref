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

import java.util.*;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.BibtexEntry;

/**
 * @author jzieren
 * 
 */
public class UndoableChangeAssignment extends AbstractUndoableEdit {
    private final Set m_previousAssignmentBackup;
    private final Set m_newAssignmentBackup;
    /** The path to the edited node */
    private int[] m_pathToNode = null;
    /** The root of the global groups tree */
    private GroupTreeNode m_groupsRootHandle = null;

    public UndoableChangeAssignment(Set previousAssignment,
            Set currentAssignment) {
        m_previousAssignmentBackup = new HashSet(previousAssignment);
        m_newAssignmentBackup = new HashSet(currentAssignment);
    }

    /**
     * Sets the node of the group that was edited. This method has to be called
     * before this instance may be used.
     * 
     * @param node
     *            The node whose assignments were edited.
     */
    public void setEditedNode(GroupTreeNode node) {
        m_groupsRootHandle = (GroupTreeNode) node.getRoot();
        m_pathToNode = node.getIndexedPath();
    }

    public String getUndoPresentationName() {
        return "Undo: (de)assign entries";
    }

    public String getRedoPresentationName() {
        return "Redo: (de)assign entries";
    }

    public void undo() {
        super.undo();
        ExplicitGroup group = (ExplicitGroup) m_groupsRootHandle.getChildAt(
                m_pathToNode).getGroup();
        group.clearAssignments();
        for (Iterator it = m_previousAssignmentBackup.iterator(); it.hasNext();)
            group.addEntry((BibtexEntry) it.next());
    }

    public void redo() {
        super.redo();
        ExplicitGroup group = (ExplicitGroup) m_groupsRootHandle.getChildAt(
                m_pathToNode).getGroup();
        group.clearAssignments();
        for (Iterator it = m_newAssignmentBackup.iterator(); it.hasNext();)
            group.addEntry((BibtexEntry) it.next());
    }
}
