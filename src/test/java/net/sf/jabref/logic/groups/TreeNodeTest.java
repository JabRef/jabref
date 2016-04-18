package net.sf.jabref.logic.groups;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TreeNodeTest {

    /**
     * Gets the marked node in the following tree:
     * Root
     *      A
     *      A (= parent)
     *          B (<-- this)
     */
    public TreeNodeMock getNodeInSimpleTree(TreeNodeMock root) {
        root.addChild(new TreeNodeMock());
        TreeNodeMock parent = new TreeNodeMock();
        root.addChild(parent);
        TreeNodeMock node = new TreeNodeMock();
        parent.addChild(node);
        return node;
    }

    public TreeNodeMock getNodeInSimpleTree() {
        return getNodeInSimpleTree(new TreeNodeMock());
    }

    /**
     * Gets the marked node in the following tree:
     * Root
     *      A
     *      A
     *      A (= grand parent)
     *          B
     *          B (= parent)
     *              C (<-- this)
     *                  D (= child)
     *              C
     *              C
     *              C
     *          B
     *          B
     *      A
     */
    public TreeNodeMock getNodeInComplexTree(TreeNodeMock root) {
        root.addChild(new TreeNodeMock());
        root.addChild(new TreeNodeMock());
        TreeNodeMock grandParent = new TreeNodeMock();
        root.addChild(grandParent);
        root.addChild(new TreeNodeMock());

        grandParent.addChild(new TreeNodeMock());
        TreeNodeMock parent = new TreeNodeMock();
        grandParent.addChild(parent);
        grandParent.addChild(new TreeNodeMock());
        grandParent.addChild(new TreeNodeMock());

        TreeNodeMock node = new TreeNodeMock();
        parent.addChild(node);
        parent.addChild(new TreeNodeMock());
        parent.addChild(new TreeNodeMock());
        parent.addChild(new TreeNodeMock());

        node.addChild(new TreeNodeMock());
        return node;
    }

    public TreeNodeMock getNodeInComplexTree() {
        return getNodeInComplexTree(new TreeNodeMock());
    }

    /**
     * Gets the marked in the following tree:
     * Root
     *      A
     *      A
     *      A (<- this)
     *      A
     */
    public TreeNodeMock getNodeAsChild(TreeNodeMock root) {
        root.addChild(new TreeNodeMock());
        root.addChild(new TreeNodeMock());
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        root.addChild(new TreeNodeMock());
        return node;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void constructorChecksThatClassImplementsCorrectInterface() {
        new WrongTreeNodeImplementation();
    }

    @Test
    public void constructorExceptsCorrectImplementation() {
        TreeNodeMock treeNode = new TreeNodeMock();
        assertNotNull(treeNode);
    }

    @Test
    public void newTreeNodeHasNoParentOrChildren() {
        TreeNodeMock treeNode = new TreeNodeMock();
        assertEquals(Optional.empty(), treeNode.getParent());
        assertEquals(Collections.emptyList(), treeNode.getChildren());
        assertNotNull(treeNode);
    }

    @Test
    public void getIndexedPathFromRootReturnsEmptyListForRoot() {
        TreeNodeMock root = new TreeNodeMock();
        assertEquals(Collections.emptyList(), root.getIndexedPathFromRoot());
    }

    @Test
    public void getIndexedPathFromRootSimplePath() {
        assertEquals(Arrays.asList(1, 0), getNodeInSimpleTree().getIndexedPathFromRoot());
    }

    @Test
    public void getIndexedPathFromRootComplexPath() {
        assertEquals(Arrays.asList(2, 1, 0), getNodeInComplexTree().getIndexedPathFromRoot());
    }

    @Test
    public void getDescendantSimplePath() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInSimpleTree(root);
        assertEquals(node, root.getDescendant(Arrays.asList(1, 0)).get());
    }

    @Test
    public void getDescendantComplexPath() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);
        assertEquals(node, root.getDescendant(Arrays.asList(2, 1, 0)).get());
    }

    @Test
    public void getDescendantNonExistentReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        getNodeInComplexTree(root);
        assertEquals(Optional.empty(), root.getDescendant(Arrays.asList(1, 100, 0)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getPositionInParentForRootThrowsException() {
        TreeNodeMock root = new TreeNodeMock();
        root.getPositionInParent();
    }

    @Test
    public void getPositionInParentSimpleTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);
        assertEquals(2, node.getPositionInParent());
    }

    @Test
    public void getIndexOfNonExistentChildReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        assertEquals(Optional.empty(), root.getIndexOfChild(new TreeNodeMock()));
    }

    @Test
    public void getIndexOfChild() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);
        assertEquals((Integer)2, root.getIndexOfChild(node).get());
    }

    @Test
    public void getLevelOfRoot() {
        TreeNodeMock root = new TreeNodeMock();
        assertEquals(0, root.getLevel());
    }

    @Test
    public void getLevelInSimpleTree() {
        assertEquals(2, getNodeInSimpleTree().getLevel());
    }

    @Test
    public void getLevelInComplexTree() {
        assertEquals(3, getNodeInComplexTree().getLevel());
    }

    @Test
    public void getChildCountInSimpleTree() {
        TreeNodeMock root = new TreeNodeMock();
        getNodeInSimpleTree(root);
        assertEquals(2, root.getNumberOfChildren());
    }

    @Test
    public void getChildCountInComplexTree() {
        TreeNodeMock root = new TreeNodeMock();
        getNodeInComplexTree(root);
        assertEquals(4, root.getNumberOfChildren());
    }

    @Test
    public void moveToAddsAsLastChildInSimpleTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInSimpleTree(root);
        node.moveTo(root);
        assertEquals((Integer)2, root.getIndexOfChild(node).get());
    }

    @Test
    public void moveToAddsAsLastChildInComplexTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);
        node.moveTo(root);
        assertEquals((Integer)4, root.getIndexOfChild(node).get());
    }

    @Test
    public void moveToChangesParent() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInSimpleTree(root);
        node.moveTo(root);
        assertEquals(root, node.getParent().get());
    }

    @Test
    public void moveToInSameLevelAddsAtEnd() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock child1 = new TreeNodeMock();
        TreeNodeMock child2 = new TreeNodeMock();
        root.addChild(child1);
        root.addChild(child2);

        child1.moveTo(root);

        assertEquals(Arrays.asList(child2, child1), root.getChildren());
    }

    @Test
    public void getPathFromRootInSimpleTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInSimpleTree(root);

        List<TreeNodeMock> path = node.getPathFromRoot();
        assertEquals(3, path.size());
        assertEquals(root, path.get(0));
        assertEquals(node, path.get(2));
    }

    @Test
    public void getPathFromRootInComplexTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);

        List<TreeNodeMock> path = node.getPathFromRoot();
        assertEquals(4, path.size());
        assertEquals(root, path.get(0));
        assertEquals(node, path.get(3));
    }

    @Test
    public void getPreviousSiblingReturnsCorrect() {
        TreeNodeMock root = new TreeNodeMock();
        root.addChild(new TreeNodeMock());
        TreeNodeMock previous = new TreeNodeMock();
        root.addChild(previous);
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        root.addChild(new TreeNodeMock());

        assertEquals(previous, node.getPreviousSibling().get());
    }

    @Test
    public void getPreviousSiblingForRootReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        assertEquals(Optional.empty(), root.getPreviousSibling());
    }

    @Test
    public void getPreviousSiblingForNonexistentReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        assertEquals(Optional.empty(), node.getPreviousSibling());
    }

    @Test
    public void getNextSiblingReturnsCorrect() {
        TreeNodeMock root = new TreeNodeMock();
        root.addChild(new TreeNodeMock());
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        TreeNodeMock next = new TreeNodeMock();
        root.addChild(next);
        root.addChild(new TreeNodeMock());

        assertEquals(next, node.getNextSibling().get());
    }

    @Test
    public void getNextSiblingForRootReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        assertEquals(Optional.empty(), root.getNextSibling());
    }

    @Test
    public void getNextSiblingForNonexistentReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        assertEquals(Optional.empty(), node.getPreviousSibling());
    }

    @Test
    public void getParentReturnsCorrect() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        assertEquals(root, node.getParent().get());
    }

    @Test
    public void getParentForRootReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        assertEquals(Optional.empty(), root.getParent());
    }

    @Test
    public void getChildAtReturnsCorrect() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        assertEquals(node, root.getChildAt(2).get());
    }

    @Test
    public void getChildAtInvalidIndexReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        root.addChild(new TreeNodeMock());
        root.addChild(new TreeNodeMock());
        assertEquals(Optional.empty(), root.getChildAt(10));
    }

    @Test
    public void getRootReturnsTrueForRoot() {
        TreeNodeMock root = new TreeNodeMock();
        assertTrue(root.isRoot());
    }

    @Test
    public void getRootReturnsFalseForChild() {
        assertFalse(getNodeInSimpleTree().isRoot());
    }

    @Test
    public void nodeIsAncestorOfItself() {
        TreeNodeMock root = new TreeNodeMock();
        assertTrue(root.isAncestorOf(root));
    }

    @Test
    public void isAncestorOfInSimpleTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInSimpleTree(root);
        assertTrue(root.isAncestorOf(node));
    }

    @Test
    public void isAncestorOfInComplexTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);
        assertTrue(root.isAncestorOf(node));
    }

    @Test
    public void getRootOfSingleNode() {
        TreeNodeMock root = new TreeNodeMock();
        assertEquals(root, root.getRoot());
    }

    @Test
    public void getRootInSimpleTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInSimpleTree(root);
        assertEquals(root, node.getRoot());
    }

    @Test
    public void getRootInComplexTree() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);
        assertEquals(root, node.getRoot());
    }

    @Test
    public void isLeafIsCorrectForRootWithoutChildren() {
        TreeNodeMock root = new TreeNodeMock();
        assertTrue(root.isLeaf());
    }

    @Test
    public void removeFromParentSetsParentToEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        node.removeFromParent();
        assertEquals(Optional.empty(), node.getParent());
    }

    @Test
    public void removeFromParentRemovesNodeFromChildrenCollection() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        node.removeFromParent();
        assertFalse(root.getChildren().contains(node));
    }

    @Test
    public void removeAllChildrenSetsParentOfChildToEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        root.removeAllChildren();
        assertEquals(Optional.empty(), node.getParent());
    }

    @Test
    public void removeAllChildrenRemovesAllNodesFromChildrenCollection() {
        TreeNodeMock root = new TreeNodeMock();
        getNodeAsChild(root);

        root.removeAllChildren();
        assertEquals(Collections.emptyList(), root.getChildren());
    }

    @Test
    public void getFirstChildAtReturnsCorrect() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);

        assertEquals(node, root.getFirstChild().get());
    }

    @Test
    public void getFirstChildAtLeafReturnsEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock leaf = getNodeAsChild(root);
        assertEquals(Optional.empty(), leaf.getFirstChild());
    }

    @Test
    public void isNodeDescendantInFirstLevel() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock child = getNodeAsChild(root);
        assertTrue(root.isNodeDescendant(child));
    }

    @Test
    public void isNodeDescendantInComplex() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock descendant = getNodeInComplexTree(root);
        assertTrue(root.isNodeDescendant(descendant));
    }

    @Test
    public void getChildrenReturnsAllChildren() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock child1 = new TreeNodeMock();
        TreeNodeMock child2 = new TreeNodeMock();
        root.addChild(child1);
        root.addChild(child2);

        assertEquals(Arrays.asList(child1, child2), root.getChildren());
    }

    @Test
    public void removeChildSetsParentToEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        root.removeChild(node);
        assertEquals(Optional.empty(), node.getParent());
    }

    @Test
    public void removeChildRemovesNodeFromChildrenCollection() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        root.removeChild(node);
        assertFalse(root.getChildren().contains(node));
    }

    @Test
    public void removeChildIndexSetsParentToEmpty() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        root.removeChild(2);
        assertEquals(Optional.empty(), node.getParent());
    }

    @Test
    public void removeChildIndexRemovesNodeFromChildrenCollection() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        root.removeChild(2);
        assertFalse(root.getChildren().contains(node));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addThrowsExceptionIfNodeHasParent() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);
        root.addChild(node);
    }

    @Test
    public void moveAllChildrenToAddsAtSpecifiedPosition() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        TreeNodeMock child1 = new TreeNodeMock();
        TreeNodeMock child2 = new TreeNodeMock();
        node.addChild(child1);
        node.addChild(child2);

        node.moveAllChildrenTo(root, 0);
        assertEquals(Arrays.asList(child1, child2, node), root.getChildren());
    }

    @Test
    public void moveAllChildrenToChangesParent() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        TreeNodeMock child1 = new TreeNodeMock();
        TreeNodeMock child2 = new TreeNodeMock();
        node.addChild(child1);
        node.addChild(child2);

        node.moveAllChildrenTo(root, 0);
        assertEquals(root, child1.getParent().get());
        assertEquals(root, child2.getParent().get());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void moveAllChildrenToDescendantThrowsException() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        root.moveAllChildrenTo(node, 0);
    }

    @Test
    public void sortChildrenSortsInFirstLevel() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock child1 = new TreeNodeMock("a");
        TreeNodeMock child2 = new TreeNodeMock("b");
        TreeNodeMock child3 = new TreeNodeMock("c");
        root.addChild(child2);
        root.addChild(child3);
        root.addChild(child1);

        root.sortChildren((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()), false);
        assertEquals(Arrays.asList(child1, child2, child3), root.getChildren());
    }

    @Test
    public void sortChildrenRecursiveSortsInDeeperLevel() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInSimpleTree(root);
        TreeNodeMock child1 = new TreeNodeMock("a");
        TreeNodeMock child2 = new TreeNodeMock("b");
        TreeNodeMock child3 = new TreeNodeMock("c");
        node.addChild(child2);
        node.addChild(child3);
        node.addChild(child1);

        root.sortChildren((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()), true);
        assertEquals(Arrays.asList(child1, child2, child3), node.getChildren());
    }

    @Test
    public void copySubtreeCopiesChildren() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeAsChild(root);

        TreeNodeMock copiedRoot = root.copySubtree();
        assertEquals(Optional.empty(), copiedRoot.getParent());
        assertFalse(copiedRoot.getChildren().contains(node));
        assertEquals(root.getNumberOfChildren(), copiedRoot.getNumberOfChildren());
    }

    @Test
    public void addChildSomewhereInTreeInvokesChangeEvent() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);

        Consumer<TreeNodeMock> subscriber = (Consumer<TreeNodeMock>) mock(Consumer.class);
        root.subscribeToDescendantChanged(subscriber);

        node.addChild(new TreeNodeMock());
        verify(subscriber).accept(node);
    }

    @Test
    public void moveNodeSomewhereInTreeInvokesChangeEvent() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);
        TreeNodeMock oldParent = node.getParent().get();

        Consumer<TreeNodeMock> subscriber = (Consumer<TreeNodeMock>) mock(Consumer.class);
        root.subscribeToDescendantChanged(subscriber);

        node.moveTo(root);
        verify(subscriber).accept(root);
        verify(subscriber).accept(oldParent);
    }

    @Test
    public void removeChildSomewhereInTreeInvokesChangeEvent() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);
        TreeNodeMock child = node.addChild(new TreeNodeMock());

        Consumer<TreeNodeMock> subscriber = (Consumer<TreeNodeMock>) mock(Consumer.class);
        root.subscribeToDescendantChanged(subscriber);

        node.removeChild(child);
        verify(subscriber).accept(node);
    }

    @Test
    public void removeChildIndexSomewhereInTreeInvokesChangeEvent() {
        TreeNodeMock root = new TreeNodeMock();
        TreeNodeMock node = getNodeInComplexTree(root);
        node.addChild(new TreeNodeMock());

        Consumer<TreeNodeMock> subscriber = (Consumer<TreeNodeMock>) mock(Consumer.class);
        root.subscribeToDescendantChanged(subscriber);

        node.removeChild(0);
        verify(subscriber).accept(node);
    }

    /**
     * This is just a dummy class deriving from TreeNode<T> so that we can test the generic class
     */
    private class TreeNodeMock extends TreeNode<TreeNodeMock> {

        private final String name;

        public TreeNodeMock() {
            this("");
        }

        public TreeNodeMock(String name) {
            super(TreeNodeMock.class);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "TreeNodeMock{" +
                    "name='" + name + '\'' +
                    '}';
        }

        @Override
        public TreeNodeMock copyNode() {
            return new TreeNodeMock(name);
        }
    }

    private class WrongTreeNodeImplementation extends TreeNode<TreeNodeMock> {
        // This class is a wrong derived class of TreeNode<T>
        // since it does not extends TreeNode<WrongTreeNodeImplementation>
        // See test constructorChecksThatClassImplementsCorrectInterface
        public WrongTreeNodeImplementation() {
            super(TreeNodeMock.class);
        }

        @Override
        public TreeNodeMock copyNode() {
            return null;
        }
    }
}
