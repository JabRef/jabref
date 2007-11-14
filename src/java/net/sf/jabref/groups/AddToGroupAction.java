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

import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.undo.NamedCompound;

public class AddToGroupAction extends AbstractAction {
    protected GroupTreeNode m_node;
    protected final boolean m_move;
    protected BasePanel m_panel;
    /**
     * @param move If true, remove node from all other groups.
     */
    public AddToGroupAction(GroupTreeNode node, boolean move,
            BasePanel panel) {
        super(node.getGroup().getName());
        m_node = node;
        m_move = move;
        m_panel = panel;
    }
    public AddToGroupAction(boolean move) {
        super(Globals.lang(move ? "Assign entry selection exclusively to this group"
                : "Add entry selection to this group")); // JZTODO lyrics
        m_move = move;
    }
    public void setBasePanel(BasePanel panel) {
        m_panel = panel;
    }
    public void setNode(GroupTreeNode node) {
        m_node = node;
    }
    public void actionPerformed(ActionEvent evt) {
        final BibtexEntry[] entries = m_panel.getSelectedEntries();
        final Vector<GroupTreeNode> removeGroupsNodes = new Vector<GroupTreeNode>(); // used only when moving
        
        if (m_move) {
            // collect warnings for removal
            Enumeration<GroupTreeNode> e = ((GroupTreeNode) m_node.getRoot()).preorderEnumeration();
            GroupTreeNode node;
            while (e.hasMoreElements()) {
                node = e.nextElement();
                if (!node.getGroup().supportsRemove())
                    continue;
                for (int i = 0; i < entries.length; ++i) {
                    if (node.getGroup().contains(entries[i]))
                        removeGroupsNodes.add(node);
                }
            }
            // warning for all groups from which the entries are removed, and 
            // for the one to which they are added! hence the magical +1
            AbstractGroup[] groups = new AbstractGroup[removeGroupsNodes.size()+1];
            for (int i = 0; i < removeGroupsNodes.size(); ++i)
                groups[i] = removeGroupsNodes.elementAt(i).getGroup();
            groups[groups.length-1] = m_node.getGroup();
            if (!Util.warnAssignmentSideEffects(groups,
                    entries, m_panel.getDatabase(), m_panel.frame()))
                return; // user aborted operation
        } else {
            // warn if assignment has undesired side effects (modifies a field != keywords)
            if (!Util.warnAssignmentSideEffects(new AbstractGroup[]{m_node.getGroup()},
                    entries, m_panel.getDatabase(), m_panel.frame()))
                return; // user aborted operation
        }
        
        // if an editor is showing, its fields must be updated
        // after the assignment, and before that, the current
        // edit has to be stored:
        m_panel.storeCurrentEdit();
        
        NamedCompound undoAll = new NamedCompound(Globals.lang("change assignment of entries")); 
        
        if (m_move) {
            // first remove
            for (int i = 0; i < removeGroupsNodes.size(); ++i) {
                GroupTreeNode node = removeGroupsNodes.elementAt(i);
                if (node.getGroup().containsAny(entries))
                    undoAll.addEdit(node.removeFromGroup(entries));
            }
            // then add
            AbstractUndoableEdit undoAdd = m_node.addToGroup(entries);
            if (undoAdd != null)
                undoAll.addEdit(undoAdd);
        } else {
            AbstractUndoableEdit undoAdd = m_node.addToGroup(entries);
            if (undoAdd == null)
                return; // no changed made
            undoAll.addEdit(undoAdd);
        }
        
        undoAll.end();
        
        m_panel.undoManager.addEdit(undoAll);
        m_panel.markBaseChanged();
        m_panel.updateEntryEditorIfShowing();
        m_panel.getGroupSelector().valueChanged(null);
    }
}
