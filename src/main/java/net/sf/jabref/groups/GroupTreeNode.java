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
package net.sf.jabref.groups;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.logic.search.SearchMatcher;
import net.sf.jabref.logic.search.matchers.MatcherSet;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.groups.structure.AllEntriesGroup;
import net.sf.jabref.groups.structure.GroupHierarchyType;
import net.sf.jabref.logic.search.matchers.MatcherSets;

/**
 * A node in the groups tree that holds exactly one AbstractGroup.
 *
 * @author jzieren
 */
public class GroupTreeNode extends DefaultMutableTreeNode implements Transferable {

    public static final DataFlavor FLAVOR;
    private static final DataFlavor[] FLAVORS;

    static {
        DataFlavor df = null;
        try {
            df = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=net.sf.jabref.groups.GroupTreeNode");
        } catch (ClassNotFoundException e) {
            // never happens
        }
        FLAVOR = df;
        FLAVORS = new DataFlavor[] {GroupTreeNode.FLAVOR};
    }


    /**
     * Creates this node and associates the specified group with it.
     */
    public GroupTreeNode(AbstractGroup group) {
        setGroup(group);
    }

    /**
     * @return The group associated with this node.
     */
    public AbstractGroup getGroup() {
        return (AbstractGroup) getUserObject();
    }

    /**
     * Associates the specified group with this node.
     */
    public void setGroup(AbstractGroup group) {
        setUserObject(group);
    }

