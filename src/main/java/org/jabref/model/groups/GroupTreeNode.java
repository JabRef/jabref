package org.jabref.model.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.FieldChange;
import org.jabref.model.TreeNode;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;

/**
 * A node in the groups tree that holds exactly one AbstractGroup.
 */
public class GroupTreeNode extends TreeNode<GroupTreeNode> {

    private static final String PATH_DELIMITER = " > ";
    private AbstractGroup group;

    /**
     * Creates this node and associates the specified group with it.
     *
     * @param group the group underlying this node
     */
    public GroupTreeNode(AbstractGroup group) {
        super(GroupTreeNode.class);
        setGroup(group, false, false, null);
    }

    public static GroupTreeNode fromGroup(AbstractGroup group) {
        return new GroupTreeNode(group);
    }

    /**
     * Returns the group underlying this node.
     *
     * @return the group associated with this node
     */
    public AbstractGroup getGroup() {
        return group;
    }

    /**
     * Associates the specified group with this node.
     *
     * @param newGroup the new group (has to be non-null)
     * @deprecated use {@link #setGroup(AbstractGroup, boolean, boolean, List)}} instead
     */
    @Deprecated
    public void setGroup(AbstractGroup newGroup) {
        this.group = Objects.requireNonNull(newGroup);
    }

    /**
     * Associates the specified group with this node while also providing the possibility to modify previous matched entries so that they are now matched by the new group.
     *
     * @param newGroup                        the new group (has to be non-null)
     * @param shouldKeepPreviousAssignments   specifies whether previous matched entries should be added to the new group
     * @param shouldRemovePreviousAssignments specifies whether previous matched entries should be removed from the old group
     * @param entriesInDatabase               list of entries in the database
     */
    public List<FieldChange> setGroup(AbstractGroup newGroup, boolean shouldKeepPreviousAssignments,
                                      boolean shouldRemovePreviousAssignments, List<BibEntry> entriesInDatabase) {
        AbstractGroup oldGroup = getGroup();
        group = Objects.requireNonNull(newGroup);

        List<FieldChange> changes = new ArrayList<>();
        boolean shouldRemove = shouldRemovePreviousAssignments && (oldGroup instanceof GroupEntryChanger);
        boolean shouldAdd = shouldKeepPreviousAssignments && (newGroup instanceof GroupEntryChanger);
        if (shouldAdd || shouldRemove) {
            List<BibEntry> entriesMatchedByOldGroup = entriesInDatabase.stream().filter(oldGroup::isMatch)
                                                                       .collect(Collectors.toList());
            if (shouldRemove) {
                GroupEntryChanger entryChanger = (GroupEntryChanger) oldGroup;
                changes.addAll(entryChanger.remove(entriesMatchedByOldGroup));
            }

            if (shouldAdd) {
                GroupEntryChanger entryChanger = (GroupEntryChanger) newGroup;
                changes.addAll(entryChanger.add(entriesMatchedByOldGroup));
            }
        }
        return changes;
    }

    /**
     * Creates a {@link SearchMatcher} that matches entries of this group and that takes the hierarchical information into account. I.e., it finds elements contained in this nodes group, or the union of those elements in its own group and its children's groups (recursively), or the intersection of the elements in its own group and its parent's group (depending on the hierarchical settings stored in the involved groups)
     */
    public SearchMatcher getSearchMatcher() {
        return getSearchMatcher(group.getHierarchicalContext());
    }

    private SearchMatcher getSearchMatcher(GroupHierarchyType originalContext) {
        final GroupHierarchyType context = group.getHierarchicalContext();
        if (context == GroupHierarchyType.INDEPENDENT) {
            return group;
        }
        MatcherSet searchRule = MatcherSets.build(
                context == GroupHierarchyType.REFINING ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);
        searchRule.addRule(group);
        if ((context == GroupHierarchyType.INCLUDING) && (originalContext != GroupHierarchyType.REFINING)) {
            for (GroupTreeNode child : getChildren()) {
                searchRule.addRule(child.getSearchMatcher(originalContext));
            }
        } else if ((context == GroupHierarchyType.REFINING) && !isRoot() && (originalContext
                != GroupHierarchyType.INCLUDING)) {
            // noinspection OptionalGetWithoutIsPresent
            searchRule.addRule(getParent().get().getSearchMatcher(originalContext));
        }
        return searchRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        GroupTreeNode that = (GroupTreeNode) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(getChildren(), that.getChildren());
    }

    @Override
    public int hashCode() {
        return Objects.hash(group);
    }

