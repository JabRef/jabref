package org.jabref.gui.icon;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import org.jabref.gui.util.ColorUtil;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

public class InternalMaterialDesignIcon implements JabRefIcon {

    private final List<Ikon> icons;
    private Optional<Color> color;
    private final String unicode;

    public InternalMaterialDesignIcon(Color color, Ikon... icons) {
        this(color, Arrays.asList(icons));
    }

    InternalMaterialDesignIcon(Color color, List<Ikon> icons) {
        this(icons);
        this.color = Optional.of(color);
    }

    public InternalMaterialDesignIcon(Ikon... icons) {
        this(Arrays.asList(icons));
    }

    public InternalMaterialDesignIcon(List<Ikon> icons) {
        this.icons = icons;
        this.unicode = icons.stream().map(icon->icon.getCode()).map(String::valueOf).collect(Collectors.joining());
        this.color = Optional.empty();
    }

    @Override
    public Node getGraphicNode() {

        Ikon icon = icons.get(0);
        FontIcon fontIcon = FontIcon.of(icon);

        Text text = new Text(unicode);
        text.getStyleClass().add("glyph-icon");
        text.setStyle(String.format("-fx-font-family: %s;", fontIcon.getFont().getFamily()));

        color.ifPresent(color -> text.setStyle(text.getStyle() + String.format("-fx-fill: %s;", ColorUtil.toRGBCode(color))));
        return text;
    }

    @Override
    public JabRefIcon disabled() {
        return new InternalMaterialDesignIcon(IconTheme.DEFAULT_DISABLED_COLOR, icons);
    }

    @Override
    public JabRefIcon withColor(Color color) {
        return new InternalMaterialDesignIcon(color, icons);
    }

    @Override
    public String name() {
        return unicode;
    }


    public String getCode() {
        return this.unicode;
    }

    @Override
    public String fontFamily() {
        return "";
    }

    @Override
    public String unicode() {
        return this.unicode;
    }

    @Override
    public Ikon getIkon() {
       return icons.get(0);
    }


}
