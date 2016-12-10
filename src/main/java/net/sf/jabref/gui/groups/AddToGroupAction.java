package net.sf.jabref.gui.groups;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.GroupEntryChanger;
import net.sf.jabref.model.groups.GroupTreeNode;

/**
 * TODO: rework code and try to reuse some from {@link GroupTreeNode#setGroup(AbstractGroup, boolean, boolean, List)}.
 */
public class AddToGroupAction extends AbstractAction {

    private final boolean move;
    private GroupTreeNodeViewModel node;
    private BasePanel panel;

    /**
     * @param move If true, remove entries from all other groups.
     */
    public AddToGroupAction(GroupTreeNodeViewModel node, boolean move, BasePanel panel) {
        super(node.getNode().getGroup().getName());
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

    public void setNode(GroupTreeNodeViewModel node) {
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

        panel.getUndoManager().addEdit(undoAll);
        panel.markBaseChanged();
        panel.updateEntryEditorIfShowing();
        panel.getGroupSelector().valueChanged(null);
    }

    private void moveToGroup(List<BibEntry> entries, NamedCompound undoAll) {
        List<AbstractGroup> affectedGroups =
                node.getNode().getRoot().getContainingGroups(entries, false).stream()
                        .map(GroupTreeNode::getGroup)
                        .filter(group -> group instanceof GroupEntryChanger)
                        .collect(Collectors.toList());
        affectedGroups.add(node.getNode().getGroup());
        if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(affectedGroups, panel.frame())) {
            return; // user aborted operation
        }

        // first remove
        for (AbstractGroup group : affectedGroups) {
            GroupEntryChanger entryChanger = (GroupEntryChanger)group;
            List<FieldChange> changes = entryChanger.remove(entries);
            if (!changes.isEmpty()) {
                undoAll.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(node, changes));
            }
        }

        // then add
        List<FieldChange> undoAdd = node.addEntriesToGroup(entries);
        if (!undoAdd.isEmpty()) {
            undoAll.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(node, undoAdd));
        }
    }

    private void addToGroup(List<BibEntry> entries, NamedCompound undo) {
        if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(node.getNode().getGroup(), panel.frame())) {
            return; // user aborted operation
        }

        List<FieldChange> undoAdd = node.addEntriesToGroup(entries);
        if (!undoAdd.isEmpty()) {
            undo.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(node, undoAdd));
        }
    }

}
