package org.jabref.gui.icon;

import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import de.jensd.fx.glyphs.GlyphIcon;

public class JabRefIconView extends GlyphIcon<IconTheme.JabRefIcons> {

    /**
     * This property is only needed to get proper IDE support in FXML files
     * (e.g. validation that parameter passed to "icon" is indeed of type {@link IconTheme.JabRefIcons}).
     */
    private ObjectProperty<IconTheme.JabRefIcons> icon;

    public JabRefIconView(IconTheme.JabRefIcons icon, String iconSize) {
        super(IconTheme.JabRefIcons.class);
        this.icon = new SimpleObjectProperty<>(icon);
        setIcon(icon);
        setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;", icon.fontFamily(), iconSize));
    }

    public JabRefIconView(@NamedArg("icon") IconTheme.JabRefIcons icon) {
        this(icon, "1em");
    }

    @Override
    public IconTheme.JabRefIcons getDefaultGlyph() {
        return IconTheme.JabRefIcons.ERROR;
    }
}
