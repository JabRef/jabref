package org.jabref.gui.entryeditor;

import java.util.Optional;
import java.util.function.Predicate;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;

/// Pure, stateless DOM traversal helpers shared by {@link EntryEditorFocusUtils}.
final class EntryEditorFocusTraversal {

    private EntryEditorFocusTraversal() {
    }

    static Optional<Node> findFirstTextInputById(Parent parent, String id) {
        return findFirstMatching(parent, node -> node instanceof TextInputControl textInput && id.equalsIgnoreCase(textInput.getId()));
    }

    static Optional<Node> findFirstTextInput(Parent parent) {
        return findFirstMatching(parent, node -> node instanceof TextInputControl);
    }

    static Optional<Node> findFirstFocusableNode(Parent parent) {
        return findFirstMatching(parent, EntryEditorFocusTraversal::isNodeFocusable);
    }

    static Optional<Node> findLastFocusableNode(Parent parent) {
        return findLastMatching(parent, EntryEditorFocusTraversal::isNodeFocusable);
    }

    /// Tries to locate the editor grid (style class {@code "editorPane"}) to avoid including preview
    /// or other sibling panels when determining focus-order boundaries.
    static Optional<Parent> findEditorGridParent(Parent root) {
        if (root.getStyleClass().contains("editorPane")) {
            return Optional.of(root);
        }
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Parent p) {
                Optional<Parent> found = findEditorGridParent(p);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Node> findFirstMatching(Parent parent, Predicate<Node> predicate) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (predicate.test(child)) {
                return Optional.of(child);
            } else if (child instanceof Parent childParent) {
                Optional<Node> found = findFirstMatching(childParent, predicate);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Node> findLastMatching(Parent parent, Predicate<Node> predicate) {
        Optional<Node> last = Optional.empty();
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent childParent) {
                Optional<Node> sub = findLastMatching(childParent, predicate);
                if (sub.isPresent()) {
                    last = sub;
                }
            }
            if (predicate.test(child)) {
                last = Optional.of(child);
            }
        }
        return last;
    }

    private static boolean isNodeFocusable(Node node) {
        return node.isFocusTraversable() && node.isVisible() && !node.isDisabled() && node.isManaged();
    }
}
