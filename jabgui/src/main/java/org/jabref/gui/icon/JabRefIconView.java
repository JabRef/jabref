package org.jabref.gui.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.PaintConverter;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import org.jabref.gui.icon.IconTheme.JabRefIcons;

import com.tobiasdiez.easybind.EasyBind;

/// View for a {@link JabRefIcons} usable in FXML (e.g. {@code <JabRefIconView glyph="REFRESH"/>}).
///
/// Hosts the backing node produced by {@link JabRefIcons#getGraphicNode()} instead of being a {@code FontIcon}
/// itself, so it renders both Ikonli-font-backed glyphs (as a {@code FontIcon}) and SVG-backed glyphs
/// (see {@link SvgIcon}, rendered as a {@link JabRefSvgIcon}).
///
/// Theme coloring is handled by the hosted child itself (a {@code FontIcon} or {@link JabRefSvgIcon}, both of
/// which carry the icon style classes), so this view stays unclassed to avoid double-theming. The styleable
/// {@code -fx-icon-color} here is an explicit override knob — e.g. an inline {@code style="-fx-icon-color: ..."}
/// in FXML — and, when set, is forwarded to an SVG child as a user-origin color (overriding theme CSS).
public class JabRefIconView extends Group {

    private static final CssMetaData<JabRefIconView, Paint> ICON_COLOR =
            new CssMetaData<>("-fx-icon-color", PaintConverter.getInstance()) {
                @Override
                public boolean isSettable(JabRefIconView node) {
                    return !node.iconColor.isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(JabRefIconView node) {
                    return node.iconColor;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA;

    /// CSS-styleable color, fed by {@code -fx-icon-color} rules (e.g. an inline {@code style="-fx-icon-color: ..."}).
    /// Forwarded to an SVG child as a user-origin color.
    private final StyleableObjectProperty<Paint> iconColor =
            new SimpleStyleableObjectProperty<>(ICON_COLOR, this, "iconColor");

    /// This property is only needed to get proper IDE support in FXML files
    /// (e.g. validation that parameter passed to "icon" is indeed of type {@link IconTheme.JabRefIcons}).
    private final ObjectProperty<IconTheme.JabRefIcons> glyph;
    private final ObjectProperty<Number> glyphSize;

    public JabRefIconView(JabRefIcons icon, int size) {
        this.glyph = new SimpleObjectProperty<>(icon);
        this.glyphSize = new SimpleObjectProperty<>(size);
        initialize();
    }

    public JabRefIconView(IconTheme.JabRefIcons icon) {
        Size size = new Size(1.0, SizeUnits.EM);
        this.glyph = new SimpleObjectProperty<>(icon);
        this.glyphSize = new SimpleObjectProperty<>((int) size.pixels(Font.getDefault()));

        List<CssMetaData<? extends Styleable, ?>> metaData = new ArrayList<>(Group.getClassCssMetaData());
        metaData.add(ICON_COLOR);
        CSS_META_DATA = Collections.unmodifiableList(metaData);

        initialize();
    }

    public JabRefIconView() {
        this(IconTheme.JabRefIcons.ERROR);
    }

    private void initialize() {
        EasyBind.subscribe(glyph, _ -> updateGraphic());
        EasyBind.subscribe(glyphSize, _ -> updateGraphic());
        EasyBind.subscribe(iconColor, _ -> applyColor());
    }

    /// Rebuilds the hosted node from the current glyph, sized to the current glyph size. The icon applies the size
    /// to its own backing node type (font or SVG), so this view stays agnostic to which one it is.
    private void updateGraphic() {
        Node node = glyph.get().withSize(glyphSize.get().intValue()).getGraphicNode();
        getChildren().setAll(node);
        applyColor();
    }

    /// Forwards an explicit {@link #iconColor} override to an SVG child as a user-origin color. When unset
    /// (the common case) the child colors itself from theme CSS, so it is left untouched.
    private void applyColor() {
        Paint color = iconColor.get();
        if ((color != null) && !getChildren().isEmpty() && (getChildren().getFirst() instanceof JabRefSvgIcon svgIcon)) {
            svgIcon.setIconColor(color);
        }
    }

    public IconTheme.JabRefIcons getGlyph() {
        return glyph.get();
    }

    public void setGlyph(IconTheme.JabRefIcons icon) {
        this.glyph.set(icon);
    }

    public ObjectProperty<IconTheme.JabRefIcons> glyphProperty() {
        return glyph;
    }

    public void setGlyphSize(Number value) {
        this.glyphSize.set(value);
    }

    public ObjectProperty<Number> glyphSizeProperty() {
        return glyphSize;
    }

    public Number getGlyphSize() {
        return glyphSize.getValue();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA;
    }
}
