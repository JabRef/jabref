package org.jabref.logic.groups;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
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

        // We deliberately do NOT use root.findOrCreateExplicitGroup(...) here. That method attaches
        // a freshly created group to the tree before returning it, so the only place left to assign
        // the entries is *after* the node is attached. Attaching the node is what creates the GUI's
        // GroupNodeViewModel, whose constructor immediately (re)computes its membership in a
        // background task. Assigning afterwards races that task: the stale (empty) result can
        // overwrite the correct membership, so the entries only show up in the group after switching
        // tabs back and forth. We therefore split find and create so that, for a new group, the
        // entries are assigned BEFORE the node is attached.
        Optional<GroupTreeNode> existing = root.findGroupByName(groupName);

        if (existing.isPresent()) {
            // The group (and thus its GroupNodeViewModel) already exists. Assigning sets the
            // entries' group field, which the existing view model picks up live via its
            // database-change listener.
            existing.get().addEntriesToGroup(entries);
        } else {
            GroupTreeNode newGroup = GroupTreeNode.fromGroup(new ExplicitGroup(groupName, GroupHierarchyType.INDEPENDENT, keywordSeparator));
            newGroup.addEntriesToGroup(entries);
            root.addChild(newGroup);
        }
    }
}
