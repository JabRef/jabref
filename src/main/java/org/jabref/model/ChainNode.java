package org.jabref.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a node in a chain.
 * We view a chain as a vertical hierarchy and thus refer to the previous node as parent and the next node is a child.
 * <p>
 * In usual implementations, nodes function as wrappers around a data object. Thus normally they have a value property
 * which allows access to the value stored in the node.
 * In contrast to this approach, the ChainNode&lt;T> class is designed to be used as a base class which provides the
 * tree traversing functionality via inheritance.
 * <p>
 * Example usage:
 * private class BasicChainNode extends ChainNode&lt;BasicChainNode> {
 * public BasicChainNode() {
 * super(BasicChainNode.class);
 * }
 * }
 *
 * @param <T> the type of the class
 */
@SuppressWarnings("unchecked") // We use some explicit casts of the form "(T) this". The constructor ensures that this cast is valid.
public abstract class ChainNode<T extends ChainNode<T>> {

    /**
     * This node's parent, or null if this node has no parent
     */
    private T parent;
    /**
     * This node's child, or null if this node has no child
     */
    private T child;

    /**
     * Constructs a chain node without parent and no child.
     *
     * @param derivingClass class deriving from TreeNode&lt;T>. It should always be "T.class".
     *                      We need this parameter since it is hard to get this information by other means.
     */
    public ChainNode(Class<T> derivingClass) {
        parent = null;
        child = null;

        if (!derivingClass.isInstance(this)) {
            throw new UnsupportedOperationException("The class extending ChainNode<T> has to derive from T");
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
     * This method does not set this node as the child of the new parent nor does it remove this node
     * from the old parent. You should probably call {@link #moveTo(ChainNode)} to change the chain.
     *
     * @param parent the new parent
     */
    protected void setParent(T parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    /**
     * Returns this node's child or an empty Optional if this node has no child.
     *
     * @return this node's child T, or an empty Optional if this node has no child
     */
    public Optional<T> getChild() {
        return Optional.ofNullable(child);
    }

    /**
     * Adds the node as the child. Also sets the parent of the given node to this node.
     * The given node is not allowed to already be in a tree (i.e. it has to have no parent).
     *
     * @param child the node to add as child
     * @return the child node
     * @throws UnsupportedOperationException if the given node has already a parent
     */
    public T setChild(T child) {
        Objects.requireNonNull(child);
        if (child.getParent().isPresent()) {
            throw new UnsupportedOperationException("Cannot add a node which already has a parent, use moveTo instead");
        }

        child.setParent((T) this);
        this.child = child;

        return child;
    }

    /**
     * Removes this node from its parent and makes it a child of the specified node.
     * In this way the whole subchain based at this node is moved to the given node.
     *
     * @param target the new parent
     * @throws NullPointerException          if target is null
     * @throws UnsupportedOperationException if target is an descendant of this node
     */
    public void moveTo(T target) {
        Objects.requireNonNull(target);

        // Check that the target node is not an ancestor of this node, because this would create loops in the tree
        if (this.isAncestorOf(target)) {
            throw new UnsupportedOperationException("the target cannot be a descendant of this node");
        }

        // Remove from previous parent
        getParent().ifPresent(oldParent -> oldParent.removeChild());

        // Add as child
        target.setChild((T) this);
    }

    /**
     * Removes the child from this node's child list, giving it an empty parent.
     */
    public void removeChild() {
        if (child != null) {
            // NPE if this is ever called
            child.setParent(null);
        }
        child = null;
    }

    /**
     * Returns true if this node is an ancestor of the given node.
     * <p>
     * A node is considered an ancestor of itself.
     *
     * @param anotherNode node to test
     * @return true if anotherNode is a descendant of this node
     * @throws NullPointerException if anotherNode is null
     */
    public boolean isAncestorOf(T anotherNode) {
        Objects.requireNonNull(anotherNode);

        if (anotherNode == this) {
            return true;
        } else {
            return child.isAncestorOf(anotherNode);
        }
    }

    /**
     * Adds the given node at the end of the chain.
     * E.g., "A > B > C" + "D" -> "A > B > C > D".
     */
    public void addAtEnd(T node) {
        if (child == null) {
            setChild(node);
        } else {
            child.addAtEnd(node);
        }
    }
}
