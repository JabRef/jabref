package org.jabref.gui.icon;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import org.jabref.gui.util.ColorUtil;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonProvider;
import org.kordamp.ikonli.javafx.FontIcon;

/// {@link JabRefIcon} backed by an <a href="https://kordamp.org/ikonli/">Ikonli</a> font glyph, rendered as a
/// {@link FontIcon}. The font-backed counterpart to {@link SvgIcon}. Immutable: {@link #withColor} and
/// {@link #withSize} return copies.
@NullMarked
public final class IkonliIcon implements JabRefIcon {

    private final List<Ikon> icons;
    private final @Nullable Color color;
    private final @Nullable Integer size;

    public IkonliIcon(Ikon... icons) {
        this(List.of(icons), null, null);
    }

    public IkonliIcon(List<Ikon> icons) {
        this(icons, null, null);
    }

    public IkonliIcon(Color color, Ikon... icons) {
        this(List.of(icons), color, null);
    }

    IkonliIcon(Color color, List<Ikon> icons) {
        this(icons, color, null);
    }

    private IkonliIcon(List<Ikon> icons, @Nullable Color color, @Nullable Integer size) {
        this.icons = List.copyOf(icons);
        this.color = color;
        this.size = size;
    }

    /// Finds the Ikonli icon whose name matches {@code code} (case-insensitive), tinted with {@code color}.
    public static Optional<JabRefIcon> findIcon(String code, Color color) {
        return Optional.ofNullable(IkonliIcons.BY_NAME.get(code.toUpperCase(Locale.ENGLISH)))
                       .map(ikon -> new IkonliIcon(ikon).withColor(color));
    }

    /// Holds every {@link Ikon} discovered via the {@link IkonProvider} service loader. Initialization on first
    /// access guaranteed by JVM.
    private static final class IkonliIcons {
        private static final Set<Ikon> ALL = load();
        private static final Map<String, Ikon> BY_NAME = loadMap();

        private static Set<Ikon> load() {
            Set<Ikon> all = new HashSet<>();
            for (IkonProvider provider : ServiceLoader.load(IkonProvider.class)) {
                all.addAll(EnumSet.allOf(provider.getIkon()));
            }
            return Set.copyOf(all);
        }

        private static Map<String, Ikon> loadMap() {
            return ALL.stream()
                      .collect(Collectors.toUnmodifiableMap(
                              Ikon::toString,
                              ikon -> ikon,
                              (existing, duplicate) -> existing
                      ));
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
        if (size != null) {
            fontIcon.setIconSize(size);
        }

        // Override the default color from the css files
        if (color != null) {
            fontIcon.setStyle(fontIcon.getStyle() +
                    "-fx-fill: %s;".formatted(ColorUtil.toRGBCode(color)) +
                    "-fx-icon-color: %s;".formatted(ColorUtil.toRGBCode(color)));
        }

        return fontIcon;
    }

    @Override
    public boolean matches(Node graphicNode) {
        return (graphicNode instanceof FontIcon fontIcon) && fontIcon.getIconCode() == icons.getFirst();
    }

    @Override
    public JabRefIcon withColor(Color color) {
        return new IkonliIcon(icons, color, size);
    }

    @Override
    public JabRefIcon withSize(int size) {
        return new IkonliIcon(icons, color, size);
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
