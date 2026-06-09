package org.jabref.gui.icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

/// Library-agnostic icon contract. Implementations may be backed by Ikonli fonts
/// ({@link InternalMaterialDesignIcon}, {@link IconTheme.JabRefIcons}) or by SVG glyphs
/// ({@link SvgIcon}). Ikonli-specific access (e.g. {@code getIkon()}) lives on the concrete
/// font-backed implementations, not here, so non-Ikonli sources can implement this interface.
public interface JabRefIcon {

    Node getGraphicNode();

    String name();

    JabRefIcon withColor(Color color);

    JabRefIcon disabled();
}
