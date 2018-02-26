package org.jabref.gui.util;

import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

import org.jabref.model.TreeNode;
import org.jabref.model.TreeNodeTestData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class RecursiveTreeItemTest {

    private RecursiveTreeItem<TreeNodeTestData.TreeNodeMock> rootTreeItem;
    private TreeNodeTestData.TreeNodeMock root;
    private ObjectProperty<Predicate<TreeNodeTestData.TreeNodeMock>> filterPredicate;
    private TreeNodeTestData.TreeNodeMock node;

    @BeforeEach
    public void setUp() throws Exception {
        root = new TreeNodeTestData.TreeNodeMock();
        node = TreeNodeTestData.getNodeInSimpleTree(root);
        node.setName("test node");

        filterPredicate = new SimpleObjectProperty<>();

        rootTreeItem = new RecursiveTreeItem<>(root, TreeNode::getChildren, filterPredicate);
    }

    @Test
    public void addsAllChildrenNodes() throws Exception {
        assertEquals(root.getChildren(), rootTreeItem.getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));
    }

    @Test
    public void addsAllChildrenOfChildNode() throws Exception {
        assertEquals(
                root.getChildAt(1).get().getChildren(),
                rootTreeItem.getChildren().get(1).getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));
    }

    @Test
    public void respectsFilter() throws Exception {
        filterPredicate.setValue(item -> item.getName().contains("test"));

        assertEquals(Collections.singletonList(node.getParent().get()), rootTreeItem.getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));
        assertEquals(
                Collections.singletonList(node),
                rootTreeItem.getChildren().get(0).getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));
    }
}
