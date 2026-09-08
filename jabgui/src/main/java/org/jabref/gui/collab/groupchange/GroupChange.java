package org.jabref.gui.collab.groupchange;

import java.util.Optional;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.logic.bibtex.comparator.GroupDiff;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableGroupTreeChange;

public final class GroupChange extends DatabaseChange {
    private final GroupDiff groupDiff;

    public GroupChange(GroupDiff groupDiff, BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.groupDiff = groupDiff;
        setChangeName(groupDiff.getOriginalGroupRoot() == null ? Localization.lang("Removed all groups")
                                                               : Localization.lang("Modified groups tree"));
    }

    @Override
    public void applyChange(CompoundEdit undoEdit) {
        GroupTreeNode newRoot = groupDiff.getNewGroupRoot();

        // Before the root below is installed: a library that had no groups has to end up with none
        // again when this change is undone, not with the empty root accepting it created.
        Optional<GroupTreeNode> before = databaseContext.getMetaData().getGroups().map(GroupTreeNode::copySubtree);

        GroupTreeNode root = databaseContext.getMetaData().getGroups().orElseGet(() -> {
            GroupTreeNode groupTreeNode = new GroupTreeNode(GroupsFactory.createAllEntriesGroup());
            databaseContext.getMetaData().setGroups(groupTreeNode);
            return groupTreeNode;
        });

        root.removeAllChildren();
        if (newRoot == null) {
            // I think setting root to null is not possible
            root.setGroup(GroupsFactory.createAllEntriesGroup(), false, false, null);
        } else {
            // change root group, even though it'll be AllEntries anyway
            root.setGroup(newRoot.getGroup(), false, false, null);
            for (GroupTreeNode child : newRoot.getChildren()) {
                child.copySubtree().moveTo(root);
            }
        }
        // Recorded as the whole tree, like every other group operation: a record that holds nodes
        // is undone silently once a later operation installs a fresh tree, and those nodes are then
        // no longer the ones the library holds.
        undoEdit.addEdit(new UndoableGroupTreeChange(
                databaseContext.getMetaData(), before, databaseContext.getMetaData().getGroups()));
    }

    public GroupDiff getGroupDiff() {
        return groupDiff;
    }
}
