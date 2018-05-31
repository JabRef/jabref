package org.jabref.gui.collab;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jabref.gui.BasePanel;
import org.jabref.gui.groups.GroupTreeNodeViewModel;
import org.jabref.gui.groups.UndoableModifySubtree;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.bibtex.comparator.GroupDiff;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.groups.GroupTreeNode;

class GroupChangeViewModel extends ChangeViewModel {

    private final GroupTreeNode changedGroups;
    private final GroupTreeNode tmpGroupRoot;


    public GroupChangeViewModel(GroupDiff diff) {
        super(diff.getOriginalGroupRoot() == null ? Localization.lang("Removed all groups") : Localization
                .lang("Modified groups tree"));
        this.changedGroups = diff.getOriginalGroupRoot();
        this.tmpGroupRoot = diff.getNewGroupRoot();
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        GroupTreeNode root = panel.getBibDatabaseContext().getMetaData().getGroups().orElse(null);
        if (root == null) {
            root = new GroupTreeNode(DefaultGroupsFactory.getAllEntriesGroup());
            panel.getBibDatabaseContext().getMetaData().setGroups(root);
        }
        final UndoableModifySubtree undo = new UndoableModifySubtree(
                new GroupTreeNodeViewModel(panel.getBibDatabaseContext().getMetaData().getGroups().orElse(null)),
                new GroupTreeNodeViewModel(root), Localization.lang("Modified groups"));
        root.removeAllChildren();
        if (changedGroups == null) {
            // I think setting root to null is not possible
            root.setGroup(DefaultGroupsFactory.getAllEntriesGroup());
        } else {
            // change root group, even though it'll be AllEntries anyway
            root.setGroup(changedGroups.getGroup());
            for (GroupTreeNode child : changedGroups.getChildren()) {
                child.copySubtree().moveTo(root);
            }
        }

        undoEdit.addEdit(undo);

        // Update tmp database:
        if (tmpGroupRoot != null) {
            tmpGroupRoot.removeAllChildren();
            if (changedGroups != null) {
                GroupTreeNode copied = changedGroups.copySubtree();
                tmpGroupRoot.setGroup(copied.getGroup());
                for (GroupTreeNode child : copied.getChildren()) {
                    child.copySubtree().moveTo(tmpGroupRoot);
                }
            }
        }

        return true;
    }

    @Override
    public JComponent description() {
        return new JLabel("<html>" + toString() + '.'
                + (changedGroups == null ? "" : ' ' + Localization
                        .lang("Accepting the change replaces the complete groups tree with the externally modified groups tree."))
                + "</html>");

    }
}
