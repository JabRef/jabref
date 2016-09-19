package net.sf.jabref.model.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.search.SearchMatcher;
import net.sf.jabref.model.search.matchers.MatcherSet;
import net.sf.jabref.model.search.matchers.MatcherSets;

/**
 * A node in the groups tree that holds exactly one AbstractGroup.
 */
public class GroupTreeNode extends TreeNode<GroupTreeNode> {

    private AbstractGroup group;

    /**
     * Creates this node and associates the specified group with it.
     *
     * @param group the group underlying this node
     */
    public GroupTreeNode(AbstractGroup group) {
        super(GroupTreeNode.class);
        setGroup(group);
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
     */
    @Deprecated // use other overload
    public void setGroup(AbstractGroup newGroup) {
        this.group = Objects.requireNonNull(newGroup);
    }

    /**
     * Associates the specified group with this node while also providing the possibility to modify previous matched
     * entries so that they are now matched by the new group.
     *
     * @param newGroup the new group (has to be non-null)
     * @param shouldKeepPreviousAssignments specifies whether previous matched entries should be carried over
     * @param entriesInDatabase list of entries in the database
     */
    public Optional<EntriesGroupChange> setGroup(AbstractGroup newGroup, boolean shouldKeepPreviousAssignments,
            List<BibEntry> entriesInDatabase) {
        AbstractGroup oldGroup = getGroup();
        setGroup(newGroup);

        // Keep assignments from previous group
        if (shouldKeepPreviousAssignments && newGroup.supportsAdd()) {
            List<BibEntry> entriesMatchedByOldGroup = entriesInDatabase.stream().filter(oldGroup::isMatch)
                    .collect(Collectors.toList());
            if ((oldGroup instanceof ExplicitGroup) && (newGroup instanceof ExplicitGroup)) {
                // Rename of explicit group, so remove old group assignment
                oldGroup.remove(entriesMatchedByOldGroup);
            }
            return newGroup.add(entriesMatchedByOldGroup);
        }
        return Optional.empty();
    }

    /**
     * Returns a textual representation of this node and its children. This
     * representation contains both the tree structure and the textual
     * representations of the group associated with each node.
     * Every node is one entry in the list of strings.
     *
     * @return a representation of the tree based at this node as a list of strings
     */
    public List<String> getTreeAsString() {

        List<String> representation = new ArrayList<>();

        // Append myself
        representation.add(this.toString());

        // Append children
        for(GroupTreeNode child : getChildren()) {
            representation.addAll(child.getTreeAsString());
        }

        return representation;
    }

    /**
     * Update all groups, if necessary, to handle the situation where the group
     * tree is applied to a different BibDatabase than it was created for. This
     * is for instance used when updating the group tree due to an external change.
     *
     * @param db The database to refresh for.
     * @deprecated This method shouldn't be necessary anymore once explicit group memberships are saved directly in the entry.
     * TODO: Remove this method.
     */
    @Deprecated
    public void refreshGroupsForNewDatabase(BibDatabase db) {
        for (GroupTreeNode node : getChildren()) {
            node.group.refreshForNewDatabase(db);
            node.refreshGroupsForNewDatabase(db);
        }
    }

    /**
     * Creates a SearchRule that finds elements contained in this nodes group,
     * or the union of those elements in its own group and its
     * children's groups (recursively), or the intersection of the elements in
     * its own group and its parent's group (depending on the hierarchical settings stored in the involved groups)
     *
     * @return a SearchRule that finds the desired elements
     */
    public SearchMatcher getSearchRule() {
        return getSearchRule(group.getHierarchicalContext());
    }

    private SearchMatcher getSearchRule(GroupHierarchyType originalContext) {
        final GroupHierarchyType context = group.getHierarchicalContext();
        if (context == GroupHierarchyType.INDEPENDENT) {
            return group;
        }
        MatcherSet searchRule = MatcherSets.build(
                context == GroupHierarchyType.REFINING ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);
        searchRule.addRule(group);
        if ((context == GroupHierarchyType.INCLUDING) && (originalContext != GroupHierarchyType.REFINING)) {
            for (GroupTreeNode child : getChildren()) {
                searchRule.addRule(child.getSearchRule(originalContext));
            }
        } else if ((context == GroupHierarchyType.REFINING) && !isRoot() && (originalContext
                != GroupHierarchyType.INCLUDING)) {
            searchRule.addRule(getParent().get().getSearchRule(originalContext));
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
        return Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group);
    }

    public List<GroupTreeNode> getContainingGroups(List<BibEntry> entries, boolean requireAll) {
        List<GroupTreeNode> groups = new ArrayList<>();

        // Add myself if I contain the entries
        if(requireAll) {
            if(this.group.containsAll(entries)) {
                groups.add(this);
            }
        } else {
            if(this.group.containsAny(entries)) {
                groups.add(this);
            }
        }

        // Traverse children
        for(GroupTreeNode child : getChildren()) {
            groups.addAll(child.getContainingGroups(entries, requireAll));
        }

        return groups;
    }

    public List<GroupTreeNode> getMatchingGroups(List<BibEntry> entries) {
        List<GroupTreeNode> groups = new ArrayList<>();

        // Add myself if I contain the entries
        SearchMatcher matcher = getSearchRule();
        for (BibEntry entry : entries) {
            if (matcher.isMatch(entry)) {
                groups.add(this);
                break;
            }
        }

        // Traverse children
        for(GroupTreeNode child : getChildren()) {
            groups.addAll(child.getMatchingGroups(entries));
        }

        return groups;
    }

    public boolean supportsAddingEntries() {
        return group.supportsAdd();
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
    public String toString() {
        return String.valueOf(this.getLevel()) + ' ' + group.toString();
    }

    @Override
    public GroupTreeNode copyNode() {
        return GroupTreeNode.fromGroup(group);
    }

    /**
     * Determines the number of entries in the specified list which are matched by this group.
     * @param entries list of entries to be searched
     * @return number of hits
     */
    public int numberOfHits(List<BibEntry> entries) {
        int hits = 0;
        SearchMatcher matcher = getSearchRule();
        for (BibEntry entry : entries) {
            if (matcher.isMatch(entry)) {
                hits++;
            }
        }
        return hits;
    }
}
