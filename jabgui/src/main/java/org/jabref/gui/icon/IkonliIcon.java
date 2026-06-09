package org.jabref.gui.icon;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.jabref.gui.util.ColorUtil;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonProvider;
import org.kordamp.ikonli.javafx.FontIcon;

/// {@link JabRefIcon} backed by an <a href="https://kordamp.org/ikonli/">Ikonli</a> font glyph, rendered as a
/// {@link FontIcon}. The font-backed counterpart to {@link SvgIcon}; unlike SVG icons it exposes its underlying
/// {@link Ikon} via {@link #getIkon()}. Immutable: {@link #withColor} and {@link #withSize} return copies.
public class IkonliIcon implements JabRefIcon {

    /// All {@link Ikon} values discovered via the {@link IkonProvider} service loader, used by {@link #findIcon}.
    /// Lazily populated on first lookup.
    private static final Set<Ikon> ALL_IKONS = new HashSet<>();

    private final List<Ikon> icons;
    private final Optional<Color> color;
    private final Optional<Integer> size;
    private final String unicode;

    public IkonliIcon(Ikon... icons) {
        this(Arrays.asList(icons), Optional.empty(), Optional.empty());
    }

    public IkonliIcon(List<Ikon> icons) {
        this(icons, Optional.empty(), Optional.empty());
    }

    public IkonliIcon(Color color, Ikon... icons) {
        this(Arrays.asList(icons), Optional.of(color), Optional.empty());
    }

    IkonliIcon(Color color, List<Ikon> icons) {
        this(icons, Optional.of(color), Optional.empty());
    }

    private IkonliIcon(List<Ikon> icons, Optional<Color> color, Optional<Integer> size) {
        this.icons = icons;
        this.color = color;
        this.size = size;
        this.unicode = icons.stream().map(Ikon::getCode).map(String::valueOf).collect(Collectors.joining());
    }

    /// Finds the Ikonli icon whose name matches {@code code} (case-insensitive), tinted with {@code color}.
    public static Optional<JabRefIcon> findIcon(String code, Color color) {
        if (ALL_IKONS.isEmpty()) {
            loadAllIkons();
        }
        return ALL_IKONS.stream()
                        .filter(ikon -> ikon.toString().equals(code.toUpperCase(Locale.ENGLISH)))
                        .<JabRefIcon>map(ikon -> new IkonliIcon(ikon).withColor(color))
                        .findFirst();
    }

    private static void loadAllIkons() {
        ServiceLoader<IkonProvider> providers = ServiceLoader.load(IkonProvider.class);
        for (IkonProvider provider : providers) {
            ALL_IKONS.addAll(EnumSet.allOf(provider.getIkon()));
        }
    }

    @Override
    public Node getGraphicNode() {
        FontIcon fontIcon = FontIcon.of(icons.getFirst());
        fontIcon.getStyleClass().add("glyph-icon");
        size.ifPresent(fontIcon::setIconSize);

        // Override the default color from the css files
        color.ifPresent(color -> fontIcon.setStyle(fontIcon.getStyle() +
                "-fx-fill: %s;".formatted(ColorUtil.toRGBCode(color)) +
                "-fx-icon-color: %s;".formatted(ColorUtil.toRGBCode(color))));

        return fontIcon;
    }

    @Override
    public boolean matches(Node graphicNode) {
        return (graphicNode instanceof FontIcon fontIcon) && fontIcon.getIconCode() == icons.getFirst();
    }

    @Override
    public JabRefIcon withColor(Color color) {
        return new IkonliIcon(icons, Optional.of(color), size);
    }

    @Override
    public JabRefIcon withSize(int size) {
        return new IkonliIcon(icons, color, Optional.of(size));
    }

    @Override
    public JabRefIcon disabled() {
        return withColor(IconTheme.DEFAULT_DISABLED_COLOR);
    }

    @Override
    public String name() {
        return icons.getFirst().toString();
    }

    public String getCode() {
        return this.unicode;
    }

    public Ikon getIkon() {
        return icons.getFirst();
    }
}
