package org.jabref.gui.icon;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.paint.Color;

/// {@link JabRefIcon} backed by an SVG path via the <a href="https://github.com/Maran23/svgnode">svgnode</a>
/// library, instead of an Ikonli font glyph. Use as a source for toolbar buttons and other graphics where
/// the icon is only available as SVG.
public class SvgIcon implements JabRefIcon {

    private static final int DEFAULT_SIZE = 24;

    private final String name;
    private final String svgPath;
    private final int size;
    private final Optional<Color> color;

    public SvgIcon(String name, String svgPath) {
        this(name, svgPath, DEFAULT_SIZE, Optional.empty());
    }

    public SvgIcon(String name, String svgPath, int size) {
        this(name, svgPath, size, Optional.empty());
    }

    private SvgIcon(String name, String svgPath, int size, Optional<Color> color) {
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
        color.ifPresent(node::setIconColor);
        return node;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public JabRefIcon withColor(Color color) {
        return new SvgIcon(name, svgPath, size, Optional.of(color));
    }

    @Override
    public JabRefIcon disabled() {
        return new SvgIcon(name, svgPath, size, Optional.of(IconTheme.DEFAULT_DISABLED_COLOR));
    }
}
