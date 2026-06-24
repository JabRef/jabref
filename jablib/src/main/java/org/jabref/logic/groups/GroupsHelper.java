package org.jabref.logic.groups;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class GroupsHelper {

    private GroupsHelper() {
    }

    /// Assigns `entries` to the explicit group named `groupName` in `databaseContext`,
    /// creating the group as a top-level explicit group (and the group-tree root) if it does not exist yet.
    ///
    /// @param keywordSeparator separator used when the group is created as a keyword-backed explicit group
    public static void assignEntriesToGroup(BibDatabaseContext databaseContext, List<BibEntry> entries, String groupName, Character keywordSeparator) {
        GroupTreeNode root = databaseContext.getMetaData().getGroups().orElseGet(() -> {
            GroupTreeNode newRoot = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
            databaseContext.getMetaData().setGroups(newRoot);
            return newRoot;
        });
        root.findOrCreateExplicitGroup(groupName, keywordSeparator)
            .addEntriesToGroup(entries);
    }
}
