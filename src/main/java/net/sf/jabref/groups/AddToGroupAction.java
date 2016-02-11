/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;
import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.gui.undo.NamedCompound;

public class AddToGroupAction extends AbstractAction {

    private GroupTreeNode mNode;
    private final boolean m_move;
    private BasePanel mPanel;


    /**
     * @param move If true, remove node from all other groups.
     */
    public AddToGroupAction(GroupTreeNode node, boolean move,
            BasePanel panel) {
        super(node.getGroup().getName());
        mNode = node;
        m_move = move;
        mPanel = panel;
    }

    public AddToGroupAction(boolean move) {
        super(move ? Localization.lang("Assign entry selection exclusively to this group")
                : Localization.lang("Add entry selection to this group"));
        m_move = move;
    }

    public void setBasePanel(BasePanel panel) {
        mPanel = panel;
    }

    public void setNode(GroupTreeNode node) {
        mNode = node;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        final List<BibEntry> entries = mPanel.getSelectedEntries();
        final Vector<GroupTreeNode> removeGroupsNodes = new Vector<>(); // used only when moving

        if (m_move) {
            // collect warnings for removal
            Enumeration<GroupTreeNode> e = ((GroupTreeNode) mNode.getRoot()).preorderEnumeration();
            for (GroupTreeNode node : Collections.list(e)) {
                if (!node.getGroup().supportsRemove()) {
                    continue;
                }
                for (BibEntry entry : entries) {
                    if (node.getGroup().contains(entry)) {
                        removeGroupsNodes.add(node);
                    }
                }
            }
            // warning for all groups from which the entries are removed, and
            // for the one to which they are added! hence the magical +1
            List<AbstractGroup> groups = new ArrayList<>(removeGroupsNodes.size() + 1);
            for (int i = 0; i < removeGroupsNodes.size(); ++i) {
                groups.add(removeGroupsNodes.elementAt(i).getGroup());
            }
            groups.add(mNode.getGroup());
            if (!Util.warnAssignmentSideEffects(groups, mPanel.frame())) {
                return; // user aborted operation
            }
        } else {
            // warn if assignment has undesired side effects (modifies a field != keywords)
            if (!Util.warnAssignmentSideEffects(Arrays.asList(mNode.getGroup()), mPanel.frame())) {
                return; // user aborted operation
            }
        }

        // if an editor is showing, its fields must be updated
        // after the assignment, and before that, the current
        // edit has to be stored:
        mPanel.storeCurrentEdit();

        NamedCompound undoAll = new NamedCompound(Localization.lang("change assignment of entries"));

        if (m_move) {
            // first remove
            for (int i = 0; i < removeGroupsNodes.size(); ++i) {
                GroupTreeNode node = removeGroupsNodes.elementAt(i);
                if (node.getGroup().containsAny(entries)) {
                    AbstractUndoableEdit undoRemove = node.removeFromGroup(entries);
                    if (undoRemove != null) {
                        undoAll.addEdit(undoRemove);
                    }
                }
            }
            // then add
            AbstractUndoableEdit undoAdd = mNode.addToGroup(entries);
            if (undoAdd != null) {
                undoAll.addEdit(undoAdd);
            }
        } else {
            AbstractUndoableEdit undoAdd = mNode.addToGroup(entries);
            if (undoAdd == null)
            {
                return; // no changed made
            }
            undoAll.addEdit(undoAdd);
        }

        undoAll.end();

        mPanel.undoManager.addEdit(undoAll);
        mPanel.markBaseChanged();
        mPanel.updateEntryEditorIfShowing();
        mPanel.getGroupSelector().valueChanged(null);
    }
}
