package org.jabref.gui.walkthrough;

import javafx.scene.Node;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.Nullable;

public class WalkthroughUtils {
    public static boolean isNodeVisible(@Nullable Node node) {
        return node != null && NodeHelper.isTreeVisible(node);
    }

    public static boolean cannotPositionNode(@Nullable Node node) {
        return node == null || node.getScene() == null || !isNodeVisible(node) || node.getBoundsInLocal().isEmpty();
    }
}