    public List<GroupTreeNode> getContainingGroups(List<BibEntry> entries, boolean requireAll) {
        List<GroupTreeNode> groups = new ArrayList<>();

        // Add myself if I contain the entries
        if (requireAll) {
            if (this.group.containsAll(entries)) {
                groups.add(this);
            }
        } else {
            if (this.group.containsAny(entries)) {
                groups.add(this);
            }
        }

        // Traverse children
        for (GroupTreeNode child : getChildren()) {
            groups.addAll(child.getContainingGroups(entries, requireAll));
        }

        return groups;
    }

    /**
     * Determines all groups in the subtree starting at this node which contain the given entry.
     */
    public List<GroupTreeNode> getMatchingGroups(BibEntry entry) {
        return getMatchingGroups(Collections.singletonList(entry));
    }

    /**
     * Determines all groups in the subtree starting at this node which contain at least one of the given entries.
     */
    public List<GroupTreeNode> getMatchingGroups(List<BibEntry> entries) {
        List<GroupTreeNode> groups = new ArrayList<>();

        // Add myself if I contain the entries
        SearchMatcher matcher = getSearchMatcher();
        for (BibEntry entry : entries) {
            if (matcher.isMatch(entry)) {
                groups.add(this);
                break;
            }
        }

        // Traverse children
        for (GroupTreeNode child : getChildren()) {
            groups.addAll(child.getMatchingGroups(entries));
        }

        return groups;
    }

    public List<BibEntry> getEntriesInGroup(List<BibEntry> entries) {
        List<BibEntry> result = new ArrayList<>();
        for (BibEntry entry : entries) {
            if (this.group.contains(entry)) {
                result.add(entry);
            }
        }
        return result;
    }

    public String getName() {
        return group.getName();
    }

    public GroupTreeNode addSubgroup(AbstractGroup subgroup) {
        GroupTreeNode child = GroupTreeNode.fromGroup(subgroup);
        addChild(child);
        return child;
    }

    @Override
    public GroupTreeNode copyNode() {
        return GroupTreeNode.fromGroup(group);
    }

    /**
     * Determines the entries in the specified list which are matched by this group.
     *
     * @param entries list of entries to be searched
     * @return matched entries
     */
    public List<BibEntry> findMatches(List<BibEntry> entries) {
        SearchMatcher matcher = getSearchMatcher();
        return entries.stream()
                      .filter(matcher::isMatch)
                      .collect(Collectors.toList());
    }

    /**
     * Determines the entries in the specified database which are matched by this group.
     *
     * @param database database to be searched
     * @return matched entries
     */
    public List<BibEntry> findMatches(BibDatabase database) {
        return findMatches(database.getEntries());
    }

    /**
     * Returns whether this group matches the specified {@link BibEntry} while taking the hierarchical information into account.
     */
    public boolean matches(BibEntry entry) {
        return getSearchMatcher().isMatch(entry);
    }

    /**
     * Get the path from the root of the tree as a string (every group name is separated by {@link #PATH_DELIMITER}.
     * <p>
     * The name of the root is not included.
     */
    public String getPath() {
        return getPathFromRoot().stream()
                                .skip(1) // Skip root
                                .map(GroupTreeNode::getName)
                                .collect(Collectors.joining(PATH_DELIMITER));
    }

    @Override
    public String toString() {
        return "GroupTreeNode{" +
                "group=" + group +
                '}';
    }

    /**
     * Finds a children using the given path. Each group name should be separated by {@link #PATH_DELIMITER}.
     * <p>
     * The path should be generated using {@link #getPath()}.
     */
    public Optional<GroupTreeNode> getChildByPath(String pathToSource) {
        GroupTreeNode present = this;
        for (String groupName : pathToSource.split(PATH_DELIMITER)) {
            Optional<GroupTreeNode> childWithName = present
                    .getChildren().stream()
                    .filter(group -> Objects.equals(group.getName(), groupName))
                    .findFirst();
            if (childWithName.isPresent()) {
                present = childWithName.get();
            } else {
                // No child with that name found -> path seems to be invalid
                return Optional.empty();
            }
        }

        return Optional.of(present);
    }

    /**
     * Adds the specified entries to this group. If the group does not support explicit adding of entries (i.e., does not implement {@link GroupEntryChanger}), then no action is performed.
     */
    public List<FieldChange> addEntriesToGroup(Collection<BibEntry> entries) {
        if (getGroup() instanceof GroupEntryChanger) {
            return ((GroupEntryChanger) getGroup()).add(entries);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Removes the given entries from this group. If the group does not support the explicit removal of entries (i.e., does not implement {@link GroupEntryChanger}), then no action is performed.
     */
    public List<FieldChange> removeEntriesFromGroup(List<BibEntry> entries) {
        if (getGroup() instanceof GroupEntryChanger) {
            return ((GroupEntryChanger) getGroup()).remove(entries);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns true if the underlying groups of both {@link GroupTreeNode}s is the same.
     */
    public boolean isSameGroupAs(GroupTreeNode other) {
        return Objects.equals(group, other.group);
    }
}
