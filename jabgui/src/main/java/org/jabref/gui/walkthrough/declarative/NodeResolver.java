package org.jabref.gui.walkthrough.declarative;

import java.util.Optional;
import java.util.function.Predicate;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuItem;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.logic.l10n.Localization;

import com.google.common.collect.Streams;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.control.LabeledText;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/// Resolves nodes from a Scene
@FunctionalInterface
public interface NodeResolver {
    /// Resolves a node from the given scene.
    ///
    /// @param scene the scene to search in
    /// @return an optional containing the found node, or empty if not found
    Optional<Node> resolve(@NonNull Scene scene);

    /// Creates a resolver that finds a node by CSS selector. The returned node is
    /// guaranteed to be visible.
    ///
    /// @param selector the CSS selector to find the node
    /// @return a resolver that finds the node by selector
    static NodeResolver selector(@NonNull String selector) {
        return scene -> scene.getRoot().lookupAll(selector).stream().filter(NodeResolver::isVisible).findFirst();
    }

    /// Creates a resolver that finds a node by its fx:id. The returned node is
    /// guaranteed to be visible.
    ///
    /// @param fxId the fx:id of the node
    /// @return a resolver that finds the node by fx:id
    static NodeResolver fxId(@NonNull String fxId) {
        return selector("#" + fxId);
    }

    /// Creates a resolver that finds a button by its graphic. The returned button is
    /// guaranteed to be visible.
    ///
    /// @param glyph the graphic of the button
    /// @return a resolver that finds the button by graphic
    static NodeResolver buttonWithGraphic(IconTheme.JabRefIcons glyph) {
        return scene -> Streams
                // .icon-button, .button selector is not used, because lookupAll doesn't support multiple selectors
                .concat(scene.getRoot().lookupAll(".button").stream(),
                        scene.getRoot().lookupAll(".icon-button").stream())
                .filter(node -> {
                    if (!(node instanceof ButtonBase button) || !NodeResolver.isVisible(button)) {
                        return false;
                    }
                    Node graphic = button.getGraphic();
                    return (graphic instanceof JabRefIconView jabRefIconView) && jabRefIconView.getGlyph() == glyph ||
                            (graphic instanceof FontIcon fontIcon) && fontIcon.getIconCode() == glyph.getIkon();
                })
                .findFirst();
    }

    /// Creates a resolver that finds a node by a predicate. The returned node is
    /// guaranteed to be visible.
    ///
    /// @param predicate the predicate to match the node
    /// @return a resolver that finds the node matching the predicate
    static NodeResolver predicate(@NonNull Predicate<Node> predicate) {
        return scene -> Optional.ofNullable(findNode(scene.getRoot(),
                node -> NodeResolver.isVisible(node) && predicate.test(node)));
    }

    /// Creates a resolver that finds a button by its StandardAction. The button is
    /// matched by its tooltip text or button text. The returned button is guaranteed to
    /// be visible.
    ///
    /// @param action the StandardAction associated with the button
    /// @return a resolver that finds the button by action
    static NodeResolver action(@NonNull StandardActions action) {
        return scene -> Optional.ofNullable(findNode(scene.getRoot(), node -> {
            if (!(node instanceof ButtonBase button) || !NodeResolver.isVisible(button)) {
                return false;
            }

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

            return false;
        }));
    }

    /// Creates a resolver that finds a button by its button type, assuming the node
    /// resolved is a [javafx.scene.control.DialogPane].
    ///
    /// @param buttonType the button type to find
    /// @return a resolver that finds the button by button type
    static NodeResolver buttonType(@NonNull ButtonType buttonType) {
        return scene -> predicate(DialogPane.class::isInstance)
                .resolve(scene)
                .map(node -> node instanceof DialogPane pane ? pane.lookupButton(buttonType) : null);
    }

    /// Creates a resolver that finds a node by selector first, then matches text
    /// content in the node and the node's LabeledText children. The returned node is
    /// guaranteed to be visible.
    ///
    /// @param selector    the style class to match
    /// @param textMatcher predicate to match text content in LabeledText children
    /// @return a resolver that finds the node by style class and text content
    static NodeResolver selectorWithText(@NonNull String selector, @NonNull Predicate<String> textMatcher) {
        return scene -> scene
                .getRoot()
                .lookupAll(selector)
                .stream()
                .filter(NodeHelper::isTreeVisible)
                .filter(node -> textMatcher.test(node.toString()) || node.lookupAll(".text").stream().anyMatch(child -> {
                            if (child instanceof LabeledText text) {
                                String textContent = text.getText();
                                return textContent != null && textMatcher.test(textContent);
                            }
                            return false;
                        }
                )).findFirst();
    }

    /// Creates a resolver that finds a menu item by its language key.
    ///
    /// @param key the language key of the menu item
    /// @return a resolver that finds the menu item by language key
    static NodeResolver menuItem(@NonNull String key) {
        return scene -> {
            if (!(scene.getWindow() instanceof ContextMenu menu)) {
                return Optional.empty();
            }

            if (!menu.isShowing()) {
                return Optional.empty();
            }

            return menu.getItems().stream()
                       .filter(item -> NodeResolver.isVisible(item.getStyleableNode()))
                       .filter(item -> Optional
                               .ofNullable(item.getText())
                               .map(str -> str.contains(Localization.lang(key)))
                               .orElse(false))
                       .map(MenuItem::getStyleableNode).findFirst();
        };
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

    private static boolean isVisible(@Nullable Node node) {
        return node != null && NodeHelper.isTreeVisible(node);
    }
}
