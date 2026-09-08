package org.jabref.gui.theme;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.Nullable;

/// A theme the user can select: one of the two built-in themes, or one of the community themes
/// from <https://themes.jabref.org/> that the build bundles from the `jabgui/src/main/themes.jabref.org`
/// submodule (see `processResources` in `jabgui/build.gradle.kts`).
///
/// The community constants are a hand-maintained mirror of that submodule -- `ThemePresetTest`
/// fails when the two drift apart -- so that the set of themes is fixed at compile time and no
/// classpath scanning happens at startup. The two JabRef themes declare the complete `-color-*`
/// token contract; community themes only override what differs from their [#getParent()], the
/// JabRef theme, which [ThemeManager] installs beneath them.
///
/// [impl->req~ux.themes.bundled-community-themes~1]
public enum ThemePreset {
    JABREF(Localization.lang("JabRef theme"), "jabref-theme.css", null),
    EVERFOREST("Everforest", "community/everforest.css", JABREF),
    NORD("Nord", "community/nord.css", JABREF),
    PAPERS("Papers", "community/papers.css", JABREF),
    PRIMER("Primer", "primer-theme.css", null),
    CHOCOLATE_HONEY("Chocolate Honey", "community/chocolate-honey.css", JABREF),
    DINOGIRLS_CHOCOLATEBROWN("Dino Girl's Chocolate Brown", "Dino Girl's Dark Salmon", "community/chocolatebrown-darksalmon-contrasttext.css", JABREF),
    DINOGIRLS_FUCHSIAPURPLE("Dino Girl's Fuchsia Purple", "Dino Girl's Japanese Sakura", "community/fuchsiapurple-japanesesakura-contrasttext.css", JABREF),
    DINOGIRLS_LIGHTBLUE("Dino Girl's Light Blue", "Dino Girl's Ice Age", "community/lightblue-iceage-contrasttext.css", JABREF),
    DINOGIRLS_LIGHTSEAGREEN("Dino Girl's Light Sea Green", "Dino Girl's Lime Green", "community/lightseagreen-limegreen-contrasttext.css", JABREF),
    DINOGIRLS_PREHISTORICAMBER("Dino Girl's Prehistoric Amber", "Dino Girl's Peach Orange", "community/prehistoricamber-peachorange-contrasttext.css", JABREF),
    DINOGIRLS_TWILIGHTLAVENDER("Dino Girl's Twilight Lavender", "Dino Girl's Neon", "community/twilightlavender-neon-contrasttext.css", JABREF),
    DINOGIRLS_WINERED("Dino Girl's Wine Red", "Dino Girl's Iced Strawberry", "community/winered-icedstrawberry-contrasttext.css", JABREF);

    private final String darkName;
    private final String lightName;
    private final String css;
    private final @Nullable ThemePreset parent;

    private StyleSheet styleSheet;

    /// @param parent the theme this one only overrides tokens of, `null` for a theme declaring the complete token contract
    ThemePreset(String themeName, String css, @Nullable ThemePreset parent) {
        this(themeName, themeName, css, parent);
    }

    /// Dino Girl published her dark and light hues as separate themes; the port pairs each dark hue with
    /// the closest light one in a single file, so such a theme carries one name per color scheme.
    /// Community theme names are proper nouns and stay untranslated.
    ThemePreset(String darkName, String lightName, String css, @Nullable ThemePreset parent) {
        this.darkName = darkName;
        this.lightName = lightName;
        this.css = css;
        this.parent = parent;
    }

    public static ThemePreset of(String themePreset) {
        if (themePreset == null) {
            return JABREF;
        }

        try {
            return valueOf(themePreset);
        } catch (IllegalArgumentException e) {
            return JABREF;
        }
    }

    public String getPreferenceName() {
        return name();
    }

    /// Both names of a paired theme, e.g. for lists that are not tied to a color scheme.
    public String getLocalizedName() {
        return darkName.equals(lightName) ? darkName : darkName + " / " + lightName;
    }

    /// The name matching the color scheme the user would see the theme in; when following the system,
    /// both hues are shown as "dark / light".
    public String getLocalizedName(ThemeColorScheme colorScheme) {
        return switch (colorScheme) {
            case DARK ->
                    darkName;
            case LIGHT ->
                    lightName;
            case FOLLOW_SYSTEM ->
                    getLocalizedName();
        };
    }

    /// The theme whose stylesheet is installed beneath this one, so that the tokens this theme does not
    /// declare come from there. Empty for the JabRef and Primer themes, which declare every token.
    public Optional<ThemePreset> getParent() {
        return Optional.ofNullable(parent);
    }

    public StyleSheet getStyleSheet() {
        if (styleSheet == null) {
            styleSheet = StyleSheet.create(css).orElseThrow();
        }
        return styleSheet;
    }
}
