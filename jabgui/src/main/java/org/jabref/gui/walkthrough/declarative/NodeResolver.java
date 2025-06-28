package org.jabref.gui.walkthrough.declarative;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;

import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Resolves nodes from a Scene
 */
@FunctionalInterface
public interface NodeResolver {
    /**
     * Resolves a node from the given scene.
     *
     * @param scene the scene to search in
     * @return an optional containing the found node, or empty if not found
     */
    Optional<Node> resolve(@NonNull Scene scene);

    /**
     * Creates a resolver that finds a node by CSS selector.
     *
     * @param selector the CSS selector to find the node
     * @return a resolver that finds the node by selector
     */
    static NodeResolver selector(@NonNull String selector) {
        return scene -> Optional.ofNullable(scene.lookup(selector));
    }

    /**
     * Creates a resolver that finds a node by its fx:id.
     *
     * @param fxId the fx:id of the node
     * @return a resolver that finds the node by fx:id
     */
    static NodeResolver fxId(@NonNull String fxId) {
        return scene -> Optional.ofNullable(scene.lookup("#" + fxId));
    }

    /**
     * Creates a resolver that finds a node by a predicate.
     *
     * @param predicate the predicate to match the node
     * @return a resolver that finds the node matching the predicate
     */
    static NodeResolver predicate(@NonNull Predicate<Node> predicate) {
        return scene -> Optional.ofNullable(findNode(scene.getRoot(), predicate));
    }

    /**
     * Creates a resolver that finds a button by its StandardAction.
     *
     * @param action the StandardAction associated with the button
     * @return a resolver that finds the button by action
     */
    static NodeResolver action(@NonNull StandardActions action) {
        return scene -> Optional.ofNullable(findNodeByAction(scene, action));
    }

    /**
     * Creates a resolver that finds a menu item by its language key.
     *
     * @param key the language key of the menu item
     * @return a resolver that finds the menu item by language key
     */
    static NodeResolver menuItem(@NonNull String key) {
        return scene -> {
            if (!(scene.getWindow() instanceof ContextMenu menu)) {
                return Optional.empty();
            }

            if (!menu.isShowing()) {
                return Optional.empty();
            }

            return menu.getItems().stream()
                       .filter(item -> Optional
                               .ofNullable(item.getText())
                               .map(str -> str.contains(Localization.lang(key)))
                               .orElse(false))
                       .flatMap(item -> Stream
                               .iterate(item.getGraphic(), Objects::nonNull, Node::getParent)
                               .filter(node -> node.getStyleClass().contains("menu-item"))
                               .findFirst().stream()
                       ).findFirst();
        };
    }

    @Nullable
    private static Node findNodeByAction(@NonNull Scene scene, @NonNull StandardActions action) {
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

    @Nullable
    private static Node findNode(@NonNull Node root, @NonNull Predicate<Node> predicate) {
        if (predicate.test(root)) {
            return root;
        }

        if (root instanceof Parent parent) {
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
