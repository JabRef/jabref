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
/// A suggested group is recognized by its parent in the tree and by its search criteria (search expression, search flags,
/// and hierarchy), never by its name: the name is translated at the moment the group is created and the user may rename it,
/// while the search criteria are what make the group what it is.
/// A suggested group whose search criteria the user changed is therefore treated as the user's own group, and the suggestion is added again.
@NullMarked
public final class SuggestedGroups {

    private SuggestedGroups() {
    }

    /// Adds the suggested groups that are missing below `allEntries`.
    /// Suggested groups that are already present stay as they are, including the user's changes to their name, icon, or color.
    ///
    /// A missing group is inserted before the next suggested group that is present, or as last child if there is none.
    /// Groups that are already present are not moved.
    /// A missing group whose name another group of the library already has gets a number appended to its name.
    ///
    /// @return the roots of the added subtrees, in the order of the suggestions
    public static List<GroupTreeNode> addMissing(GroupTreeNode allEntries) {
        List<GroupTreeNode> added = new ArrayList<>();
        for (MissingSuggestion missing : findMissing(createSuggestions(), allEntries)) {
            int index = insertionIndex(missing.parent(), missing.followingSuggestions());
            GroupTreeNode suggestion = missing.suggestion().copySubtree();
            suggestion.iterateOverTree().forEach(node -> useFreeName(allEntries, node));
            added.add(missing.parent().addChild(suggestion, index));
        }
        return added;
    }

    /// Tells whether every suggested group is present below `allEntries`.
    public static boolean areAllPresent(GroupTreeNode allEntries) {
        return findMissing(createSuggestions(), allEntries).isEmpty();
    }

    /// Tells whether the tree below `allEntries` contains a search group other than a suggested group, wherever it is placed.
    /// The suggested groups use the current search syntax; another search group of a library without a search syntax version
    /// may still use an older one.
    public static boolean containsOtherSearchGroups(GroupTreeNode allEntries) {
        List<AbstractGroup> suggestions = createSuggestions().iterateOverTree().map(GroupTreeNode::getGroup).toList();
        return allEntries.iterateOverTree()
                         .map(GroupTreeNode::getGroup)
                         .filter(SearchGroup.class::isInstance)
                         .anyMatch(group -> suggestions.stream().noneMatch(suggestion -> isSameSuggestion(group, suggestion)));
    }

    /// Creates the suggested groups below a stand-in for the "All entries" root.
    static GroupTreeNode createSuggestions() {
        GroupTreeNode allEntries = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        allEntries.addSubgroup(GroupsFactory.createWithoutFilesGroup());
        allEntries.addSubgroup(GroupsFactory.createWithoutGroupsGroup());
        allEntries.addChild(GroupsFactory.createMarkingAndGradingGroups());
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

    /// Group names are unique within a library, see [GroupTreeNode#findGroupByName(String)].
    /// Paths of group names identify groups, for example when dragging them.
    private static void useFreeName(GroupTreeNode allEntries, GroupTreeNode suggestion) {
        String name = suggestion.getName();
        for (int number = 2; allEntries.findGroupByName(suggestion.getName()).isPresent(); number++) {
            suggestion.getGroup().nameProperty().set(name + " (" + number + ")");
        }
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
                && searchGroup.getSearchFlags().equals(suggestedSearchGroup.getSearchFlags())
                && searchGroup.getHierarchicalContext() == suggestedSearchGroup.getHierarchicalContext();
    }

    private record MissingSuggestion(GroupTreeNode parent, GroupTreeNode suggestion, List<GroupTreeNode> followingSuggestions) {
    }
}
