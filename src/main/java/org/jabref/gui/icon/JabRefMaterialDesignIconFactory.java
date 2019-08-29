package org.jabref.gui.icon;

import javafx.scene.control.Button;
import javafx.scene.text.Text;

import de.jensd.fx.glyphs.GlyphIcon;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;

/**
 Custom Factory class as a workaround for using de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory because of the following issue: https://github.com/JabRef/jabref/issues/5245
 If fixed, use de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory again and delete this class
 */
public class JabRefMaterialDesignIconFactory {

    private static JabRefMaterialDesignIconFactory me;

    private JabRefMaterialDesignIconFactory() { }

    public static JabRefMaterialDesignIconFactory get() {
        if (me == null) {
            me = new JabRefMaterialDesignIconFactory();
        }
        return me;
    }

    public Button createIconButton(GlyphIcons icon) {
        Text label = createIcon(icon, GlyphIcon.DEFAULT_FONT_SIZE);
        Button button = new Button();
        button.setGraphic(label);
        return button;
    }

    public Text createIcon(GlyphIcons icon) {
        return createIcon(icon, GlyphIcon.DEFAULT_FONT_SIZE);
    }

    public Text createIcon(GlyphIcons icon, String iconSize) {
        if (icon instanceof MaterialDesignIcon) {
            // workaround for not using MaterialDesignIconFactory
            return new MaterialDesignIconView((MaterialDesignIcon) icon, iconSize);
        } else {
            // default case copied from GlyphsFactory
            Text text = new Text(icon.unicode());
            text.getStyleClass().add("glyph-icon");
            text.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;", icon.fontFamily(), iconSize));
            return text;
        }
    }
}
