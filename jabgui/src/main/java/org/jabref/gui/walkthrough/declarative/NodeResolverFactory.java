package org.jabref.gui.walkthrough.declarative;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import org.jabref.gui.actions.StandardActions;

/**
 * Factory class for creating different types of node resolvers.
 */
public class NodeResolverFactory {
    private NodeResolverFactory() {
    }

    /**
     * Creates a resolver that finds a node by CSS selector
     *
     * @param selector The CSS selector to find the node
     * @return A function that resolves the node from a Scene
     */
    public static Function<Scene, Optional<Node>> forSelector(String selector) {
        return scene -> Optional.ofNullable(scene.lookup(selector));
    }

    /**
     * Creates a resolver that finds a node by its fx:id
     *
     * @param fxId The fx:id of the node
     * @return A function that resolves the node from a Scene
     */
    public static Function<Scene, Optional<Node>> forFxId(String fxId) {
        return scene -> Optional.ofNullable(scene.lookup("#" + fxId));
    }

    /**
     * Creates a resolver that returns a node from a supplier
     *
     * @param nodeSupplier A supplier that provides the node
     * @return A function that resolves the node from a Scene (ignoring the Scene parameter)
     */
    public static Function<Scene, Optional<Node>> forNodeSupplier(Supplier<Node> nodeSupplier) {
        return _ -> Optional.ofNullable(nodeSupplier.get());
    }

    /**
     * Creates a resolver that finds a node by a predicate
     *
     * @param predicate The predicate to match the node
     * @return A function that resolves the node from a Scene
     */
    public static Function<Scene, Optional<Node>> forPredicate(Predicate<Node> predicate) {
        return scene -> Optional.ofNullable(findNode(scene.getRoot(), predicate));
    }

    /**
     * Creates a resolver that finds a button by its StandardAction
     *
     * @param action The StandardAction associated with the button
     * @return A function that resolves the button from a Scene
     */
    public static Function<Scene, Optional<Node>> forAction(StandardActions action) {
        return scene -> Optional.ofNullable(findNodeByAction(scene, action));
    }

    private static Node findNodeByAction(Scene scene, StandardActions action) {
        return findNode(scene.getRoot(), node -> {
            if (node instanceof Button button) {
                if (button.getTooltip() != null) {
                    String tooltipText = button.getTooltip().getText();
                    if (tooltipText != null && tooltipText.equals(action.getText())) {
                        return true;
                    }
                }

                if (button.getStyleClass().contains("icon-button")) {
                    String actionText = action.getText();
                    if (button.getTooltip() != null && button.getTooltip().getText() != null) {
                        String tooltipText = button.getTooltip().getText();
                        if (tooltipText.startsWith(actionText) || tooltipText.contains(actionText)) {
                            return true;
                        }
                    }

                    return button.getText() != null && button.getText().equals(actionText);
                }
            }
            return false;
        });
    }

    private static Node findNode(Node root, Predicate<Node> predicate) {
        if (predicate.test(root)) {
            return root;
        }

        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node result = findNode(child, predicate);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }
}
