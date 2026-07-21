package org.jabref.gui.preferences;

import javafx.scene.Node;

/// A visible text of a preference tab together with the node it captions, so the preferences
/// search can match the text and highlight the node.
public record SearchableElement(String text, Node node) {
}
