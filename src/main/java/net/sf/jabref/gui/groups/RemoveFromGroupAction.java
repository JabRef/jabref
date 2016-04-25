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
package net.sf.jabref.gui.groups;

import java.awt.event.ActionEvent;
import java.util.Optional;

import javax.swing.AbstractAction;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.groups.EntriesGroupChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

public class RemoveFromGroupAction extends AbstractAction {

    private GroupTreeNodeViewModel mNode;
    private BasePanel mPanel;


    public RemoveFromGroupAction(GroupTreeNodeViewModel node, BasePanel panel) {
        super(node.getNode().getGroup().getName());
        mNode = node;
        mPanel = panel;
    }

    public RemoveFromGroupAction() {
        super(Localization.lang("Remove entry selection from this group"));
    }

    public void setNode(GroupTreeNodeViewModel node) {
        mNode = node;
    }

    public void setBasePanel(BasePanel panel) {
        mPanel = panel;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        // warn if assignment has undesired side effects (modifies a field != keywords)
        if (!Util.warnAssignmentSideEffects(mNode.getNode().getGroup(), mPanel.frame())) {
            return; // user aborted operation
        }

        Optional<EntriesGroupChange> undo = mNode.removeEntriesFromGroup(mPanel.getSelectedEntries());
        if (! undo.isPresent()) {
            return; // no changed made
        }

        mPanel.undoManager.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(mNode, undo.get()));
        mPanel.markBaseChanged();
        mPanel.updateEntryEditorIfShowing();
        mPanel.getGroupSelector().valueChanged(null);
    }
}
