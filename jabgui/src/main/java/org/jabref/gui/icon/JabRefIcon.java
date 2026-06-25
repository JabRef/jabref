package org.jabref.gui.icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;

/// Library-agnostic icon contract, with two parallel implementations: {@link IconTheme.JabRefIcons} enum
/// delegates to implementation per glyph.
@NullMarked
public sealed interface JabRefIcon permits IconTheme.JabRefIcons, IkonliIcon, SvgIcon {

    Node getGraphicNode();

    boolean matches(Node graphicNode);

    String name();

    JabRefIcon withColor(Color color);

    /// Returns a copy of this icon rendered at {@code size} pixels. Each implementation applies the size to its own
    /// backing node type, so callers (e.g. {@link JabRefIconView}) need not know whether it is a font or SVG node.
    ///
    /// Note the unit differs by backing type: for a font glyph {@code size} is the em/font size, so the glyph sits
    /// inside that box with the font's intrinsic padding; for an SVG the path is fitted edge-to-edge into a
    /// {@code size}×{@code size} square. The same {@code size} can thus render an SVG slightly larger than the font
    /// glyph.
    JabRefIcon withSize(int size);

    JabRefIcon disabled();
}
