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

import net.sf.jabref.Globals;

class UndoableResetGroups extends AbstractUndoableEdit {
    /** A backup of the groups before the modification */
    private final GroupTreeNode m_groupsBackup;
    /** A handle to the global groups root node */
    private final GroupTreeNode m_groupsRootHandle;
    private final GroupSelector m_groupSelector;
    private boolean m_revalidate = true;

    public UndoableResetGroups(GroupSelector groupSelector,
            GroupTreeNode groupsRoot) {
        this.m_groupsBackup = groupsRoot.deepCopy();
        this.m_groupsRootHandle = groupsRoot;
        this.m_groupSelector = groupSelector;
    }

    public String getUndoPresentationName() {
        return Globals.lang("Undo") + ": " 
            + Globals.lang("clear all groups");
    }

    public String getRedoPresentationName() {
        return Globals.lang("Redo") + ": " 
            + Globals.lang("clear all groups");
    }

    public void undo() {
        super.undo();
        // keep root handle, but restore everything else from backup
        m_groupsRootHandle.removeAllChildren();
        m_groupsRootHandle.setGroup(m_groupsBackup.getGroup().deepCopy());
        for (int i = 0; i < m_groupsBackup.getChildCount(); ++i)
            m_groupsRootHandle.add(((GroupTreeNode) m_groupsBackup
                    .getChildAt(i)).deepCopy());
        if (m_revalidate)
            m_groupSelector.revalidateGroups();
    }

    public void redo() {
        super.redo();
        m_groupsRootHandle.removeAllChildren();
        m_groupsRootHandle.setGroup(new AllEntriesGroup());
        if (m_revalidate)
            m_groupSelector.revalidateGroups();
    }

    /**
     * Call this method to decide if the group list should be immediately
     * revalidated by this operation. Default is true.
     */
    public void setRevalidate(boolean revalidate) {
        m_revalidate = revalidate;
    }
}
