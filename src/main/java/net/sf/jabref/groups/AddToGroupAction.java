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
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;
import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.gui.undo.NamedCompound;

public class AddToGroupAction extends AbstractAction {

    private final boolean move;
    private GroupTreeNode node;
    private BasePanel panel;

    /**
     * @param move If true, remove entries from all other groups.
     */
    public AddToGroupAction(GroupTreeNode node, boolean move, BasePanel panel) {
        super(node.getGroup().getName());
        this.node = node;
        this.move = move;
        this.panel = panel;
    }

    public AddToGroupAction(boolean move) {
        super(move ? Localization.lang("Assign entry selection exclusively to this group") :
                Localization.lang("Add entry selection to this group"));
        this.move = move;
    }

    public void setBasePanel(BasePanel panel) {
        this.panel = panel;
    }

    public void setNode(GroupTreeNode node) {
        this.node = node;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        final List<BibEntry> entries = panel.getSelectedEntries();

        // if an editor is showing, its fields must be updated after the assignment,
        // and before that, the current edit has to be stored:
        panel.storeCurrentEdit();

        NamedCompound undoAll = new NamedCompound(Localization.lang("change assignment of entries"));

        if (move) {
            moveToGroup(entries, undoAll);
        } else {
            addToGroup(entries, undoAll);
        }

        undoAll.end();

        panel.undoManager.addEdit(undoAll);
        panel.markBaseChanged();
        panel.updateEntryEditorIfShowing();
        panel.getGroupSelector().valueChanged(null);
    }

    public void moveToGroup(List<BibEntry> entries, NamedCompound undoAll) {
        List<GroupTreeNode> groupsContainingEntries =
                ((GroupTreeNode) node.getRoot()).getParentGroupsSupportingRemoval(entries);

        List<AbstractGroup> affectedGroups = groupsContainingEntries.stream().map(node -> node.getGroup()).collect(
                Collectors.toList());
        affectedGroups.add(node.getGroup());
        if (!Util.warnAssignmentSideEffects(affectedGroups, panel.frame())) {
            return; // user aborted operation
        }

        // first remove
        for (GroupTreeNode group : groupsContainingEntries) {
            AbstractUndoableEdit undoRemove = group.removeFromGroup(entries);
            if (undoRemove != null) {
                undoAll.addEdit(undoRemove);
            }
        }

        // then add
        AbstractUndoableEdit undoAdd = node.addToGroup(entries);
        if (undoAdd != null) {
            undoAll.addEdit(undoAdd);
        }
    }

    public void addToGroup(List<BibEntry> entries, NamedCompound undo) {
        if (!Util.warnAssignmentSideEffects(node.getGroup(), panel.frame())) {
            return; // user aborted operation
        }

        AbstractUndoableEdit undoAdd = node.addToGroup(entries);
        if (undoAdd != null) {
            undo.addEdit(undoAdd);
        }
    }

}
