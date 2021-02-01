package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;

import org.jabref.gui.groups.GroupTreeNodeViewModel;
import org.jabref.gui.groups.UndoableModifySubtree;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.bibtex.comparator.GroupDiff;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.GroupTreeNode;

class GroupChangeViewModel extends DatabaseChangeViewModel {

    private final GroupTreeNode changedGroups;

    public GroupChangeViewModel(GroupDiff diff) {
        super(diff.getOriginalGroupRoot() == null ? Localization.lang("Removed all groups") : Localization
                .lang("Modified groups tree"));
        this.changedGroups = diff.getNewGroupRoot();
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        GroupTreeNode root = database.getMetaData().getGroups().orElseGet(() -> {
            GroupTreeNode groupTreeNode = new GroupTreeNode(DefaultGroupsFactory.getAllEntriesGroup());
            database.getMetaData().setGroups(groupTreeNode);
            return groupTreeNode;
        });

        final UndoableModifySubtree undo = new UndoableModifySubtree(
                new GroupTreeNodeViewModel(database.getMetaData().getGroups().orElse(null)),
                new GroupTreeNodeViewModel(root), Localization.lang("Modified groups"));
        root.removeAllChildren();
        if (changedGroups == null) {
            // I think setting root to null is not possible
            root.setGroup(DefaultGroupsFactory.getAllEntriesGroup(), false, false, null);
        } else {
            // change root group, even though it'll be AllEntries anyway
            root.setGroup(changedGroups.getGroup(), false, false, null);
            for (GroupTreeNode child : changedGroups.getChildren()) {
                child.copySubtree().moveTo(root);
            }
        }

        undoEdit.addEdit(undo);
    }

    @Override
    public Node description() {
        return new Label(toString() + '.'
                + (changedGroups == null ? "" : ' ' + Localization
                .lang("Accepting the change replaces the complete groups tree with the externally modified groups tree.")));
    }
}
