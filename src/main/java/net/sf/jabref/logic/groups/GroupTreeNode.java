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

import java.util.*;

import net.sf.jabref.logic.search.SearchMatcher;
import net.sf.jabref.logic.search.matchers.MatcherSet;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.search.matchers.MatcherSets;

/**
 * A node in the groups tree that holds exactly one AbstractGroup.
 *
 * @author jzieren
 */
public class GroupTreeNode extends TreeNode<GroupTreeNode> {

    private AbstractGroup group;

    /**
     * Creates this node and associates the specified group with it.
     */
    public GroupTreeNode(AbstractGroup group) {
        super(GroupTreeNode.class);
        setGroup(group);
    }

    /**
     * @return The group associated with this node.
     */
    public AbstractGroup getGroup() {
        return group;
    }

    /**
     * Associates the specified group with this node.
     */
    public void setGroup(AbstractGroup group) {
        this.group = Objects.requireNonNull(group);
    }

    /**
     * Returns a textual representation of this node and its children. This
     * representation contains both the tree structure and the textual
     * representations of the group associated with each node. It thus allows a
     * complete reconstruction of this object and its children.
     */
    public String getTreeAsString() {
        StringBuilder sb = new StringBuilder();

        // Append myself
        sb.append(this.getLevel()).append(' ').append(group.toString()).append('\n');

        // Append children
        for(GroupTreeNode child : getChildren()) {
            sb.append(child.getTreeAsString());
        }

        return sb.toString();
    }

    /**
     * Creates a deep copy of this node and all of its children, including all
     * groups.
     *
     * @return This object's deep copy.
     */
    public GroupTreeNode deepCopy() {
        GroupTreeNode copy = new GroupTreeNode(group);
        for (GroupTreeNode child : getChildren()) {
            child.deepCopy().moveTo(copy);
        }
        return copy;
    }

    /**
     * Update all groups, if necessary, to handle the situation where the group
     * tree is applied to a different BibDatabase than it was created for. This
     * is for instance used when updating the group tree due to an external change.
     *
     * @param db The database to refresh for.
     */
    public void refreshGroupsForNewDatabase(BibDatabase db) {
        for (GroupTreeNode node : getChildren()) {
            node.group.refreshForNewDatabase(db);
            node.refreshGroupsForNewDatabase(db);
        }
    }

    /**
     * A GroupTreeNode can create a SearchRule that finds elements contained in
     * its own group, or the union of those elements in its own group and its
     * children's groups (recursively), or the intersection of the elements in
     * its own group and its parent's group. This setting is configured in the
     * group contained in this node.
     *
     * @return A SearchRule that finds the desired elements.
     */
    public SearchMatcher getSearchRule() {
        return getSearchRule(group.getHierarchicalContext());
    }

    private SearchMatcher getSearchRule(GroupHierarchyType originalContext) {
        final GroupHierarchyType context = group.getHierarchicalContext();
        if (context == GroupHierarchyType.INDEPENDENT) {
            return group;
        }
        MatcherSet searchRule = MatcherSets.build(context == GroupHierarchyType.REFINING ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);
        searchRule.addRule(group);
        if ((context == GroupHierarchyType.INCLUDING)
                && (originalContext != GroupHierarchyType.REFINING)) {
            for (GroupTreeNode child : getChildren()) {
                searchRule.addRule(child.getSearchRule(originalContext));
            }
        } else if ((context == GroupHierarchyType.REFINING) && !isRoot()
                && (originalContext != GroupHierarchyType.INCLUDING)) {
            // TODO: Null!
            searchRule.addRule(getParent().get()
                    .getSearchRule(originalContext));
        }
        return searchRule;
    }

    public Optional<MoveGroupChange> moveUp() {
        final GroupTreeNode parent = getParent().get();
        // TODO: Null!
        final int index = parent.getIndexOfChild(this).get();
        if (index > 0) {
            this.moveTo(parent, index - 1);
            return Optional.of(new MoveGroupChange(parent, index, parent, index - 1));
        }
        return Optional.empty();
    }

