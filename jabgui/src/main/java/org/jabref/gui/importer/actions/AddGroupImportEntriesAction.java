package org.jabref.gui.importer.actions;

import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

/// Creates the group that imported entries are collected in, at position 0 under the root.
///
/// This runs when entries are imported, not when a library is opened: a library the user only
/// looked at is not written to, and the write that does happen belongs to an import the user
/// asked for.
public class AddGroupImportEntriesAction {

    /// @return whether the group was added
    public boolean addImportedEntriesGroupIfNeeded(BibDatabaseContext databaseContext, CliPreferences preferences) {
        if (!preferences.getLibraryPreferences().shouldAddImportedEntries()) {
            return false;
        }

        String groupName = preferences.getLibraryPreferences().getAddImportedEntriesGroupName();
        MetaData metaData = databaseContext.getMetaData();
        boolean groupMissing = metaData.getGroups()
                                       .map(root -> root.getChildren().stream()
                                                        .map(GroupTreeNode::getGroup)
                                                        .noneMatch(grp -> grp instanceof ExplicitGroup && grp.getName().equalsIgnoreCase(groupName)))
                                       .orElse(true);

        if (!groupMissing) {
            return false;
        }

        char keywordSeparator = metaData.getKeywordSeparator().orElse(preferences.getBibEntryPreferences().getKeywordSeparator());
        GroupTreeNode root = metaData.getGroups().orElseGet(() -> {
            GroupTreeNode newRoot = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
            metaData.setGroups(newRoot);
            return newRoot;
        });

        AbstractGroup importEntriesGroup = new ExplicitGroup(groupName, GroupHierarchyType.INDEPENDENT, keywordSeparator);
        GroupTreeNode newSubgroup = root.addSubgroup(importEntriesGroup);
        newSubgroup.moveTo(root, 0);
        // The tree was edited in place, so the root is written back to report the write: that is
        // what marks the library and what the group panel refreshes on.
        metaData.setGroups(root);
        return true;
    }
}
