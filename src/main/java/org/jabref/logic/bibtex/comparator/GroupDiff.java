package org.jabref.logic.bibtex.comparator;

import java.util.Optional;

import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

public class GroupDiff {
    private final GroupTreeNode originalGroupRoot;
    private final GroupTreeNode newGroupRoot;

    GroupDiff(GroupTreeNode originalGroupRoot, GroupTreeNode newGroupRoot) {
        this.originalGroupRoot = originalGroupRoot;
        this.newGroupRoot = newGroupRoot;
    }

    /**
     * This method only detects whether a change took place or not. It does not determine the type of change. This would
     * be possible, but difficult to do properly, so we rather only report the change.
     */
    public static Optional<GroupDiff> compare(MetaData originalMetaData, MetaData newMetaData) {
        final Optional<GroupTreeNode> originalGroups = originalMetaData.getGroups();
        final Optional<GroupTreeNode> newGroups = newMetaData.getGroups();

        if (!originalGroups.equals(newGroups)) {
            return Optional.of(new GroupDiff(newGroups.orElse(null), originalGroups.orElse(null)));
        } else {
            return Optional.empty();
        }
    }

    public GroupTreeNode getOriginalGroupRoot() {
        return originalGroupRoot;
    }

    public GroupTreeNode getNewGroupRoot() {
        return newGroupRoot;
    }
}