    /**
     * Returns a textual representation of this node and its children. This
     * representation contains both the tree structure and the textual
     * representations of the group associated with each node. It thus allows a
     * complete reconstruction of this object and its children.
     */
    public String getTreeAsString() {
        StringBuilder sb = new StringBuilder();
        Enumeration<GroupTreeNode> e = preorderEnumeration();
        GroupTreeNode cursor;
        while (e.hasMoreElements()) {
            cursor = e.nextElement();
            sb.append(cursor.getLevel()).append(' ').append(cursor.getGroup()).append('\n');
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
        GroupTreeNode copy = new GroupTreeNode(getGroup());
        for (int i = 0; i < getChildCount(); ++i) {
            copy.add(((GroupTreeNode) getChildAt(i)).deepCopy());
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
        for (int i = 0; i < getChildCount(); ++i) {
            GroupTreeNode node = (GroupTreeNode) getChildAt(i);
            node.getGroup().refreshForNewDatabase(db);
            node.refreshGroupsForNewDatabase(db);
        }
    }

    /**
     * @return An indexed path from the root node to this node. The elements in
     * the returned array represent the child index of each node in the
     * path. If this node is the root node, the returned array has zero
     * elements.
     */
    public int[] getIndexedPath() {
        TreeNode[] path = getPath();
        int[] indexedPath = new int[path.length - 1];
        for (int i = 1; i < path.length; ++i) {
            indexedPath[i - 1] = path[i - 1].getIndex(path[i]);
        }
        return indexedPath;
    }

    /**
     * Returns the node indicated by the specified indexedPath, which contains
     * child indices obtained e.g. by getIndexedPath().
     */
    public GroupTreeNode getNode(int[] indexedPath) {
        GroupTreeNode cursor = this;
        for (int anIndexedPath : indexedPath) {
            cursor = (GroupTreeNode) cursor.getChildAt(anIndexedPath);
        }
        return cursor;
    }

    /**
     * @param indexedPath A sequence of child indices that describe a path from this
     *                    node to one of its desendants. Be aware that if <b>indexedPath
     *                    </b> was obtained by getIndexedPath(), this node should
     *                    usually be the root node.
     * @return The descendant found by evaluating <b>indexedPath </b>. If the
     * path could not be traversed completely (i.e. one of the child
     * indices did not exist), null will be returned.
     */
    public GroupTreeNode getDescendant(int[] indexedPath) {
        GroupTreeNode cursor = this;
        for (int i = 0; (i < indexedPath.length) && (cursor != null); ++i) {
            cursor = (GroupTreeNode) cursor.getChildAt(indexedPath[i]);
        }
        return cursor;
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
        return getSearchRule(getGroup().getHierarchicalContext());
    }

    private SearchMatcher getSearchRule(GroupHierarchyType originalContext) {
        final GroupHierarchyType context = getGroup().getHierarchicalContext();
        if (context == GroupHierarchyType.INDEPENDENT) {
            return getGroup();
        }
        MatcherSet searchRule = MatcherSets.build(context == GroupHierarchyType.REFINING ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);
        searchRule.addRule(getGroup());
        if ((context == GroupHierarchyType.INCLUDING)
                && (originalContext != GroupHierarchyType.REFINING)) {
            for (int i = 0; i < getChildCount(); ++i) {
                searchRule.addRule(((GroupTreeNode) getChildAt(i))
                        .getSearchRule(originalContext));
            }
        } else if ((context == GroupHierarchyType.REFINING) && !isRoot()
                && (originalContext != GroupHierarchyType.INCLUDING)) {
            searchRule.addRule(((GroupTreeNode) getParent())
                    .getSearchRule(originalContext));
        }
        return searchRule;
    }

    public boolean canMoveUp() {
        return (getPreviousSibling() != null)
                && !(getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveDown() {
        return (getNextSibling() != null)
                && !(getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveLeft() {
        return !(getGroup() instanceof AllEntriesGroup)
                && !(((GroupTreeNode) getParent()).getGroup() instanceof AllEntriesGroup);
    }

    public boolean canMoveRight() {
        return (getPreviousSibling() != null)
                && !(getGroup() instanceof AllEntriesGroup);
    }

    public AbstractUndoableEdit moveUp(GroupSelector groupSelector) {
        final GroupTreeNode myParent = (GroupTreeNode) getParent();
        final int index = myParent.getIndex(this);
        if (index > 0) {
            UndoableMoveGroup undo = new UndoableMoveGroup(groupSelector,
                    groupSelector.getGroupTreeRoot(), this, myParent, index - 1);
            myParent.insert(this, index - 1);
            return undo;
        }
        return null;
    }

    public AbstractUndoableEdit moveDown(GroupSelector groupSelector) {
        final GroupTreeNode myParent = (GroupTreeNode) getParent();
        final int index = myParent.getIndex(this);
        if (index < (parent.getChildCount() - 1)) {
            UndoableMoveGroup undo = new UndoableMoveGroup(groupSelector,
                    groupSelector.getGroupTreeRoot(), this, myParent, index + 1);
            myParent.insert(this, index + 1);
            return undo;
        }
        return null;
    }

    public AbstractUndoableEdit moveLeft(GroupSelector groupSelector) {
        final GroupTreeNode myParent = (GroupTreeNode) getParent();
        final GroupTreeNode myGrandParent = (GroupTreeNode) myParent
                .getParent();
        // paranoia
        if (myGrandParent == null) {
            return null;
        }
        final int index = myGrandParent.getIndex(myParent);
        UndoableMoveGroup undo = new UndoableMoveGroup(groupSelector,
                groupSelector.getGroupTreeRoot(), this, myGrandParent,
                index + 1);
        myGrandParent.insert(this, index + 1);
        return undo;
    }

    public AbstractUndoableEdit moveRight(GroupSelector groupSelector) {
        final GroupTreeNode myPreviousSibling = (GroupTreeNode) getPreviousSibling();
        // paranoia
        if (myPreviousSibling == null) {
            return null;
        }
        UndoableMoveGroup undo = new UndoableMoveGroup(groupSelector,
                groupSelector.getGroupTreeRoot(), this, myPreviousSibling,
                myPreviousSibling.getChildCount());
        myPreviousSibling.add(this);
        return undo;
    }

    /**
     * @param path A sequence of child indices that designate a node relative to
     *             this node.
     * @return The node designated by the specified path, or null if one or more
     * indices in the path could not be resolved.
     */
    public GroupTreeNode getChildAt(int[] path) {
        GroupTreeNode cursor = this;
        for (int i = 0; (i < path.length) && (cursor != null); ++i) {
            cursor = (GroupTreeNode) cursor.getChildAt(path[i]);
        }
        return cursor;
    }

    /**
     * Adds the selected entries to this node's group.
     */
    public AbstractUndoableEdit addToGroup(List<BibEntry> entries) {
        if (getGroup() == null) {
            return null; // paranoia
        }
        AbstractUndoableEdit undo = getGroup().add(entries);
        if (undo instanceof UndoableChangeAssignment) {
            ((UndoableChangeAssignment) undo).setEditedNode(this);
        }
        return undo;
    }

    /**
     * Removes the selected entries from this node's group.
     */
    public AbstractUndoableEdit removeFromGroup(List<BibEntry> entries) {
        if (getGroup() == null) {
            return null; // paranoia
        }
        AbstractUndoableEdit undo = getGroup().remove(entries);
        if (undo instanceof UndoableChangeAssignment) {
            ((UndoableChangeAssignment) undo).setEditedNode(this);
        }
        return undo;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return GroupTreeNode.FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor someFlavor) {
        return someFlavor.equals(GroupTreeNode.FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor someFlavor)
            throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(someFlavor)) {
            throw new UnsupportedFlavorException(someFlavor);
        }
        return this;
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
        if (getChildCount() != otherNode.getChildCount()) {
            return false;
        }
        AbstractGroup g1 = getGroup();
        AbstractGroup g2 = otherNode.getGroup();
        if (((g1 == null) && (g2 != null)) || ((g1 != null) && (g2 == null))) {
            return false;
        }
        if ((g1 != null) && (g2 != null) && !g1.equals(g2)) {
            return false;
        }
        for (int i = 0; i < getChildCount(); ++i) {
            if (!getChildAt(i).equals(otherNode.getChildAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getGroup().getName().hashCode();
    }
}
