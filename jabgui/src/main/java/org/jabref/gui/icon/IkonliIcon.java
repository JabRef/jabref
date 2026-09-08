package org.jabref.gui.icon;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonProvider;
import org.kordamp.ikonli.javafx.FontIcon;

/// [JabRefIcon] backed by an <a href="https://kordamp.org/ikonli/">Ikonli</a> font glyph, rendered as a
/// [FontIcon]. The font-backed counterpart to [SvgIcon]. Immutable: [#withColor] and
/// [#withSize] return copies.
@NullMarked
public final class IkonliIcon implements JabRefIcon {

    /// Ikonli's own default, so an icon that was never given a size renders as it always has.
    private static final int DEFAULT_SIZE = 8;

    private final List<Ikon> icons;
    private final @Nullable Color color;
    private final int size;

    public IkonliIcon(Ikon... icons) {
        this(List.of(icons), null, DEFAULT_SIZE);
    }

    public IkonliIcon(List<Ikon> icons) {
        this(icons, null, DEFAULT_SIZE);
    }

    public IkonliIcon(Color color, Ikon... icons) {
        this(List.of(icons), color, DEFAULT_SIZE);
    }

    IkonliIcon(Color color, List<Ikon> icons) {
        this(icons, color, DEFAULT_SIZE);
    }

    private IkonliIcon(List<Ikon> icons, @Nullable Color color, int size) {
        this.icons = List.copyOf(icons);
        this.color = color;
        this.size = size;
    }

    /// Finds the Ikonli icon whose name matches `code` (case-insensitive).
    public static Optional<JabRefIcon> findIcon(String code) {
        return Optional.ofNullable(IkonliIcons.BY_NAME.get(code.toUpperCase(Locale.ENGLISH)))
                       .map(IkonliIcon::new);
    }

    /// Finds the Ikonli icon whose pack-qualified description matches `description` (case-insensitive).
    public static Optional<JabRefIcon> findIconByDescription(String description) {
        return Optional.ofNullable(IkonliIcons.BY_DESCRIPTION.get(description.toUpperCase(Locale.ENGLISH)))
                       .map(IkonliIcon::new);
    }

    public static SequencedSet<Ikon> allIcons() {
        return IkonliIcons.ALL;
    }

    /// Holds every [Ikon] discovered via the [IkonProvider] service loader. Initialization on first
    /// access guaranteed by JVM.
    private static final class IkonliIcons {
        private static final SequencedSet<Ikon> ALL = load();
        private static final Map<String, Ikon> BY_NAME = loadMap(Ikon::toString);
        private static final Map<String, Ikon> BY_DESCRIPTION = loadMap(Ikon::getDescription);

        private static SequencedSet<Ikon> load() {
            SequencedSet<Ikon> all = new LinkedHashSet<>();
            for (IkonProvider provider : ServiceLoader.load(IkonProvider.class)) {
                all.addAll(EnumSet.allOf(provider.getIkon()));
            }
            return Collections.unmodifiableSequencedSet(all);
        }

        private static Map<String, Ikon> loadMap(Function<Ikon, String> keyMapper) {
            return ALL.stream()
                      .collect(Collectors.toUnmodifiableMap(
                              ikon -> keyMapper.apply(ikon).toUpperCase(Locale.ENGLISH),
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
        // An explicit color (via withColor/disabled) has to survive the theme's .glyph-icon rules, which is what
        // JabRefFontIcon takes care of. Without one, those rules are what colors the icon.
        FontIcon fontIcon = color == null ? FontIcon.of(ikon, size) : new JabRefFontIcon(ikon, size, color);
        fontIcon.getStyleClass().add("glyph-icon");
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
