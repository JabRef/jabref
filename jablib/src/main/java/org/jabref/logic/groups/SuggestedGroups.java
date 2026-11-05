package org.jabref.logic.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;

import org.jspecify.annotations.NullMarked;

/// The groups offered by "Add JabRef suggested groups", and how they are merged into the group tree of a library.
///
/// A suggested group is recognized by its position in the tree and by its search expression, never by its name:
/// the name is translated at the moment the group is created and the user may rename it, while the search expression
/// is what makes the group what it is.
/// A suggested group whose expression the user changed is therefore treated as the user's own group, and the suggestion is added again.
@NullMarked
public final class SuggestedGroups {

    private SuggestedGroups() {
    }

    /// Adds the suggested groups that are missing below `allEntries`.
    /// Suggested groups that are already present stay as they are, including the user's changes to their name, icon, or color.
    ///
    /// A missing group is inserted before the next suggested group that is present, so that a restored group returns to its place.
    ///
    /// @return the added nodes, in the order of the suggestions
    public static List<GroupTreeNode> addMissing(GroupTreeNode allEntries) {
        List<GroupTreeNode> added = new ArrayList<>();
        for (MissingSuggestion missing : findMissing(createSuggestions(), allEntries)) {
            int index = insertionIndex(missing.parent(), missing.followingSuggestions());
            added.add(missing.parent().addChild(missing.suggestion().copySubtree(), index));
        }
        return added;
    }

    /// Tells whether every suggested group is present below `allEntries`.
    public static boolean areAllPresent(GroupTreeNode allEntries) {
        return findMissing(createSuggestions(), allEntries).isEmpty();
    }

    /// Creates the suggested groups below a stand-in for the "All entries" root.
    static GroupTreeNode createSuggestions() {
        GroupTreeNode allEntries = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        allEntries.addSubgroup(GroupsFactory.createWithoutFilesGroup());
        allEntries.addSubgroup(GroupsFactory.createWithoutGroupsGroup());
        return allEntries;
    }

    /// Walks the suggestions and the tree side by side.
    /// When a suggested group is missing, its whole subtree is reported once and not walked further.
    private static List<MissingSuggestion> findMissing(GroupTreeNode suggestions, GroupTreeNode target) {
        List<MissingSuggestion> missing = new ArrayList<>();
        List<GroupTreeNode> suggestedChildren = suggestions.getChildren();
        for (int i = 0; i < suggestedChildren.size(); i++) {
            GroupTreeNode suggestion = suggestedChildren.get(i);
            List<GroupTreeNode> followingSuggestions = suggestedChildren.subList(i + 1, suggestedChildren.size());
            findPresent(target, suggestion).ifPresentOrElse(
                    present -> missing.addAll(findMissing(suggestion, present)),
                    () -> missing.add(new MissingSuggestion(target, suggestion, followingSuggestions)));
        }
        return missing;
    }

    private static int insertionIndex(GroupTreeNode parent, List<GroupTreeNode> followingSuggestions) {
        return followingSuggestions.stream()
                                   .map(following -> findPresent(parent, following))
                                   .flatMap(Optional::stream)
                                   .findFirst()
                                   .map(GroupTreeNode::getPositionInParent)
                                   .orElse(parent.getNumberOfChildren());
    }

    // [impl->req~ux.groups.suggested-groups-recognized-by-expression~1]
    private static Optional<GroupTreeNode> findPresent(GroupTreeNode parent, GroupTreeNode suggestion) {
        return parent.getChildren().stream()
                     .filter(child -> isSameSuggestion(child.getGroup(), suggestion.getGroup()))
                     .findFirst();
    }

    private static boolean isSameSuggestion(AbstractGroup group, AbstractGroup suggestion) {
        return group instanceof SearchGroup searchGroup
                && suggestion instanceof SearchGroup suggestedSearchGroup
                && searchGroup.getSearchExpression().equals(suggestedSearchGroup.getSearchExpression())
                && searchGroup.getSearchFlags().equals(suggestedSearchGroup.getSearchFlags());
    }

    private record MissingSuggestion(GroupTreeNode parent, GroupTreeNode suggestion, List<GroupTreeNode> followingSuggestions) {
    }
}
