package net.sf.jabref.model.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.TreeNode;
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
        this.group = Objects.requireNonNull(group);
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
     * Associates the specified group with this node while also providing the possibility to modify previous matched
     * entries so that they are now matched by the new group.
     *
     * @param newGroup the new group (has to be non-null)
     * @param shouldKeepPreviousAssignments specifies whether previous matched entries should be added to the new group
     * @param shouldRemovePreviousAssignments specifies whether previous matched entries should be removed from the old group
     * @param entriesInDatabase list of entries in the database
     */
    public List<FieldChange> setGroup(AbstractGroup newGroup, boolean shouldKeepPreviousAssignments,
                                      boolean shouldRemovePreviousAssignments, List<BibEntry> entriesInDatabase) {
        AbstractGroup oldGroup = getGroup();
        setGroup(newGroup);

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
     * Creates a {@link SearchMatcher} that matches entries of this group and that takes the hierarchical information
     * into account. I.e., it finds elements contained in this nodes group,
     * or the union of those elements in its own group and its
     * children's groups (recursively), or the intersection of the elements in
     * its own group and its parent's group (depending on the hierarchical settings stored in the involved groups)
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
            //noinspection OptionalGetWithoutIsPresent
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
        SearchMatcher matcher = getSearchMatcher();
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
     * Determines the number of entries in the specified list which are matched by this group.
     * @param entries list of entries to be searched
     * @return number of hits
     */
    public int numberOfMatches(List<BibEntry> entries) {
        int hits = 0;
        SearchMatcher matcher = getSearchMatcher();
        for (BibEntry entry : entries) {
            if (matcher.isMatch(entry)) {
                hits++;
            }
        }
        return hits;
    }
}
