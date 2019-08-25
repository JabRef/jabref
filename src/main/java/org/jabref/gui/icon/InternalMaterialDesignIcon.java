package org.jabref.gui.icon;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import org.jabref.gui.util.ColorUtil;

import de.jensd.fx.glyphs.GlyphIcons;

public class InternalMaterialDesignIcon implements JabRefIcon {

    private final List<GlyphIcons> icons;
    private Optional<Color> color;
    private final String unicode;

    public InternalMaterialDesignIcon(Color color, GlyphIcons... icons) {
        this(color, Arrays.asList(icons));
    }

    InternalMaterialDesignIcon(Color color, List<GlyphIcons> icons) {
        this(icons);
        this.color = Optional.of(color);
    }

    public InternalMaterialDesignIcon(GlyphIcons... icons) {
        this(Arrays.asList(icons));
    }

    public InternalMaterialDesignIcon(List<GlyphIcons> icons) {
        this.icons = icons;
        this.unicode = icons.stream().map(GlyphIcons::unicode).collect(Collectors.joining());
        this.color = Optional.empty();
    }

    @Override
    public Node getGraphicNode() {
        GlyphIcons icon = icons.get(0);

        Text text = new Text(unicode);
        text.getStyleClass().add("glyph-icon");
        text.setStyle(String.format("-fx-font-family: %s;", icon.fontFamily()));

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
    public String unicode() {
        return icons.get(0).unicode();
    }

    @Override
    public String fontFamily() {
        return icons.get(0).fontFamily();
    }
}
