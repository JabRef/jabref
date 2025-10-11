package org.jabref.model.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.FieldChange;
import org.jabref.model.TreeNode;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;

import org.jspecify.annotations.NonNull;

/**
 * A node in the groups tree that holds exactly one AbstractGroup.
 */
public class GroupTreeNode extends TreeNode<GroupTreeNode> {

    private static final String PATH_DELIMITER = " > ";
    private ObjectProperty<AbstractGroup> groupProperty = new SimpleObjectProperty<>();

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
        return groupProperty.get();
    }

    public ObjectProperty<AbstractGroup> getGroupProperty() {
        return groupProperty;
    }

    /**
     * Associates the specified group with this node.
     *
     * @param newGroup the new group (has to be non-null)
     */
    public void setGroup(@NonNull AbstractGroup newGroup) {
        this.groupProperty.set(newGroup);
    }

    /**
     * Associates the specified group with this node while also providing the possibility to modify previous matched entries so that they are now matched by the new group.
     *
     * @param newGroup                        the new group (has to be non-null)
     * @param shouldKeepPreviousAssignments   specifies whether previous matched entries should be added to the new group
     * @param shouldRemovePreviousAssignments specifies whether previous matched entries should be removed from the old group
     * @param entriesInDatabase               list of entries in the database
     */
    public List<FieldChange> setGroup(@NonNull AbstractGroup newGroup,
                                      boolean shouldKeepPreviousAssignments,
                                      boolean shouldRemovePreviousAssignments,
                                      List<BibEntry> entriesInDatabase) {
        AbstractGroup oldGroup = getGroup();
        groupProperty.set(newGroup);

        List<FieldChange> changes = new ArrayList<>();
        boolean shouldRemoveFromOldGroup = shouldRemovePreviousAssignments && (oldGroup instanceof GroupEntryChanger);
        boolean shouldAddToNewGroup = shouldKeepPreviousAssignments && (newGroup instanceof GroupEntryChanger);
        if (shouldAddToNewGroup || shouldRemoveFromOldGroup) {
            List<BibEntry> entriesMatchedByOldGroup = entriesInDatabase.stream().filter(oldGroup::isMatch)
                                                                       .collect(Collectors.toList());
            if (shouldRemoveFromOldGroup) {
                GroupEntryChanger entryChanger = (GroupEntryChanger) oldGroup;
                changes.addAll(entryChanger.remove(entriesMatchedByOldGroup));
            }

            if (shouldAddToNewGroup) {
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
        return getSearchMatcher(getGroup().getHierarchicalContext());
    }

    private SearchMatcher getSearchMatcher(GroupHierarchyType originalContext) {
        final GroupHierarchyType context = getGroup().getHierarchicalContext();
        if (context == GroupHierarchyType.INDEPENDENT) {
            return getGroup();
        }
        MatcherSet searchRule = MatcherSets.build(
                context == GroupHierarchyType.REFINING ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);
        searchRule.addRule(getGroup());
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
        return Objects.equals(getGroup(), that.getGroup()) &&
                Objects.equals(getChildren(), that.getChildren());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroup());
    }

    /**
     * Get only groups containing all the entries or just groups containing any of the
     *
     * @param entries    List of {@link BibEntry} to search for
     * @param requireAll Whether to return only groups that must contain all entries
     * @return List of {@link GroupTreeNode} containing the matches. {@link AllEntriesGroup} is always contained}
     */
    public List<GroupTreeNode> getContainingGroups(List<BibEntry> entries, boolean requireAll) {
        List<GroupTreeNode> groups = new ArrayList<>();

        // Add myself if I contain the entries
        if (requireAll) {
            if (this.getGroup().containsAll(entries)) {
                groups.add(this);
            }
        } else {
            if (this.getGroup().containsAny(entries)) {
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
        return getMatchingGroups(List.of(entry));
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
            if (this.getGroup().contains(entry)) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Get the name of the underlying group
     *
     * @return String the name of the group
     */
    public String getName() {
        return getGroup().getName();
    }

    public GroupTreeNode addSubgroup(AbstractGroup subgroup) {
        GroupTreeNode child = GroupTreeNode.fromGroup(subgroup);
        addChild(child);
        return child;
    }

    @Override
    public GroupTreeNode copyNode() {
        return GroupTreeNode.fromGroup(getGroup());
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
                "group=" + getGroup() +
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
            return List.of();
        }
    }

    /**
     * Removes the given entries from this group. If the group does not support the explicit removal of entries (i.e., does not implement {@link GroupEntryChanger}), then no action is performed.
     */
    public List<FieldChange> removeEntriesFromGroup(List<BibEntry> entries) {
        if (getGroup() instanceof GroupEntryChanger) {
            return ((GroupEntryChanger) getGroup()).remove(entries);
        } else {
            return List.of();
        }
    }

    /**
     * Returns true if the underlying groups of both {@link GroupTreeNode}s is the same.
     */
    public boolean isSameGroupAs(GroupTreeNode other) {
        return Objects.equals(getGroup(), other.getGroup());
    }

    public boolean containsGroup(AbstractGroup other) {
        if (this.getGroup() == other) {
            return true;
        } else {
            return this.getChildren().stream().anyMatch(child -> child.getGroup() == other);
        }
    }
}
