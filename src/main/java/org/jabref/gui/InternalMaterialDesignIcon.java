package org.jabref.gui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.jabref.preferences.JabRefPreferences;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;

public class InternalMaterialDesignIcon implements JabRefIcon {
    private final List<MaterialDesignIcon> icons;
    private final Color color;
    private final String unicode;

    public InternalMaterialDesignIcon(java.awt.Color color, MaterialDesignIcon... icons) {
        this(toFX(color), Arrays.asList(icons));
    }

    public InternalMaterialDesignIcon(Color color, MaterialDesignIcon... icons) {
        this(color, Arrays.asList(icons));
    }

    InternalMaterialDesignIcon(Color color, List<MaterialDesignIcon> icons) {
        this.icons = icons;
        this.color = color;
        this.unicode = icons.stream().map(MaterialDesignIcon::unicode).collect(Collectors.joining());
    }

    public static java.awt.Color toAWT(Color color) {
        return new java.awt.Color((float) color.getRed(),
                (float) color.getGreen(),
                (float) color.getBlue(),
                (float) color.getOpacity());
    }

    public static Color toFX(java.awt.Color awtColor) {
        int r = awtColor.getRed();
        int g = awtColor.getGreen();
        int b = awtColor.getBlue();
        int a = awtColor.getAlpha();
        double opacity = a / 255.0;
        return javafx.scene.paint.Color.rgb(r, g, b, opacity);
    }

    @Override
    public Icon getIcon() {
        return new IconTheme.FontBasedIcon(this.unicode, toAWT(this.color));
    }

    @Override
    public Icon getSmallIcon() {
        return new IconTheme.FontBasedIcon(this.unicode, toAWT(this.color), JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_SMALL));
    }

    @Override
    public Node getGraphicNode() {
        return MaterialDesignIconFactory.get().createIcon(this.icons.get(0));
    }

    @Override
    public JabRefIcon disabled() {
        return new InternalMaterialDesignIcon(toFX(IconTheme.DEFAULT_DISABLED_COLOR), icons);
    }

    public String getCode() {
        return this.unicode;
    }
}
