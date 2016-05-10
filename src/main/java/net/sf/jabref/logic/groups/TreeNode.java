package net.sf.jabref.logic.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Represents a node in a tree.
 * <p>
 * Usually, tree nodes have a value property which allows access to the value stored in the node.
 * In contrast to this approach, the TreeNode<T> class is designed to be used as a base class which provides the
 * tree traversing functionality via inheritance.
 * <p>
 * Example usage:
 * private class BasicTreeNode extends TreeNode<BasicTreeNode> {
 * public BasicTreeNode() {
 * super(BasicTreeNode.class);
 * }
 * }
 * <p>
 * This class started out as a copy of javax.swing.tree.DefaultMutableTreeNode.
 *
 * @param <T> the type of the class
 */
// We use some explicit casts of the form "(T) this". The constructor ensures that this cast is valid.
@SuppressWarnings("unchecked") public abstract class TreeNode<T extends TreeNode<T>> {

    /**
     * This node's parent, or null if this node has no parent
     */
    private T parent;
    /**
     * Array of children, may be empty if this node has no children (but never null)
     */
    private List<T> children;

    /**
     * Constructs a tree node without parent and no children.
     *
     * @param derivingClass class deriving from TreeNode<T>. It should always be "T.class".
     *                      We need this parameter since it is hard to get this information by other means.
     */
    public TreeNode(Class<T> derivingClass) {
        parent = null;
        children = new ArrayList<>();

        if (!derivingClass.isInstance(this)) {
            throw new UnsupportedOperationException("The class extending TreeNode<T> has to derive from T");
        }
    }

    /**
     * Get the path from the root node to this node.
     * <p>
     * The elements in the returned list represent the child index of each node in the path, starting at the root.
     * If this node is the root node, the returned list has zero elements.
     *
     * @return a list of numbers which represent an indexed path from the root node to this node
     */
    public List<Integer> getIndexedPathFromRoot() {
        if (parent == null) {
            return new ArrayList<>();
        }

        List<Integer> path = parent.getIndexedPathFromRoot();
        path.add(getPositionInParent());
        return path;
    }

