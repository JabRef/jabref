package org.jabref.gui.groups;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import org.jabref.gui.BasePanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;

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
        if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(mNode.getNode().getGroup(), mPanel.frame())) {
            return; // user aborted operation
        }

        List<FieldChange> undo = mNode.removeEntriesFromGroup(mPanel.getSelectedEntries());
        if (undo.isEmpty()) {
            return; // no changed made
        }

        mPanel.getUndoManager().addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(mNode, undo));
        mPanel.markBaseChanged();
        mPanel.updateEntryEditorIfShowing();
        mPanel.getGroupSelector().valueChanged(null);
    }
}
