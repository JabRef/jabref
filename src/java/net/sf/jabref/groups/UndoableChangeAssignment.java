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

/**
 * @author jzieren
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class UndoableChangeAssignment extends AbstractUndoableEdit {
    private final Set m_previousAssignmentBackup;
    private final Set m_currentAssignmentBackup;
    private final Set m_currentAssignmentReference;

    /**
     * 
     * @param previousAssignment
     *            The set of assigned entries before the change (this is
     *            copied).
     * 
     * @param currentAssignment
     *            A reference to the actual set of assignments
     */
    public UndoableChangeAssignment(Set previousAssignment,
            Set currentAssignment) {
        m_previousAssignmentBackup = new HashSet(previousAssignment);
        m_currentAssignmentReference = currentAssignment;
        m_currentAssignmentBackup = new HashSet(currentAssignment);
    }

    public String getUndoPresentationName() {
        return "Undo: (de)assign entries";
    }

    public String getRedoPresentationName() {
        return "Redo: (de)assign entries";
    }

    public void undo() {
        super.undo();
        m_currentAssignmentReference.clear();
        m_currentAssignmentReference.addAll(m_previousAssignmentBackup);
    }

    public void redo() {
        super.redo();
        m_currentAssignmentReference.clear();
        m_currentAssignmentReference.addAll(m_currentAssignmentBackup);
    }
}
