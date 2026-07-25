package org.jabref.gui.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import javafx.scene.Parent;
import javafx.scene.paint.Paint;

import tools.maran.svgnode.SvgNode;

/// Renders an SVG path as an icon that follows the same theme CSS as Ikonli font icons, so SVG icons need no
/// special styling.
///
/// Bridges three CSS properties (which font icons already honor) onto this node:
/// - `-fx-icon-color` → [#setSvgColor(Paint)]
/// - `-glyph-size` (Ikonli alias used in existing theme CSS) → [#setSize(double)]
/// - `-fx-icon-size` (absolute, e.g. `.action-icon`) → [#setSize(double)]
/// - `-fx-font-size` (em, e.g. `.mainToolbar` at `1.7em`) → [#setSize(double)], resolved
///   against the ambient context font so it matches neighboring font icons
///
/// Tagged with the `glyph-icon` and `ikonli-font-icon` style classes so existing selectors match it.
/// Works whether used bare (via [JabRefIcon#getGraphicNode()], as in the toolbar/menus) or inside a
/// [JabRefIconView]. `-fx-icon-size`, when present, takes precedence over `-fx-font-size`.
public class JabRefSvgIcon extends SvgNode {

    private static final CssMetaData<JabRefSvgIcon, Paint> ICON_COLOR =
            new CssMetaData<>("-fx-icon-color", PaintConverter.getInstance()) {
                @Override
                public boolean isSettable(JabRefSvgIcon node) {
                    return !node.iconColor.isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(JabRefSvgIcon node) {
                    return node.iconColor;
                }
            };

    private static final CssMetaData<JabRefSvgIcon, Number> ICON_SIZE =
            new CssMetaData<>("-fx-icon-size", SizeConverter.getInstance()) {
                @Override
                public boolean isSettable(JabRefSvgIcon node) {
                    return !node.iconSize.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(JabRefSvgIcon node) {
                    return node.iconSize;
                }
            };

    private static final CssMetaData<JabRefSvgIcon, Number> GLYPH_SIZE =
            new CssMetaData<>("-glyph-size", SizeConverter.getInstance()) {
                @Override
                public boolean isSettable(JabRefSvgIcon node) {
                    return !node.glyphSize.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(JabRefSvgIcon node) {
                    return node.glyphSize;
                }
            };

    private static final CssMetaData<JabRefSvgIcon, Number> FONT_SIZE =
            new CssMetaData<>("-fx-font-size", SizeConverter.getInstance()) {
                @Override
                public boolean isSettable(JabRefSvgIcon node) {
                    return !node.fontSize.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(JabRefSvgIcon node) {
                    return node.fontSize;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA;

    static {
        List<CssMetaData<? extends Styleable, ?>> metaData = new ArrayList<>(Parent.getClassCssMetaData());
        metaData.add(ICON_COLOR);
        metaData.add(GLYPH_SIZE);
        metaData.add(ICON_SIZE);
        metaData.add(FONT_SIZE);
        CSS_META_DATA = Collections.unmodifiableList(metaData);
    }

    private final StyleableObjectProperty<Paint> iconColor =
            new SimpleStyleableObjectProperty<>(ICON_COLOR, this, "iconColor") {
                @Override
                protected void invalidated() {
                    Paint color = get();
                    if (color != null) {
                        setSvgColor(color);
                    }
                }
            };

    private final StyleableObjectProperty<Number> iconSize =
            new SimpleStyleableObjectProperty<>(ICON_SIZE, this, "iconSize") {
                @Override
                protected void invalidated() {
                    updateSize();
                }
            };

    private final StyleableObjectProperty<Number> glyphSize =
            new SimpleStyleableObjectProperty<>(GLYPH_SIZE, this, "glyphSize") {
                @Override
                protected void invalidated() {
                    updateSize();
                }
            };

    private final StyleableObjectProperty<Number> fontSize =
            new SimpleStyleableObjectProperty<>(FONT_SIZE, this, "fontSize") {
                @Override
                protected void invalidated() {
                    updateSize();
                }
            };

    public JabRefSvgIcon(String path, double size) {
        super(path, size);
        getStyleClass().addAll("glyph-icon", "ikonli-font-icon");
    }

    /// Applies the CSS-resolved size, preferring an explicit {@code -fx-icon-size} over an em {@code -fx-font-size}.
    /// When neither is set by CSS, the constructor/programmatic size is left untouched.
    private void updateSize() {
        Number size = iconSize.get() != null ? iconSize.get() : glyphSize.get() != null ? glyphSize.get() : fontSize.get();
        if (size != null) {
            setSize(size.doubleValue());
        }
    }

    /// Sets the icon color directly (user origin). Overrides any color coming from a stylesheet, so it is the
    /// right entry point for an explicit, programmatic color (e.g. {@link SvgIcon#withColor}).
    public void setIconColor(Paint color) {
        iconColor.set(color);
    }

    public Paint getIconColor() {
        return iconColor.get();
    }

    public StyleableObjectProperty<Paint> iconColorProperty() {
        return iconColor;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA;
    }
}
