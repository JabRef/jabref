package net.sf.jabref.gui.groups;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.EntriesGroupChange;
import net.sf.jabref.model.groups.GroupTreeNode;

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

    public void moveToGroup(List<BibEntry> entries, NamedCompound undoAll) {
        List<GroupTreeNode> groupsContainingEntries =
                node.getNode().getRoot().getContainingGroups(entries, false).stream().filter(node -> node.getGroup().supportsRemove()).collect(
                        Collectors.toList());

        List<AbstractGroup> affectedGroups = groupsContainingEntries.stream().map(GroupTreeNode::getGroup).collect(
                Collectors.toList());
        affectedGroups.add(node.getNode().getGroup());
        if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(affectedGroups, panel.frame())) {
            return; // user aborted operation
        }

        // first remove
        for (GroupTreeNode group : groupsContainingEntries) {
            Optional<EntriesGroupChange> undoRemove = group.getGroup().remove(entries);
            if (undoRemove.isPresent()) {
                undoAll.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(node, undoRemove.get()));
            }
        }

        // then add
        Optional<EntriesGroupChange> undoAdd = node.addEntriesToGroup(entries);
        if (undoAdd.isPresent()) {
            undoAll.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(node, undoAdd.get()));
        }
    }

    public void addToGroup(List<BibEntry> entries, NamedCompound undo) {
        if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(node.getNode().getGroup(), panel.frame())) {
            return; // user aborted operation
        }

        Optional<EntriesGroupChange> undoAdd = node.addEntriesToGroup(entries);
        if (undoAdd.isPresent()) {
            undo.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(node, undoAdd.get()));
        }
    }

}
