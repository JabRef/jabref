/*  Copyright (C) 2003-2015 JabRef contributors.
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

import java.util.HashSet;
import java.util.Set;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.groups.structure.ExplicitGroup;
import net.sf.jabref.logic.l10n.Localization;

/**
 * @author jzieren
 *
 */
public class UndoableChangeAssignment extends AbstractUndoableEdit {

    private final Set<BibEntry> m_previousAssignmentBackup;
    private final Set<BibEntry> m_newAssignmentBackup;
    /** The path to the edited node */
    private int[] mPathToNode;
    /** The root of the global groups tree */
    private GroupTreeNode mGroupsRootHandle;


    /**
     * Constructor for use in a group itself, where the enclosing node is
     * unknown. The node must be set using setEditedNode() before this instance
     * may be used.
     *
     * @param previousAssignment
     * @param currentAssignment
     */
    public UndoableChangeAssignment(Set<BibEntry> previousAssignment,
            Set<BibEntry> currentAssignment) {
        m_previousAssignmentBackup = new HashSet<>(previousAssignment);
        m_newAssignmentBackup = new HashSet<>(currentAssignment);
    }

    public UndoableChangeAssignment(Set<BibEntry> previousAssignment,
            Set<BibEntry> currentAssignment, GroupTreeNode node) {
        this(previousAssignment, currentAssignment);
        setEditedNode(node);
    }

    /**
     * Sets the node of the group that was edited. If this node was not
     * specified at construction time, this method has to be called before this
     * instance may be used.
     *
     * @param node
     *            The node whose assignments were edited.
     */
    public void setEditedNode(GroupTreeNode node) {
        mGroupsRootHandle = (GroupTreeNode) node.getRoot();
        mPathToNode = node.getIndexedPath();
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": "
                + Localization.lang("change assignment of entries");
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": "
                + Localization.lang("change assignment of entries");
    }

    @Override
    public void undo() {
        super.undo();
        GroupTreeNode treeNode =  mGroupsRootHandle.getChildAt(mPathToNode);
        if (treeNode != null) {
            ExplicitGroup group = (ExplicitGroup) treeNode.getGroup();
            group.clearAssignments();
            for (final BibEntry aM_previousAssignmentBackup : m_previousAssignmentBackup) {
                group.addEntry(aM_previousAssignmentBackup);
            }
        }
    }

    @Override
    public void redo() {
        super.redo();
        GroupTreeNode treeNode = mGroupsRootHandle.getChildAt(mPathToNode);
        if (treeNode != null) {
            ExplicitGroup group = (ExplicitGroup) treeNode.getGroup();
            group.clearAssignments();
            for (final BibEntry aM_newAssignmentBackup : m_newAssignmentBackup) {
                group.addEntry(aM_newAssignmentBackup);
            }
        }
    }
}
