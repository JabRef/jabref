package org.jabref.gui.icon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.Size;
import javafx.css.SizeUnits;

import org.jabref.gui.icon.IconTheme.JabRefIcons;

import com.tobiasdiez.easybind.EasyBind;
import org.kordamp.ikonli.javafx.FontIcon;

public class JabRefIconView extends FontIcon {

    /**
     * This property is only needed to get proper IDE support in FXML files
     * (e.g. validation that parameter passed to "icon" is indeed of type {@link IconTheme.JabRefIcons}).
     */
    private final ObjectProperty<IconTheme.JabRefIcons> glyph;
    private final ObjectProperty<Number> glyphSize;

    public JabRefIconView(JabRefIcons icon, int size) {
        super(icon.getIkon());
        this.glyph = new SimpleObjectProperty<>(icon);
        this.glyphSize = new SimpleObjectProperty<>(size);

        EasyBind.subscribe(glyph, glyph -> setIconCode(glyph.getIkon()));
        EasyBind.subscribe(glyphSize, glyphsize -> setIconSize(glyphsize.intValue()));
    }

    public JabRefIconView(IconTheme.JabRefIcons icon) {

        super(icon.getIkon());
        Size size = new Size(1.0, SizeUnits.EM);
        this.glyph = new SimpleObjectProperty<>(icon);
        this.glyphSize = new SimpleObjectProperty<>(9);

        int px = (int) size.pixels(getFont());
        glyphSize.set(px);
        EasyBind.subscribe(glyph, glyph -> setIconCode(glyph.getIkon()));
        EasyBind.subscribe(glyphSize, glyphsize -> setIconSize(glyphsize.intValue()));
    }

    public JabRefIconView() {
        this(IconTheme.JabRefIcons.ERROR);
    }

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
