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

    public void addImportedEntriesGroupIfNeeded(BibDatabaseContext databaseContext, CliPreferences preferences) {
        if (!preferences.getLibraryPreferences().shouldAddImportedEntries()) {
            return;
        }

        String groupName = preferences.getLibraryPreferences().getAddImportedEntriesGroupName();
        MetaData metaData = databaseContext.getMetaData();
        boolean groupMissing = metaData.getGroups()
                                       .map(root -> root.getChildren().stream()
                                                        .map(GroupTreeNode::getGroup)
                                                        .noneMatch(grp -> grp instanceof ExplicitGroup && grp.getName().equalsIgnoreCase(groupName)))
                                       .orElse(true);

        if (!groupMissing) {
            return;
        }

        char keywordSeparator = preferences.getBibEntryPreferences().getKeywordSeparator();
        GroupTreeNode root = metaData.getGroups().orElseGet(() -> {
            GroupTreeNode newRoot = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
            metaData.setGroups(newRoot);
            return newRoot;
        });

        AbstractGroup importEntriesGroup = new ExplicitGroup(groupName, GroupHierarchyType.INDEPENDENT, keywordSeparator);
        GroupTreeNode newSubgroup = root.addSubgroup(importEntriesGroup);
        newSubgroup.moveTo(root, 0);
    }

    @Override
    public boolean isActionNecessary(ParserResult pr, DialogService dialogService, CliPreferences preferences) {
        return preferences.getLibraryPreferences().shouldAddImportedEntries();
    }

    /// Creates the "Imported entries" group at position 0 under the root.
    /// Selection is omitted to prevent focus theft when switching tabs.
    @Override
    public void performAction(ParserResult pr, DialogService dialogService, CliPreferences preferences) {
        addImportedEntriesGroupIfNeeded(pr.getDatabaseContext(), preferences);
    }
}