    /**
     * Get the descendant of this node as indicated by the indexedPath.
     * <p>
     * If the path could not be traversed completely (i.e. one of the child indices did not exist),
     * an empty Optional will be returned.
     *
     * @param indexedPath sequence of child indices that describe a path from this node to one of its descendants.
     *                    Be aware that if indexedPath was obtained by getIndexedPathFromRoot(), this node should
     *                    usually be the root node.
     * @return descendant found by evaluating indexedPath
     */
    public Optional<T> getDescendant(List<Integer> indexedPath) {
        T cursor = (T) this;
        for (int index : indexedPath) {
            Optional<T> child = cursor.getChildAt(index);
            if (child.isPresent()) {
                cursor = child.get();
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(cursor);
    }

    /**
     * Get the child index of this node in its parent.
     * <p>
     * If this node is a root, then an UnsupportedOperationException is thrown.
     * Use the isRoot method to check for this case.
     *
     * @return the child index of this node in its parent
     */
    public int getPositionInParent() {
        return getParent().orElseThrow(() -> new UnsupportedOperationException("Roots have no position in parent"))
                .getIndexOfChild((T) this).get();
    }

    /**
     * Gets the index of the specified child in this node's child list.
     * <p>
     * If the specified node is not a child of this node, returns an empty Optional.
     * This method performs a linear search and is O(n) where n is the number of children.
     *
     * @param childNode the node to search for among this node's children
     * @return an integer giving the index of the node in this node's child list
     * or an empty Optional if the specified node is a not a child of this node
     * @throws NullPointerException if childNode is null
     */
    public Optional<Integer> getIndexOfChild(T childNode) {
        Objects.requireNonNull(childNode);
        int index = children.indexOf(childNode);
        if (index == -1) {
            return Optional.empty();
        } else {
            return Optional.of(index);
        }
    }

    /**
     * Gets the number of levels above this node, i.e. the distance from the root to this node.
     * <p>
     * If this node is the root, returns 0.
     *
     * @return an int giving the number of levels above this node
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
    public int getNumberOfChildren() {
        return children.size();
    }

    /**
     * Removes this node from its parent and makes it a child of the specified node
     * by adding it to the end of children list.
     * In this way the whole subtree based at this node is moved to the given node.
     *
     * @param target the new parent
     * @throws NullPointerException           if target is null
     * @throws ArrayIndexOutOfBoundsException if targetIndex is out of bounds
     * @throws UnsupportedOperationException  if target is an descendant of this node
     */
    public void moveTo(T target) {
        Objects.requireNonNull(target);

        Optional<T> oldParent = getParent();
        if (oldParent.isPresent() && oldParent.get() == target) {
            this.moveTo(target, target.getNumberOfChildren() - 1);
        } else {
            this.moveTo(target, target.getNumberOfChildren());
        }
    }

    /**
     * Returns the path from the root, to get to this node. The last element in the path is this node.
     *
     * @return a list of nodes giving the path, where the first element in the path is the root
     * and the last element is this node.
     */
    public List<T> getPathFromRoot() {
        if (parent == null) {
            List<T> pathToMe = new ArrayList<>();
            pathToMe.add((T) this);
            return pathToMe;
        }

        List<T> path = parent.getPathFromRoot();
        path.add((T) this);
        return path;
    }

    /**
     * Returns the next sibling of this node in the parent's children list.
     * Returns an empty Optional if this node has no parent or if it is the parent's last child.
     * <p>
     * This method performs a linear search that is O(n) where n is the number of children.
     * To traverse the entire children collection, use the parent's getChildren() instead.
     *
     * @return the sibling of this node that immediately follows this node
     * @see #getChildren
     */
    public Optional<T> getNextSibling() {
        return getRelativeSibling(+1);
    }

    /**
     * Returns the previous sibling of this node in the parent's children list.
     * Returns an empty Optional if this node has no parent or is the parent's first child.
     * <p>
     * This method performs a linear search that is O(n) where n is the number of children.
     *
     * @return the sibling of this node that immediately precedes this node
     * @see #getChildren
     */
    public Optional<T> getPreviousSibling() {
        return getRelativeSibling(-1);
    }

    /**
     * Returns the sibling which is shiftIndex away from this node.
     */
    private Optional<T> getRelativeSibling(int shiftIndex) {
        if (parent == null) {
            return Optional.empty();
        } else {
            int indexInParent = getPositionInParent();
            int indexTarget = indexInParent + shiftIndex;
            if (parent.childIndexExists(indexTarget)) {
                return parent.getChildAt(indexTarget);
            } else {
                return Optional.empty();
            }
        }
    }

    /**
     * Returns this node's parent or an empty Optional if this node has no parent.
     *
     * @return this node's parent T, or an empty Optional if this node has no parent
     */
    public Optional<T> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Sets the parent node of this node.
     * <p>
     * This method does not add this node to the children collection of the new parent nor does it remove this node
     * from the old parent. You should probably call moveTo or remove to change the tree.
     *
     * @param parent the new parent
     */
    protected void setParent(T parent) {
        this.parent = parent;
    }

    /**
     * Returns the child at the specified index in this node's children collection.
     *
     * @param index an index into this node's children collection
     * @return the node in this node's children collection at the specified index,
     * or an empty Optional if the index does not point to a child
     */
    public Optional<T> getChildAt(int index) {
        return childIndexExists(index) ? Optional.of(children.get(index)) : Optional.empty();
    }

    /**
     * Returns whether the specified index is a valid index for a child.
     *
     * @param index the index to be tested
     * @return returns true when index is at least 0 and less then the count of children
     */
    protected boolean childIndexExists(int index) {
        return index >= 0 && index < children.size();
    }

    /**
     * Returns true if this node is the root of the tree.
     * The root is the only node in the tree with an empty parent; every tree has exactly one root.
     *
     * @return true if this node is the root of its tree
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Returns true if this node is an ancestor of the given node.
     * <p>
     * A node is considered an ancestor of itself.
     *
     * @param anotherNode node to test
     * @return true if anotherNode is a descendant of this node
     * @throws NullPointerException if anotherNode is null
     * @see #isNodeDescendant
     */
    public boolean isAncestorOf(T anotherNode) {
        Objects.requireNonNull(anotherNode);

        if (anotherNode == this) {
            return true;
        } else {
            for (T child : children) {
                if (child.isAncestorOf(anotherNode)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns the root of the tree that contains this node. The root is the ancestor with an empty parent.
     * Thus a node without a parent is considered its own root.
     *
     * @return the root of the tree that contains this node
     */
    public T getRoot() {
        if (parent == null) {
            return (T) this;
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
        return (getNumberOfChildren() == 0);
    }

    /**
     * Removes the subtree rooted at this node from the tree, giving this node an empty parent.
     * Does nothing if this node is the root of it tree.
     */
    public void removeFromParent() {
        if (parent != null) {
            parent.removeChild((T) this);
        }
    }

    /**
     * Removes all of this node's children, setting their parents to empty.
     * If this node has no children, this method does nothing.
     */
    public void removeAllChildren() {
        while (getNumberOfChildren() > 0) {
            removeChild(0);
        }
    }

    /**
     * Returns this node's first child if it exists (otherwise returns an empty Optional).
     *
     * @return the first child of this node
     */
    public Optional<T> getFirstChild() {
        return getChildAt(0);
    }

    /**
     * Returns this node's last child if it exists (otherwise returns an empty Optional).
     *
     * @return the last child of this node
     */
    public Optional<T> getLastChild() {
        return getChildAt(children.size() - 1);
    }

    /**
     * Returns true if anotherNode is a descendant of this node
     * -- if it is this node, one of this node's children, or a descendant of one of this node's children.
     * Note that a node is considered a descendant of itself.
     * <p>
     * If anotherNode is null, an exception is thrown.
     *
     * @param anotherNode node to test as descendant of this node
     * @return true if this node is an ancestor of anotherNode
     * @see #isAncestorOf
     */
    public boolean isNodeDescendant(T anotherNode) {
        Objects.requireNonNull(anotherNode);

        return this.isAncestorOf(anotherNode);
    }

    /**
     * Gets a forward-order list of this node's children.
     * <p>
     * The returned list is unmodifiable - use the add and remove methods to modify the nodes children.
     * However, changing the nodes children (for example by calling moveTo) is reflected in a change of
     * the list returned by getChildren. In other words, getChildren provides a read-only view on the children but
     * not a copy.
     *
     * @return a list of this node's children
     */
    public List<T> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Removes the given child from this node's child list, giving it an empty parent.
     *
     * @param child a child of this node to remove
     */
    public void removeChild(T child) {
        Objects.requireNonNull(child);

        children.remove(child);
        child.setParent(null);

        notifyAboutDescendantChange((T)this);
    }

    /**
     * Removes the child at the specified index from this node's children and sets that node's parent to empty.
     * <p>
     * Does nothing if the index does not point to a child.
     *
     * @param childIndex the index in this node's child array of the child to remove
     */
    public void removeChild(int childIndex) {
        Optional<T> child = getChildAt(childIndex);
        if (child.isPresent()) {
            children.remove(childIndex);
            child.get().setParent(null);
        }

        notifyAboutDescendantChange((T)this);
    }

    /**
     * Adds the node at the end the children collection. Also sets the parent of the given node to this node.
     * The given node is not allowed to already be in a tree (i.e. it has to have no parent).
     *
     * @param child the node to add
     * @return the child node
     */
    public T addChild(T child) {
        return addChild(child, children.size());
    }

    /**
     * Adds the node at the given position in the children collection. Also sets the parent of the given node to this node.
     * The given node is not allowed to already be in a tree (i.e. it has to have no parent).
     *
     * @param child the node to add
     * @param index the position where the node should be added
     * @return the child node
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public T addChild(T child, int index) {
        Objects.requireNonNull(child);
        if (child.getParent().isPresent()) {
            throw new UnsupportedOperationException("Cannot add a node which already has a parent, use moveTo instead");
        }

        child.setParent((T) this);
        children.add(index, child);

        notifyAboutDescendantChange((T)this);

        return child;
    }

    /**
     * Removes all children from this node and makes them a child of the specified node
     * by adding it to the specified position in the children list.
     *
     * @param target      the new parent
     * @param targetIndex the position where the children should be inserted
     * @throws NullPointerException           if target is null
     * @throws ArrayIndexOutOfBoundsException if targetIndex is out of bounds
     * @throws UnsupportedOperationException  if target is an descendant of one of the children of this node
     */
    public void moveAllChildrenTo(T target, int targetIndex) {
        while (getNumberOfChildren() > 0) {
            getLastChild().get().moveTo(target, targetIndex);
        }
    }

    /**
     * Sorts the list of children according to the order induced by the specified {@link Comparator}.
     * <p>
     * All children must be mutually comparable using the specified comparator
     * (that is, {@code c.compare(e1, e2)} must not throw a {@code ClassCastException}
     * for any children {@code e1} and {@code e2} in the list).
     *
     * @param comparator the comparator used to compare the child nodes
     * @param recursive  if true the whole subtree is sorted
     * @throws NullPointerException if the comparator is null
     */
    public void sortChildren(Comparator<? super T> comparator, boolean recursive) {
        Objects.requireNonNull(comparator);

        if (this.isLeaf()) {
            return; // nothing to sort
        }

        int j = getNumberOfChildren() - 1;
        int lastModified;
        while (j > 0) {
            lastModified = j + 1;
            j = -1;
            for (int i = 1; i < lastModified; ++i) {
                T child1 = getChildAt(i - 1).get();
                T child2 = getChildAt(i).get();
                if (comparator.compare(child1, child2) > 0) {
                    child1.moveTo((T) this, i);
                    j = i;
                }
            }
        }
        if (recursive) {
            for (T child : getChildren()) {
                child.sortChildren(comparator, true);
            }
        }
    }

    /**
     * Removes this node from its parent and makes it a child of the specified node
     * by adding it to the specified position in the children list.
     * In this way the whole subtree based at this node is moved to the given node.
     *
     * @param target      the new parent
     * @param targetIndex the position where the children should be inserted
     * @throws NullPointerException           if target is null
     * @throws ArrayIndexOutOfBoundsException if targetIndex is out of bounds
     * @throws UnsupportedOperationException  if target is an descendant of this node
     */
    public void moveTo(T target, int targetIndex) {
        Objects.requireNonNull(target);

        // Check that the target node is not an ancestor of this node, because this would create loops in the tree
        if (this.isAncestorOf(target)) {
            throw new UnsupportedOperationException("the target cannot be a descendant of this node");
        }

        // Remove from previous parent
        Optional<T> oldParent = getParent();
        if (oldParent.isPresent()) {
            oldParent.get().removeChild((T) this);
        }

        // Add as child
        target.addChild((T) this, targetIndex);
    }

    /**
     * Creates a deep copy of this node and all of its children.
     *
     * @return a deep copy of the subtree
     */
    public T copySubtree() {
        T copy = copyNode();
        for (T child : getChildren()) {
            child.copySubtree().moveTo(copy);
        }
        return copy;
    }

    /**
     * Creates a copy of this node, completely separated from the tree (i.e. no children and no parent)
     *
     * @return a deep copy of this node
     */
    public abstract T copyNode();

    /**
     * The function which is invoked when something changed in the subtree.
     */
    private Consumer<T> onDescendantChanged = t -> {};

    /**
     * Adds the given function to the list of subscribers which are notified when something changes in the subtree.
     *
     * The following events are supported (the text in parentheses specifies which node is passed as the source):
     *  - addChild (new parent)
     *  - removeChild (old parent)
     *  - move (old parent and new parent)
     * @param subscriber function to be invoked upon a change
     */
    public void subscribeToDescendantChanged(Consumer<T> subscriber) {
        onDescendantChanged = onDescendantChanged.andThen(subscriber);
    }

    /**
     * Helper method which notifies all subscribers about a change in the subtree and bubbles the event to all parents.
     * @param source the node which changed
     */
    protected void notifyAboutDescendantChange(T source) {
        onDescendantChanged.accept(source);

        if(! isRoot()) {
            parent.notifyAboutDescendantChange(source);
        }
    }
}
