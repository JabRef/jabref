package org.jabref.gui.icon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import com.tobiasdiez.easybind.EasyBind;
import de.jensd.fx.glyphs.GlyphIcon;

public class JabRefIconView extends GlyphIcon<IconTheme.JabRefIcons> {

    /**
     * This property is only needed to get proper IDE support in FXML files
     * (e.g. validation that parameter passed to "icon" is indeed of type {@link IconTheme.JabRefIcons}).
     */
    private final ObjectProperty<IconTheme.JabRefIcons> glyph;

    public JabRefIconView(IconTheme.JabRefIcons icon, String iconSize) {
        super(IconTheme.JabRefIcons.class);
        this.glyph = new SimpleObjectProperty<>(icon);
        EasyBind.subscribe(glyph, this::setIcon);
        setIcon(icon);
        setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;", icon.fontFamily(), iconSize));
    }

    public JabRefIconView(IconTheme.JabRefIcons icon) {
        this(icon, "1em");
    }

    public JabRefIconView() {
        this(IconTheme.JabRefIcons.ERROR, "1em");
    }

    @Override
    public IconTheme.JabRefIcons getDefaultGlyph() {
        return IconTheme.JabRefIcons.ERROR;
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
}
