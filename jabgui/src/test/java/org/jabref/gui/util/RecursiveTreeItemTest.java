package org.jabref.gui.util;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

import org.jabref.model.TreeNode;
import org.jabref.support.TreeNodeTestData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecursiveTreeItemTest {

    private RecursiveTreeItem<TreeNodeTestData.TreeNodeMock> rootTreeItem;
    private TreeNodeTestData.TreeNodeMock root;
    private ObjectProperty<Predicate<TreeNodeTestData.TreeNodeMock>> filterPredicate;
    private TreeNodeTestData.TreeNodeMock node;

    @BeforeEach
    void setUp() {
        root = new TreeNodeTestData.TreeNodeMock();
        node = TreeNodeTestData.getNodeInSimpleTree(root);
        node.setName("test node");

        filterPredicate = new SimpleObjectProperty<>();

        rootTreeItem = new RecursiveTreeItem<>(root, TreeNode::getChildren, filterPredicate);
    }

    @Test
    void addsAllChildrenNodes() {
        assertEquals(root.getChildren(), rootTreeItem.getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));
    }

    @Test
    void addsAllChildrenOfChildNode() {
        assertEquals(
                root.getChildAt(1).get().getChildren(),
                rootTreeItem.getChildren().get(1).getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));
    }

    @Test
    void respectsFilter() {
        filterPredicate.setValue(item -> item.getName().contains("test"));

        assertEquals(List.of(node.getParent().get()), rootTreeItem.getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));
        assertEquals(
                List.of(node),
                rootTreeItem.getChildren().getFirst().getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));
    }
}
