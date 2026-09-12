package org.jabref.gui.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import javafx.scene.paint.Paint;

import tools.maran.svgnode.SvgNode;

/// Renders an SVG path as an icon that follows the same theme CSS as Ikonli font icons, so SVG icons need no
/// special styling.
///
/// Bridges three CSS properties (which font icons already honor) onto this node:
///
/// - `-fx-icon-color` → [#setColor(Paint)]
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
                /// A color set from code -- [SvgIcon#withColor], or an `-fx-icon-color` forwarded by
                /// [JabRefIconView] -- has to outlive the next CSS pass. JavaFX lets an author stylesheet win
                /// over a programmatic value, so the theme's `.ikonli-font-icon { -fx-icon-color }` rule would
                /// otherwise repaint the icon; withholding the property is the way to keep it.
                @Override
                public boolean isSettable(JabRefSvgIcon node) {
                    return node.colorProperty().getStyleOrigin() != StyleOrigin.USER && !node.colorProperty().isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(JabRefSvgIcon node) {
                    return node.colorProperty();
                }
            };

    private static final CssMetaData<JabRefSvgIcon, Number> ICON_SIZE =
            new CssMetaData<>("-fx-icon-size", SizeConverter.getInstance()) {
                @Override
                public boolean isSettable(JabRefSvgIcon node) {
                    return !node.sizeProperty().isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(JabRefSvgIcon node) {
                    return node.sizeProperty();
                }
            };

    private static final CssMetaData<JabRefSvgIcon, Number> GLYPH_SIZE =
            new CssMetaData<>("-glyph-size", SizeConverter.getInstance()) {
                @Override
                public boolean isSettable(JabRefSvgIcon node) {
                    return !node.sizeProperty().isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(JabRefSvgIcon node) {
                    return node.sizeProperty();
                }
            };

    private static final CssMetaData<JabRefSvgIcon, Number> FONT_SIZE =
            new CssMetaData<>("-fx-font-size", SizeConverter.getInstance()) {
                @Override
                public boolean isSettable(JabRefSvgIcon node) {
                    return !node.sizeProperty().isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(JabRefSvgIcon node) {
                    return node.sizeProperty();
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA;

    static {
        List<CssMetaData<? extends Styleable, ?>> metaData = new ArrayList<>(SvgNode.getClassCssMetaData());
        metaData.add(ICON_COLOR);
        metaData.add(GLYPH_SIZE);
        metaData.add(ICON_SIZE);
        metaData.add(FONT_SIZE);
        CSS_META_DATA = Collections.unmodifiableList(metaData);
    }

    public JabRefSvgIcon(String path, double size) {
        super(path, size);
        getStyleClass().addAll("glyph-icon", "ikonli-font-icon");
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA;
    }
}
