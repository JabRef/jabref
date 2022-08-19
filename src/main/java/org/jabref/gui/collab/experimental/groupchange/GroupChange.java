package org.jabref.gui.collab.experimental.groupchange;

import org.jabref.gui.collab.experimental.ExternalChange;
import org.jabref.gui.collab.experimental.ExternalChangeResolverFactory;
import org.jabref.gui.groups.GroupTreeNodeViewModel;
import org.jabref.gui.groups.UndoableModifySubtree;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.bibtex.comparator.GroupDiff;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.GroupTreeNode;

public final class GroupChange extends ExternalChange {
    private final GroupDiff groupDiff;

    public GroupChange(GroupDiff groupDiff, BibDatabaseContext databaseContext, ExternalChangeResolverFactory externalChangeResolverFactory) {
        super(databaseContext, externalChangeResolverFactory);
        this.groupDiff = groupDiff;
        setChangeName(groupDiff.getOriginalGroupRoot() == null ? Localization.lang("Removed all groups") : Localization
                .lang("Modified groups tree"));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        GroupTreeNode oldRoot = groupDiff.getOriginalGroupRoot();
        GroupTreeNode newRoot = groupDiff.getNewGroupRoot();

        GroupTreeNode root = databaseContext.getMetaData().getGroups().orElseGet(() -> {
            GroupTreeNode groupTreeNode = new GroupTreeNode(DefaultGroupsFactory.getAllEntriesGroup());
            databaseContext.getMetaData().setGroups(groupTreeNode);
            return groupTreeNode;
        });

        final UndoableModifySubtree undo = new UndoableModifySubtree(
                new GroupTreeNodeViewModel(databaseContext.getMetaData().getGroups().orElse(null)),
                new GroupTreeNodeViewModel(root), Localization.lang("Modified groups"));
        root.removeAllChildren();
        if (newRoot == null) {
            // I think setting root to null is not possible
            root.setGroup(DefaultGroupsFactory.getAllEntriesGroup(), false, false, null);
        } else {
            // change root group, even though it'll be AllEntries anyway
            root.setGroup(newRoot.getGroup(), false, false, null);
            for (GroupTreeNode child : newRoot.getChildren()) {
                child.copySubtree().moveTo(root);
            }
        }

        undoEdit.addEdit(undo);
    }

    public GroupDiff getGroupDiff() {
        return groupDiff;
    }
}
