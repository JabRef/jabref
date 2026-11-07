package org.jabref.gui.preferences;

import javafx.scene.Node;

import org.jspecify.annotations.NullMarked;

/// A visible text of a preference tab together with the node it captions, so the preferences
/// search can match the text and highlight the node.
@NullMarked
public record SearchableElement(String text, Node node) {
}
