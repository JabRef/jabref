/*
 * Created on 03.01.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jabref.groups;

import java.util.*;

import javax.swing.undo.AbstractUndoableEdit;

/**
 * @author zieren
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
