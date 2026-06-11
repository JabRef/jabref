package org.jabref.gui.icon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.Group;
import javafx.scene.Node;
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
        initialize();
    }

    public JabRefIconView() {
        this(IconTheme.JabRefIcons.ERROR);
    }

    private void initialize() {
        EasyBind.subscribe(glyph, _ -> updateGraphic());
        EasyBind.subscribe(glyphSize, _ -> updateGraphic());
    }

    /// Rebuilds the hosted node from the current glyph, sized to the current glyph size. The icon applies the size
    /// to its own backing node type (font or SVG), so this view stays agnostic to which one it is.
    private void updateGraphic() {
        Node node = glyph.get().withSize(glyphSize.get().intValue()).getGraphicNode();
        getChildren().setAll(node);
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
}
