package org.jabref.gui.walkthrough;

import javafx.scene.Node;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.Nullable;

/// Manage listeners and updates for walkthrough effects.
public class WalkthroughUtils {
    /// Check if a node is visible in the scene graph
    public static boolean isNodeVisible(@Nullable Node node) {
        return node != null && NodeHelper.isTreeVisible(node);
    }

    /// Utility method to check if a node cannot be positioned
    public static boolean cannotPositionNode(@Nullable Node node) {
        return node == null || node.getScene() == null || !isNodeVisible(node) || node.getBoundsInLocal().isEmpty();
    }
}
