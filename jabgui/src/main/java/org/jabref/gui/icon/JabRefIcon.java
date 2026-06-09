package org.jabref.gui.icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

/// Library-agnostic icon contract, with two parallel implementations: {@link IkonliIcon} (backed by an Ikonli
/// font glyph) and {@link SvgIcon} (backed by an SVG path). The {@link IconTheme.JabRefIcons} enum is a third
/// implementation that delegates to one of those two per glyph. Ikonli-specific access ({@code getIkon()}) lives
/// only on {@link IkonliIcon}, not here, so non-Ikonli sources can implement this interface.
public sealed interface JabRefIcon permits IconTheme.JabRefIcons, IkonliIcon, SvgIcon {

    Node getGraphicNode();

    /// Whether {@code graphicNode} (built elsewhere, e.g. set as a control's graphic) is a rendering of this icon.
    /// Used to locate a control by the icon shown on it. Only implementations that can identify their own backing
    /// node type return {@code true}; the default is {@code false}.
    default boolean matches(Node graphicNode) {
        return false;
    }

    String name();

    JabRefIcon withColor(Color color);

    /// Returns a copy of this icon rendered at {@code size} pixels. Each implementation applies the size to its own
    /// backing node type, so callers (e.g. {@link JabRefIconView}) need not know whether it is a font or SVG node.
    JabRefIcon withSize(int size);

    JabRefIcon disabled();
}
