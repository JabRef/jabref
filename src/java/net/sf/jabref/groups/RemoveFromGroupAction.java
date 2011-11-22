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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

public class RemoveFromGroupAction extends AbstractAction {
    protected GroupTreeNode m_node;
    protected BasePanel m_panel;
    public RemoveFromGroupAction(GroupTreeNode node, BasePanel panel) {
        super(node.getGroup().getName());
        m_node = node;
        m_panel = panel;
    }
    public RemoveFromGroupAction() {
        super(Globals.lang("Remove entry selection from this group")); // JZTODO lyrics
    }
    public void setNode(GroupTreeNode node) {
        m_node = node;
    }
    public void setBasePanel(BasePanel panel) {
        m_panel = panel;
    }
    public void actionPerformed(ActionEvent evt) {
        // warn if assignment has undesired side effects (modifies a field != keywords)
        if (!Util.warnAssignmentSideEffects(new AbstractGroup[]{m_node.getGroup()},
                m_panel.getSelectedEntries(),
                m_panel.getDatabase(),
                m_panel.frame()))
            return; // user aborted operation
        
        AbstractUndoableEdit undo = m_node.removeFromGroup(m_panel.getSelectedEntries());
        if (undo == null)
            return; // no changed made
        
        m_panel.undoManager.addEdit(undo);
        m_panel.markBaseChanged();
        m_panel.updateEntryEditorIfShowing();
        m_panel.getGroupSelector().valueChanged(null);
    }
}
