package org.jabref.gui.icon;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import org.jabref.gui.util.ColorUtil;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonProvider;
import org.kordamp.ikonli.javafx.FontIcon;

/// {@link JabRefIcon} backed by an <a href="https://kordamp.org/ikonli/">Ikonli</a> font glyph, rendered as a
/// {@link FontIcon}. The font-backed counterpart to {@link SvgIcon}. Immutable: {@link #withColor} and
/// {@link #withSize} return copies.
public final class IkonliIcon implements JabRefIcon {

    /// All {@link Ikon} values discovered via the {@link IkonProvider} service loader, used by {@link #findIcon}.
    /// Lazily populated on first lookup.
    private static final Set<Ikon> ALL_IKONS = new HashSet<>();

    private final List<Ikon> icons;
    private final Optional<Color> color;
    private final Optional<Integer> size;

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
        if (icons.size() == 1) {
            return buildFontIcon(icons.getFirst());
        }
        // Multi-glyph icon (e.g. the RANK1..RANK5 star rows): render one FontIcon per glyph side by side, so all
        // stars show instead of only the first.
        HBox row = new HBox();
        for (Ikon ikon : icons) {
            row.getChildren().add(buildFontIcon(ikon));
        }
        return row;
    }

    private FontIcon buildFontIcon(Ikon ikon) {
        FontIcon fontIcon = FontIcon.of(ikon);
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
}
