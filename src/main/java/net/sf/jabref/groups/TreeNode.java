package net.sf.jabref.groups;

import java.util.*;

public abstract class TreeNode {

    /**
     * this node's parent, or null if this node has no parent
     */
    private GroupTreeNode parent;
    /**
     * array of children, may be empty if this node has no children (but never null)
     */
    private List<GroupTreeNode> children;

    public TreeNode() {
        parent = null;
        children = new ArrayList<>();

        if (!(this instanceof GroupTreeNode)) {
            throw new UnsupportedOperationException("The class extending TreeNode has to derive from GroupTreeNode");
        }
    }

    /**
     * @return An indexed path from the root node to this node. The elements in
     * the returned array represent the child index of each node in the
     * path. If this node is the root node, the returned array has zero
     * elements.
     */
    public List<Integer> getIndexedPath() {
        if (parent == null) {
            return Collections.singletonList(0);
        }

        List<Integer> path = parent.getIndexedPath();
        path.add(getPositionInParent());
        return path;
    }

    /**
     * Returns the node indicated by the specified indexedPath, which contains
     * child indices obtained e.g. by getIndexedPath().
     */
    public GroupTreeNode getNode(List<Integer> indexedPath) {
        GroupTreeNode cursor = (GroupTreeNode) this;
        for (int anIndexedPath : indexedPath) {
            cursor = cursor.getChildAt(anIndexedPath);
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
    public GroupTreeNode getDescendant(List<Integer> indexedPath) {
        GroupTreeNode cursor = (GroupTreeNode) this;
        for (int i = 0; (i < indexedPath.size()) && (cursor != null); ++i) {
            cursor = cursor.getChildAt(indexedPath.get(i));
        }
        return cursor;
    }

    public int getPositionInParent() {
        return getParent().getIndex((GroupTreeNode) this);
    }

    /**
     * Returns the index of the specified child in this node's child list.
     * If the specified node is not a child of this node, returns <code>-1</code>.
     * This method performs a linear search and is O(n) where n is the number of children.
     *
     * @param childNode the GroupTreeNode to search for among this node's children
     * @return an int giving the index of the node in this node's child list,
     * or <code>-1</code> if the specified node is a not a child of this node
     * @throws NullPointerException if <code>aChild</code> is null
     */
    public int getIndex(GroupTreeNode childNode) {
        Objects.requireNonNull(childNode);
        return children.indexOf(childNode);
    }

    /**
     * Returns the number of levels above this node -- the distance from the root to this node.
     * If this node is the root, returns 0.
     *
     * @return the number of levels above this node
     */
    public int getLevel() {
        if (parent == null) {
            return 0;
        }
        return parent.getLevel() + 1;
    }

    /**
     * Returns the number of children of this node.
     *
     * @return an int giving the number of children of this node
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Removes <code>newChild</code> from its parent and makes it a child of this node
     * by adding it to the end of this node's child array.
     *
     * @param newChild node to add as a child of this node
     * @throws NullPointerException if <code>newChild</code> is null
     * @see #insert
     */
    public void add(GroupTreeNode newChild) {
        Objects.requireNonNull(newChild);

        if (newChild.getParent() == this) {
            insert(newChild, getChildCount() - 1);
        } else {
            insert(newChild, getChildCount());
        }
    }

    /**
     * Returns the path from the root, to get to this node. The last element in the path is this node.
     *
     * @return an array of TreeNode objects giving the path, where the first element in the path is the root
     * and the last element is this node.
     */
    public List<GroupTreeNode> getPath() {
        if (parent == null) {
            return Collections.singletonList((GroupTreeNode) this);
        }

        List<GroupTreeNode> path = parent.getPath();
        path.add((GroupTreeNode) this);
        return path;
    }

    /**
     * Returns the next sibling of this node in the parent's children list.
     * Returns null if this node has no parent or if it is the parent's last child.
     * <p>
     * This method performs a linear search that is O(n) where n is the number of children;
     * to traverse the entire array, use the parent's child enumeration instead.
     *
     * @return the sibling of this node that immediately follows this node
     * @see #children
     */
    protected GroupTreeNode getNextSibling() {
        return getRelativeSibling(+1);
    }

    private GroupTreeNode getRelativeSibling(int shiftIndex) {
        if (parent == null) {
            return null;
        } else {
            int indexInParent = getPositionInParent();
            int indexTarget = indexInParent + shiftIndex;
            if (indexTarget >= 0 && indexTarget < parent.getChildCount()) {
                return parent.getChildAt(indexTarget);
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the previous sibling of this node in the parent's children list.
     * Returns null if this node has no parent or is the parent's first child.
     * <p>
     * This method performs a linear search that is O(n) where n is the number of children.
     *
     * @return the sibling of this node that immediately precedes this node
     */
    protected GroupTreeNode getPreviousSibling() {
        return getRelativeSibling(-1);
    }

    /**
     * Returns this node's parent or null if this node has no parent.
     *
     * @return this node's parent GroupTreeNode, or null if this node has no parent
     */
    public GroupTreeNode getParent() {
        return parent;
    }

    protected void setParent(GroupTreeNode parent) {
        this.parent = parent;
    }

    /**
     * Returns the child at the specified index in this node's child array.
     *
     * @param index an index into this node's child array
     * @return the GroupTreeNode in this node's child array at the specified index
     * @throws ArrayIndexOutOfBoundsException if <code>index</code> is out of bounds
     */
    public GroupTreeNode getChildAt(int index) {
        return children.get(index);
    }

    /**
     * Returns true if this node is the root of the tree.
     * The root is the only node in the tree with a null parent; every tree has exactl one root.
     *
     * @return true if this node is the root of its tree
     */
    public boolean isRoot() {
        return getParent() == null;
    }

    /**
     * Removes <code>newChild</code> from its present parent (if it has a
     * parent), sets the child's parent to this node, and then adds the child
     * to this node's child array at index <code>childIndex</code>.
     * <code>newChild</code> must not be null and must not be an ancestor of
     * this node.
     *
     * @param child      the GroupTreeNode to insert under this node
     * @param childIndex the index in this node's child array where this node is to be inserted
     * @throws ArrayIndexOutOfBoundsException if <code>childIndex</code> is out of bounds
     * @throws IllegalArgumentException       if <code>newChild</code> is null or is an ancestor of this node
     * @see #isNodeDescendant
     */
    public void insert(GroupTreeNode child, int childIndex) {
        Objects.requireNonNull(child);

        if (isNodeAncestor(child)) {
            throw new IllegalArgumentException("new child is an ancestor");
        }

        // Remove from previous parent
        GroupTreeNode oldParent = child.getParent();
        if (oldParent != null) {
            oldParent.remove(child);
        }

        // Add as child
        child.setParent((GroupTreeNode) this);
        children.add(childIndex, child);
    }

    /**
     * Returns true if <code>anotherNode</code> is an ancestor of this node
     * -- if it is this node, this node's parent, or an ancestor of this node's parent.
     * (Note that a node is considered an ancestor of itself.)
     * If <code>anotherNode</code> is null, this method returns false.
     * This operation is at worst O(h) where h is the distance from the root to this node.
     *
     * @param anotherNode node to test as an ancestor of this node
     * @return true if this node is a descendant of <code>anotherNode</code>
     * @see #isNodeDescendant
     */
    public boolean isNodeAncestor(GroupTreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }

        if (anotherNode == this) {
            return true;
        } else {
            return parent != null && parent.isNodeAncestor(anotherNode);
        }
    }

    /**
     * Returns the root of the tree that contains this node. The root is the ancestor with a null parent.
     *
     * @return the root of the tree that contains this node
     * @see #isNodeAncestor
     */
    public GroupTreeNode getRoot() {
        if (parent == null) {
            return (GroupTreeNode) this;
        } else {
            return parent.getRoot();
        }
    }

    /**
     * Returns true if this node has no children.
     *
     * @return true if this node has no children
     */
    public boolean isLeaf() {
        return (getChildCount() == 0);
    }

    /**
     * Removes the subtree rooted at this node from the tree, giving this node a null parent.
     * Does nothing if this node is the root of it tree.
     */
    public void removeFromParent() {
        if (parent != null) {
            parent.remove((GroupTreeNode) this);
        }
    }

    /**
     * Removes all of this node's children, setting their parents to null.
     * If this node has no children, this method does nothing.
     */
    public void removeAllChildren() {
        for (GroupTreeNode child : children) {
            remove(child);
        }
    }

    /**
     * Returns this node's first child.
     *
     * @return the first child of this node
     */
    public GroupTreeNode getFirstChild() {
        if (children.size() == 0) {
            throw new NoSuchElementException("node has no children");
        }
        return getChildAt(0);
    }

    /**
     * Returns true if <code>anotherNode</code> is a descendant of this node
     * -- if it is this node, one of this node's children, or a descendant of one of this node's children.
     * Note that a node is considered a descendant of itself.
     * <p>
     * If <code>anotherNode</code> is null, returns false.
     * This operation is at worst O(h) where h is the distance from the root to <code>anotherNode</code>.
     *
     * @param anotherNode node to test as descendant of this node
     * @return true if this node is an ancestor of <code>anotherNode</code>
     * @see #isNodeAncestor
     */
    public boolean isNodeDescendant(GroupTreeNode anotherNode) {
        return anotherNode != null && anotherNode.isNodeAncestor((GroupTreeNode) this);
    }

    /**
     * Creates and returns a forward-order Iterable of this node's children.
     * Modifying this node's child array invalidates any child iterables created before the modification.
     *
     * @return an Iterable of this node's children
     */
    public Iterable<GroupTreeNode> children() {
        return children;
    }

    /**
     * Removes <code>child</code> from this node's child array, giving it a null parent.
     *
     * @param child a child of this node to remove
     */
    public void remove(GroupTreeNode child) {
        Objects.requireNonNull(child);

        remove(getIndex(child));       // linear search
    }

    /**
     * Removes the child at the specified index from this node's children and sets that node's parent to null.
     *
     * @param childIndex the index in this node's child array of the child to remove
     * @throws ArrayIndexOutOfBoundsException if <code>childIndex</code> is out of bounds
     */
    public void remove(int childIndex) {
        GroupTreeNode child = getChildAt(childIndex);
        children.remove(childIndex);
        child.setParent(null);
    }

    /**
     * @param path A sequence of child indices that designate a node relative to
     *             this node.
     * @return The node designated by the specified path, or null if one or more
     * indices in the path could not be resolved.
     */
    public GroupTreeNode getChildAt(List<Integer> path) {
        GroupTreeNode cursor = (GroupTreeNode) this;
        for (int i = 0; (i < path.size()) && (cursor != null); ++i) {
            cursor = cursor.getChildAt(path.get(i));
        }
        return cursor;
    }
}
