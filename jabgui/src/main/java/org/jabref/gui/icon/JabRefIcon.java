package org.jabref.gui.icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

/// Library-agnostic icon contract, with two parallel implementations: {@link IconTheme.JabRefIcons} enum
/// delegates to implementation per glyph.
public sealed interface JabRefIcon permits IconTheme.JabRefIcons, IkonliIcon, SvgIcon {

    Node getGraphicNode();

    boolean matches(Node graphicNode);

    String name();

    JabRefIcon withColor(Color color);

    /// Returns a copy of this icon rendered at {@code size} pixels. Each implementation applies the size to its own
    /// backing node type, so callers (e.g. {@link JabRefIconView}) need not know whether it is a font or SVG node.
    JabRefIcon withSize(int size);

    JabRefIcon disabled();
}
