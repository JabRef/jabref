package org.jabref.gui.util;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class NodeTraversalUtils {
    private NodeTraversalUtils() {
    }

    /// First [TextInputControl] in the editor node's subtree (the row-filling text field/area),
    /// or empty for composite editors that have none.
    public static Optional<TextInputControl> findFirstTextInput(Node node) {
        if (node instanceof TextInputControl textInput) {
            return Optional.of(textInput);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Optional<TextInputControl> found = findFirstTextInput(child);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }
}
