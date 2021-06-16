package org.jabref.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TreeNodeTest {

    Consumer<TreeNodeTestData.TreeNodeMock> subscriber;

    @BeforeEach
    public void setUp() {
        subscriber = mock(Consumer.class);
    }

    @Test
    public void constructorChecksThatClassImplementsCorrectInterface() {
        assertThrows(UnsupportedOperationException.class, () -> new WrongTreeNodeImplementation());
    }

    @Test
    public void constructorExceptsCorrectImplementation() {
        TreeNodeTestData.TreeNodeMock treeNode = new TreeNodeTestData.TreeNodeMock();
        assertNotNull(treeNode);
    }

    @Test
    public void newTreeNodeHasNoParentOrChildren() {
        TreeNodeTestData.TreeNodeMock treeNode = new TreeNodeTestData.TreeNodeMock();
        assertEquals(Optional.empty(), treeNode.getParent());
        assertEquals(Collections.emptyList(), treeNode.getChildren());
        assertNotNull(treeNode);
    }

    @Test
    public void getIndexedPathFromRootReturnsEmptyListForRoot() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertEquals(Collections.emptyList(), root.getIndexedPathFromRoot());
    }

    @Test
    public void getIndexedPathFromRootSimplePath() {
        assertEquals(Arrays.asList(1, 0), TreeNodeTestData.getNodeInSimpleTree().getIndexedPathFromRoot());
    }

    @Test
    public void getIndexedPathFromRootComplexPath() {
        assertEquals(Arrays.asList(2, 1, 0), TreeNodeTestData.getNodeInComplexTree().getIndexedPathFromRoot());
    }

    @Test
    public void getDescendantSimplePath() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInSimpleTree(root);
        assertEquals(node, root.getDescendant(Arrays.asList(1, 0)).get());
    }

    @Test
    public void getDescendantComplexPath() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);
        assertEquals(node, root.getDescendant(Arrays.asList(2, 1, 0)).get());
    }

    @Test
    public void getDescendantNonExistentReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.getNodeInComplexTree(root);
        assertEquals(Optional.empty(), root.getDescendant(Arrays.asList(1, 100, 0)));
    }

    @Test
    public void getPositionInParentForRootThrowsException() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertThrows(UnsupportedOperationException.class, () -> root.getPositionInParent());
    }

    @Test
    public void getPositionInParentSimpleTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);
        assertEquals(2, node.getPositionInParent());
    }

    @Test
    public void getIndexOfNonExistentChildReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertEquals(Optional.empty(), root.getIndexOfChild(new TreeNodeTestData.TreeNodeMock()));
    }

    @Test
    public void getIndexOfChild() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);
        assertEquals((Integer) 2, root.getIndexOfChild(node).get());
    }

    @Test
    public void getLevelOfRoot() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertEquals(0, root.getLevel());
    }

    @Test
    public void getLevelInSimpleTree() {
        assertEquals(2, TreeNodeTestData.getNodeInSimpleTree().getLevel());
    }

    @Test
    public void getLevelInComplexTree() {
        assertEquals(3, TreeNodeTestData.getNodeInComplexTree().getLevel());
    }

    @Test
    public void getChildCountInSimpleTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.getNodeInSimpleTree(root);
        assertEquals(2, root.getNumberOfChildren());
    }

    @Test
    public void getChildCountInComplexTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.getNodeInComplexTree(root);
        assertEquals(4, root.getNumberOfChildren());
    }

    @Test
    public void moveToAddsAsLastChildInSimpleTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInSimpleTree(root);
        node.moveTo(root);
        assertEquals((Integer) 2, root.getIndexOfChild(node).get());
    }

    @Test
    public void moveToAddsAsLastChildInComplexTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);
        node.moveTo(root);
        assertEquals((Integer) 4, root.getIndexOfChild(node).get());
    }

    @Test
    public void moveToChangesParent() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInSimpleTree(root);
        node.moveTo(root);
        assertEquals(root, node.getParent().get());
    }

    @Test
    public void moveToInSameLevelAddsAtEnd() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child1 = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child2 = new TreeNodeTestData.TreeNodeMock();
        root.addChild(child1);
        root.addChild(child2);

        child1.moveTo(root);

        assertEquals(Arrays.asList(child2, child1), root.getChildren());
    }

    @Test
    public void moveToInSameLevelWhenNodeWasBeforeTargetIndex() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child1 = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child2 = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child3 = new TreeNodeTestData.TreeNodeMock();
        root.addChild(child1);
        root.addChild(child2);
        root.addChild(child3);

        child1.moveTo(root, 1);

        assertEquals(Arrays.asList(child2, child1, child3), root.getChildren());
    }

    @Test
    public void moveToInSameLevelWhenNodeWasAfterTargetIndex() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child1 = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child2 = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child3 = new TreeNodeTestData.TreeNodeMock();
        root.addChild(child1);
        root.addChild(child2);
        root.addChild(child3);

        child3.moveTo(root, 1);

        assertEquals(Arrays.asList(child1, child3, child2), root.getChildren());
    }

    @Test
    public void getPathFromRootInSimpleTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInSimpleTree(root);

        List<TreeNodeTestData.TreeNodeMock> path = node.getPathFromRoot();
        assertEquals(3, path.size());
        assertEquals(root, path.get(0));
        assertEquals(node, path.get(2));
    }

    @Test
    public void getPathFromRootInComplexTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);

        List<TreeNodeTestData.TreeNodeMock> path = node.getPathFromRoot();
        assertEquals(4, path.size());
        assertEquals(root, path.get(0));
        assertEquals(node, path.get(3));
    }

    @Test
    public void getPreviousSiblingReturnsCorrect() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        root.addChild(new TreeNodeTestData.TreeNodeMock());
        TreeNodeTestData.TreeNodeMock previous = new TreeNodeTestData.TreeNodeMock();
        root.addChild(previous);
        TreeNodeTestData.TreeNodeMock node = new TreeNodeTestData.TreeNodeMock();
        root.addChild(node);
        root.addChild(new TreeNodeTestData.TreeNodeMock());

        assertEquals(previous, node.getPreviousSibling().get());
    }

    @Test
    public void getPreviousSiblingForRootReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertEquals(Optional.empty(), root.getPreviousSibling());
    }

    @Test
    public void getPreviousSiblingForNonexistentReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = new TreeNodeTestData.TreeNodeMock();
        root.addChild(node);
        assertEquals(Optional.empty(), node.getPreviousSibling());
    }

    @Test
    public void getNextSiblingReturnsCorrect() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        root.addChild(new TreeNodeTestData.TreeNodeMock());
        TreeNodeTestData.TreeNodeMock node = new TreeNodeTestData.TreeNodeMock();
        root.addChild(node);
        TreeNodeTestData.TreeNodeMock next = new TreeNodeTestData.TreeNodeMock();
        root.addChild(next);
        root.addChild(new TreeNodeTestData.TreeNodeMock());

        assertEquals(next, node.getNextSibling().get());
    }

    @Test
    public void getNextSiblingForRootReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertEquals(Optional.empty(), root.getNextSibling());
    }

    @Test
    public void getNextSiblingForNonexistentReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = new TreeNodeTestData.TreeNodeMock();
        root.addChild(node);
        assertEquals(Optional.empty(), node.getPreviousSibling());
    }

    @Test
    public void getParentReturnsCorrect() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        assertEquals(root, node.getParent().get());
    }

    @Test
    public void getParentForRootReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertEquals(Optional.empty(), root.getParent());
    }

    @Test
    public void getChildAtReturnsCorrect() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        assertEquals(node, root.getChildAt(2).get());
    }

    @Test
    public void getChildAtInvalidIndexReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        root.addChild(new TreeNodeTestData.TreeNodeMock());
        root.addChild(new TreeNodeTestData.TreeNodeMock());
        assertEquals(Optional.empty(), root.getChildAt(10));
    }

    @Test
    public void getRootReturnsTrueForRoot() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertTrue(root.isRoot());
    }

    @Test
    public void getRootReturnsFalseForChild() {
        assertFalse(TreeNodeTestData.getNodeInSimpleTree().isRoot());
    }

    @Test
    public void nodeIsAncestorOfItself() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertTrue(root.isAncestorOf(root));
    }

    @Test
    public void isAncestorOfInSimpleTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInSimpleTree(root);
        assertTrue(root.isAncestorOf(node));
    }

    @Test
    public void isAncestorOfInComplexTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);
        assertTrue(root.isAncestorOf(node));
    }

    @Test
    public void getRootOfSingleNode() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertEquals(root, root.getRoot());
    }

    @Test
    public void getRootInSimpleTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInSimpleTree(root);
        assertEquals(root, node.getRoot());
    }

    @Test
    public void getRootInComplexTree() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);
        assertEquals(root, node.getRoot());
    }

    @Test
    public void isLeafIsCorrectForRootWithoutChildren() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        assertTrue(root.isLeaf());
    }

    @Test
    public void removeFromParentSetsParentToEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        node.removeFromParent();
        assertEquals(Optional.empty(), node.getParent());
    }

    @Test
    public void removeFromParentRemovesNodeFromChildrenCollection() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        node.removeFromParent();
        assertFalse(root.getChildren().contains(node));
    }

    @Test
    public void removeAllChildrenSetsParentOfChildToEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        root.removeAllChildren();
        assertEquals(Optional.empty(), node.getParent());
    }

    @Test
    public void removeAllChildrenRemovesAllNodesFromChildrenCollection() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.getNodeAsChild(root);

        root.removeAllChildren();
        assertEquals(Collections.emptyList(), root.getChildren());
    }

    @Test
    public void getFirstChildAtReturnsCorrect() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = new TreeNodeTestData.TreeNodeMock();
        root.addChild(node);

        assertEquals(node, root.getFirstChild().get());
    }

    @Test
    public void getFirstChildAtLeafReturnsEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock leaf = TreeNodeTestData.getNodeAsChild(root);
        assertEquals(Optional.empty(), leaf.getFirstChild());
    }

    @Test
    public void isNodeDescendantInFirstLevel() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child = TreeNodeTestData.getNodeAsChild(root);
        assertTrue(root.isNodeDescendant(child));
    }

    @Test
    public void isNodeDescendantInComplex() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock descendant = TreeNodeTestData.getNodeInComplexTree(root);
        assertTrue(root.isNodeDescendant(descendant));
    }

    @Test
    public void getChildrenReturnsAllChildren() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child1 = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child2 = new TreeNodeTestData.TreeNodeMock();
        root.addChild(child1);
        root.addChild(child2);

        assertEquals(Arrays.asList(child1, child2), root.getChildren());
    }

    @Test
    public void removeChildSetsParentToEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        root.removeChild(node);
        assertEquals(Optional.empty(), node.getParent());
    }

    @Test
    public void removeChildRemovesNodeFromChildrenCollection() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        root.removeChild(node);
        assertFalse(root.getChildren().contains(node));
    }

    @Test
    public void removeChildIndexSetsParentToEmpty() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        root.removeChild(2);
        assertEquals(Optional.empty(), node.getParent());
    }

    @Test
    public void removeChildIndexRemovesNodeFromChildrenCollection() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        root.removeChild(2);
        assertFalse(root.getChildren().contains(node));
    }

    @Test
    public void addThrowsExceptionIfNodeHasParent() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);
        assertThrows(UnsupportedOperationException.class, () -> root.addChild(node));
    }

    @Test
    public void moveAllChildrenToAddsAtSpecifiedPosition() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = new TreeNodeTestData.TreeNodeMock();
        root.addChild(node);
        TreeNodeTestData.TreeNodeMock child1 = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child2 = new TreeNodeTestData.TreeNodeMock();
        node.addChild(child1);
        node.addChild(child2);

        node.moveAllChildrenTo(root, 0);
        assertEquals(Arrays.asList(child1, child2, node), root.getChildren());
    }

    @Test
    public void moveAllChildrenToChangesParent() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = new TreeNodeTestData.TreeNodeMock();
        root.addChild(node);
        TreeNodeTestData.TreeNodeMock child1 = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child2 = new TreeNodeTestData.TreeNodeMock();
        node.addChild(child1);
        node.addChild(child2);

        node.moveAllChildrenTo(root, 0);
        assertEquals(root, child1.getParent().get());
        assertEquals(root, child2.getParent().get());
    }

    @Test
    public void moveAllChildrenToDescendantThrowsException() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        assertThrows(UnsupportedOperationException.class, () -> root.moveAllChildrenTo(node, 0));
    }

    @Test
    public void sortChildrenSortsInFirstLevel() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock child1 = new TreeNodeTestData.TreeNodeMock("a");
        TreeNodeTestData.TreeNodeMock child2 = new TreeNodeTestData.TreeNodeMock("b");
        TreeNodeTestData.TreeNodeMock child3 = new TreeNodeTestData.TreeNodeMock("c");
        root.addChild(child2);
        root.addChild(child3);
        root.addChild(child1);

        root.sortChildren((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()), false);
        assertEquals(Arrays.asList(child1, child2, child3), root.getChildren());
    }

    @Test
    public void sortChildrenRecursiveSortsInDeeperLevel() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInSimpleTree(root);
        TreeNodeTestData.TreeNodeMock child1 = new TreeNodeTestData.TreeNodeMock("a");
        TreeNodeTestData.TreeNodeMock child2 = new TreeNodeTestData.TreeNodeMock("b");
        TreeNodeTestData.TreeNodeMock child3 = new TreeNodeTestData.TreeNodeMock("c");
        node.addChild(child2);
        node.addChild(child3);
        node.addChild(child1);

        root.sortChildren((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()), true);
        assertEquals(Arrays.asList(child1, child2, child3), node.getChildren());
    }

    @Test
    public void copySubtreeCopiesChildren() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeAsChild(root);

        TreeNodeTestData.TreeNodeMock copiedRoot = root.copySubtree();
        assertEquals(Optional.empty(), copiedRoot.getParent());
        assertFalse(copiedRoot.getChildren().contains(node));
        assertEquals(root.getNumberOfChildren(), copiedRoot.getNumberOfChildren());
    }

    @Test
    public void addChildSomewhereInTreeInvokesChangeEvent() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);

        root.subscribeToDescendantChanged(subscriber);

        node.addChild(new TreeNodeTestData.TreeNodeMock());
        verify(subscriber).accept(node);
    }

    @Test
    public void moveNodeSomewhereInTreeInvokesChangeEvent() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);
        TreeNodeTestData.TreeNodeMock oldParent = node.getParent().get();

        root.subscribeToDescendantChanged(subscriber);

        node.moveTo(root);
        verify(subscriber).accept(root);
        verify(subscriber).accept(oldParent);
    }

    @Test
    public void removeChildSomewhereInTreeInvokesChangeEvent() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);
        TreeNodeTestData.TreeNodeMock child = node.addChild(new TreeNodeTestData.TreeNodeMock());

        root.subscribeToDescendantChanged(subscriber);

        node.removeChild(child);
        verify(subscriber).accept(node);
    }

    @Test
    public void removeChildIndexSomewhereInTreeInvokesChangeEvent() {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock();
        TreeNodeTestData.TreeNodeMock node = TreeNodeTestData.getNodeInComplexTree(root);
        node.addChild(new TreeNodeTestData.TreeNodeMock());

        root.subscribeToDescendantChanged(subscriber);

        node.removeChild(0);
        verify(subscriber).accept(node);
    }

    @Test
    public void findChildrenWithSameName() throws Exception {
        TreeNodeTestData.TreeNodeMock root = new TreeNodeTestData.TreeNodeMock("A");
        TreeNodeTestData.TreeNodeMock childB = root.addChild(new TreeNodeTestData.TreeNodeMock("B"));
        TreeNodeTestData.TreeNodeMock node = childB.addChild(new TreeNodeTestData.TreeNodeMock("A"));
        TreeNodeTestData.TreeNodeMock childA = root.addChild(new TreeNodeTestData.TreeNodeMock("A"));

        assertEquals(Arrays.asList(root, node, childA), root.findChildrenSatisfying(treeNode -> treeNode.getName().equals("A")));
    }

    private static class WrongTreeNodeImplementation extends TreeNode<TreeNodeTestData.TreeNodeMock> {

        // This class is a wrong derived class of TreeNode<T>
        // since it does not extends TreeNode<WrongTreeNodeImplementation>
        // See test constructorChecksThatClassImplementsCorrectInterface
        public WrongTreeNodeImplementation() {
            super(TreeNodeTestData.TreeNodeMock.class);
        }

        @Override
        public TreeNodeTestData.TreeNodeMock copyNode() {
            return null;
        }
    }
}
