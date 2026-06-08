package org.jabref.logic.bibtex.comparator;

import java.util.Optional;

import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.Nullable;

public class GroupDiff {
    @Nullable private final GroupTreeNode originalGroupRoot;
    @Nullable private final GroupTreeNode newGroupRoot;

    GroupDiff(@Nullable GroupTreeNode originalGroupRoot, @Nullable GroupTreeNode newGroupRoot) {
        this.originalGroupRoot = originalGroupRoot;
        this.newGroupRoot = newGroupRoot;
    }

    /// This method only detects whether a change took place or not. It does not determine the type of change. This would
    /// be possible, but difficult to do properly, so we rather only report the change.
    public static Optional<GroupDiff> compare(MetaData originalMetaData, MetaData newMetaData) {
        final Optional<GroupTreeNode> originalGroups = originalMetaData.getGroups();
        final Optional<GroupTreeNode> newGroups = newMetaData.getGroups();

        if (!originalGroups.equals(newGroups)) {
            return Optional.of(new GroupDiff(originalGroups.orElse(null), newGroups.orElse(null)));
        } else {
            return Optional.empty();
        }
    }

    public Optional<GroupTreeNode> getOriginalGroupRoot() {
        return Optional.ofNullable(originalGroupRoot);
    }

    public Optional<GroupTreeNode> getNewGroupRoot() {
        return Optional.ofNullable(newGroupRoot);
    }
}
