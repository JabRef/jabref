package org.jabref.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.Icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import org.jabref.gui.util.ColorUtil;
import org.jabref.preferences.JabRefPreferences;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;

public class InternalMaterialDesignIcon implements JabRefIcon {
    private final List<MaterialDesignIcon> icons;
    private Optional<Color> color;
    private final String unicode;

    public InternalMaterialDesignIcon(java.awt.Color color, MaterialDesignIcon... icons) {
        this(ColorUtil.toFX(color), Arrays.asList(icons));
    }

    public InternalMaterialDesignIcon(Color color, MaterialDesignIcon... icons) {
        this(color, Arrays.asList(icons));
    }

    InternalMaterialDesignIcon(Color color, List<MaterialDesignIcon> icons) {
        this(icons);
        this.color = Optional.of(color);
    }

    public InternalMaterialDesignIcon(MaterialDesignIcon... icons) {
        this(Arrays.asList(icons));
    }

    public InternalMaterialDesignIcon(List<MaterialDesignIcon> icons) {
        this.icons = icons;
        this.unicode = icons.stream().map(MaterialDesignIcon::unicode).collect(Collectors.joining());
        this.color = Optional.empty();
    }

    @Override
    public Icon getIcon() {
        return new IconTheme.FontBasedIcon(this.unicode, ColorUtil.toAWT(this.color.orElse(IconTheme.getDefaultColor())));
    }

    @Override
    public Icon getSmallIcon() {
        return new IconTheme.FontBasedIcon(this.unicode, ColorUtil.toAWT(this.color.orElse(IconTheme.getDefaultColor())), JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_SMALL));
    }

    @Override
    public Node getGraphicNode() {
        Text icon = MaterialDesignIconFactory.get().createIcon(icons.get(0));
        color.ifPresent(color -> icon.setStyle(icon.getStyle() + String.format("-fx-fill: %s;", ColorUtil.toRGBCode(color))));
        return icon;
    }

    @Override
    public JabRefIcon disabled() {
        return new InternalMaterialDesignIcon(ColorUtil.toFX(IconTheme.DEFAULT_DISABLED_COLOR), icons);
    }

    public String getCode() {
        return this.unicode;
    }
}
