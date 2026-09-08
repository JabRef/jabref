package org.jabref.gui.importer.actions;

import org.jabref.gui.DialogService;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

public class AddGroupImportEntriesAction implements GUIPostOpenAction {

    /// @return whether the group was added, so that the caller can mark the library — this runs
    ///         before the tab is attached to the library, and nothing else reports the write
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
        return true;
    }

    @Override
    public boolean isActionNecessary(ParserResult pr, DialogService dialogService, CliPreferences preferences) {
        return preferences.getLibraryPreferences().shouldAddImportedEntries();
    }

    /// Creates the "Imported entries" group at position 0 under the root.
    /// Selection is omitted to prevent focus theft when switching tabs.
    @Override
    public void performAction(ParserResult pr, DialogService dialogService, CliPreferences preferences) {
        if (addImportedEntriesGroupIfNeeded(pr.getDatabaseContext(), preferences)) {
            // The library on disk has no such group, so it needs saving. Said here rather than
            // left to a listener: post-open actions run before the tab is attached to the
            // library, so nothing is listening yet.
            pr.setChangedOnMigration(true);
        }
    }
}