    public Optional<MoveGroupChange> moveDown() {
        final GroupTreeNode parent = getParent().get();
        // TODO: Null!
        final int index = parent.getIndexOfChild(this).get();
        if (index < (parent.getNumberOfChildren() - 1)) {
            this.moveTo(parent, index + 1);
            return Optional.of(new MoveGroupChange(parent, index, parent, index + 1));
        }
        return Optional.empty();
    }

    public Optional<MoveGroupChange> moveLeft() {
        final GroupTreeNode parent = getParent().get(); // TODO: Null!
        final Optional<GroupTreeNode> grandParent = parent.getParent();
        final int index = this.getPositionInParent();

        if (! grandParent.isPresent()) {
            return Optional.empty();
        }
        final int indexOfParent = grandParent.get().getIndexOfChild(parent).get();
        this.moveTo(grandParent.get(), indexOfParent + 1);
        return Optional.of(new MoveGroupChange(parent, index, grandParent.get(), indexOfParent + 1));
    }

    public Optional<MoveGroupChange> moveRight() {
        final GroupTreeNode previousSibling = getPreviousSibling().get(); // TODO: Null
        final GroupTreeNode parent = getParent().get(); // TODO: Null!
        final int index = this.getPositionInParent();

        if (previousSibling == null) {
            return Optional.empty();
        }

        this.moveTo(previousSibling);
        return Optional.of(new MoveGroupChange(parent, index, previousSibling, previousSibling.getNumberOfChildren()));
    }

    /**
     * Adds the selected entries to this node's group.
     */
    public Optional<EntriesGroupChange> addToGroup(List<BibEntry> entries) {
        if(group.supportsAdd()) {
            return group.add(entries);
        }
        else {
            return Optional.empty();
        }
    }

    /**
     * Removes the selected entries from this node's group.
     */
    public Optional<EntriesGroupChange> removeFromGroup(List<BibEntry> entries) {
        if(group.supportsRemove()) {
            return group.remove(entries);
        }
        else {
            return Optional.empty();
        }
    }

    /**
     * Recursively compares this node's group and all subgroups.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GroupTreeNode)) {
            return false;
        }
        final GroupTreeNode otherNode = (GroupTreeNode) other;
        if (getNumberOfChildren() != otherNode.getNumberOfChildren()) {
            return false;
        }
        AbstractGroup g1 = group;
        AbstractGroup g2 = otherNode.group;
        if (((g1 == null) && (g2 != null)) || ((g1 != null) && (g2 == null))) {
            return false;
        }
        if ((g1 != null) && (g2 != null) && !g1.equals(g2)) {
            return false;
        }
        for (int i = 0; i < getNumberOfChildren(); ++i) {
            if (!getChildAt(i).equals(otherNode.getChildAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return group.getName().hashCode();
    }

    /**
     * Get all groups which contain any of the entries and which support removal of entries.
     */
    public List<GroupTreeNode> getContainingGroupsSupportingRemoval(List<BibEntry> entries) {
        List<GroupTreeNode> groups = new ArrayList<>();

        // Add myself if I contain the entries
        if(this.group.supportsRemove() && this.group.containsAny(entries)) {
            groups.add(this);
        }

        // Traverse children
        for(GroupTreeNode child : getChildren()) {
            groups.addAll(child.getContainingGroupsSupportingRemoval(entries));
        }

        return groups;
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

    /**
     * For all explicit subgroups, replace the i'th entry of originalEntries with the i'th entry of newEntries.
     */
    public void replaceEntriesInExplicitGroup(List<BibEntry> originalEntries, List<BibEntry> newEntries) {

        if(this.group instanceof ExplicitGroup) {
            ExplicitGroup group = (ExplicitGroup)this.group;
            for (int i = 0; i < originalEntries.size(); ++i) {
                BibEntry entry = originalEntries.get(i);
                if (group.contains(entry)) {
                    group.removeEntry(entry);
                    group.addEntry(newEntries.get(i));
                }
            }
        }

        // Traverse children
        for(GroupTreeNode child : getChildren()) {
            child.replaceEntriesInExplicitGroup(originalEntries, newEntries);
        }
    }


    public boolean supportsAddingEntries() {
        return group.supportsAdd();
    }
}
