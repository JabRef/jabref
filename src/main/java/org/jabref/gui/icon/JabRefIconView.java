package org.jabref.gui.icon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class JabRefIconView  {

    /**
     * This property is only needed to get proper IDE support in FXML files
     * (e.g. validation that parameter passed to "icon" is indeed of type {@link IconTheme.JabRefIcons}).
     */
    private final ObjectProperty<IconTheme.JabRefIcons> glyph;

    public JabRefIconView(IconTheme.JabRefIcons icon, String iconSize) {
        this.glyph = new SimpleObjectProperty<>(icon);
    }

    public JabRefIconView(IconTheme.JabRefIcons icon) {
        this(icon, "1em");
    }

    public JabRefIconView() {
        this(IconTheme.JabRefIcons.ERROR, "1em");
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
}
