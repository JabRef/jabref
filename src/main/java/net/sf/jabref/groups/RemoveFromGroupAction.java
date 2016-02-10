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
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

public class RemoveFromGroupAction extends AbstractAction {

    private GroupTreeNode mNode;
    private BasePanel mPanel;


    public RemoveFromGroupAction(GroupTreeNode node, BasePanel panel) {
        super(node.getGroup().getName());
        mNode = node;
        mPanel = panel;
    }

    public RemoveFromGroupAction() {
        super(Localization.lang("Remove entry selection from this group"));
    }

    public void setNode(GroupTreeNode node) {
        mNode = node;
    }

    public void setBasePanel(BasePanel panel) {
        mPanel = panel;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        // warn if assignment has undesired side effects (modifies a field != keywords)
        if (!Util.warnAssignmentSideEffects(Arrays.asList(mNode.getGroup()), mPanel.frame())) {
            return; // user aborted operation
        }

        AbstractUndoableEdit undo = mNode.removeFromGroup(mPanel.getSelectedEntries());
        if (undo == null) {
            return; // no changed made
        }

        mPanel.undoManager.addEdit(undo);
        mPanel.markBaseChanged();
        mPanel.updateEntryEditorIfShowing();
        mPanel.getGroupSelector().valueChanged(null);
    }
}
