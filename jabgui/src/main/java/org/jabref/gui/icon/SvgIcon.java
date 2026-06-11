package org.jabref.gui.icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// {@link JabRefIcon} backed by an SVG path, rendered as a {@link JabRefSvgNode} via the
/// <a href="https://github.com/Maran23/svgnode">svgnode</a> library. The SVG-backed counterpart to
/// {@link IkonliIcon}; use it for glyphs only available as SVG rather than as an Ikonli font glyph.
@NullMarked
public final class SvgIcon implements JabRefIcon {

    private static final int DEFAULT_SIZE = 24;

    private final String name;
    private final String svgPath;
    private final int size;
    private final @Nullable Color color;

    public SvgIcon(String name, String svgPath) {
        this(name, svgPath, DEFAULT_SIZE, null);
    }

    public SvgIcon(String name, String svgPath, int size) {
        this(name, svgPath, size, null);
    }

    private SvgIcon(String name, String svgPath, int size, @Nullable Color color) {
        this.name = name;
        this.svgPath = svgPath;
        this.size = size;
        this.color = color;
    }

    @Override
    public Node getGraphicNode() {
        JabRefSvgNode node = new JabRefSvgNode(svgPath, size);
        // Explicit color (via withColor/disabled) is a user-origin value, so it wins over theme CSS. When absent,
        // the node's `glyph-icon`/`ikonli-font-icon` classes let theme `-fx-icon-color` rules drive the color.
        if (color != null) {
            node.setIconColor(color);
        }
        return node;
    }

    @Override
    public boolean matches(Node graphicNode) {
        return (graphicNode instanceof JabRefSvgNode svgNode) && svgNode.getPath().equals(svgPath);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public JabRefIcon withColor(Color color) {
        return new SvgIcon(name, svgPath, size, color);
    }

    @Override
    public JabRefIcon withSize(int size) {
        return new SvgIcon(name, svgPath, size, color);
    }

    @Override
    public JabRefIcon disabled() {
        return withColor(IconTheme.DEFAULT_DISABLED_COLOR);
    }
}
