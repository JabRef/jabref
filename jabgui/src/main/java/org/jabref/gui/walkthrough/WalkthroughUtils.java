package org.jabref.gui.walkthrough;

import javafx.scene.Node;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.Nullable;

public class WalkthroughUtils {
    /// Check if a node is visible in the scene graph using the
    /// [NodeHelper#isTreeVisible(Node)] method, which ensures that:
    /// 1. Window is visible
    /// 2. Node, and all its parents, are visible
    public static boolean isNodeVisible(@Nullable Node node) {
        return node != null && NodeHelper.isTreeVisible(node);
    }

    /// Utility method to check if a node cannot be positioned.
    /// A node cannot be positioned if:
    /// 1. It's not visible [#isNodeVisible(Node)]
    /// 2. Or, it's size is non-existent
    public static boolean cannotPositionNode(@Nullable Node node) {
        return node == null || node.getScene() == null || !isNodeVisible(node) || node.getBoundsInLocal().isEmpty();
    }
}
