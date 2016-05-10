/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.search.SearchMatcher;
import net.sf.jabref.logic.search.matchers.MatcherSet;
import net.sf.jabref.logic.search.matchers.MatcherSets;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

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
     * @param group the new group (has to be non-null)
     */
    public void setGroup(AbstractGroup group) {
        this.group = Objects.requireNonNull(group);
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
        if (o == null || getClass() != o.getClass()) {
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

    public GroupTreeNode addSubgroup(AbstractGroup group) {
        GroupTreeNode child = new GroupTreeNode(group);
        addChild(child);
        return child;
    }

    @Override
    public String toString() {
        return String.valueOf(this.getLevel()) + ' ' + group.toString();
    }

    @Override
    public GroupTreeNode copyNode() {
        return new GroupTreeNode(group);
    }
}
