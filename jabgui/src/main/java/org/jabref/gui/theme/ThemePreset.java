package org.jabref.gui.theme;

import javafx.application.ColorScheme;
import javafx.application.Platform;

import org.jabref.logic.l10n.Localization;

/// A theme the user can select: one of the two built-in themes, or one of the community themes
/// from <https://themes.jabref.org/> that the build bundles from the `jabgui/src/main/themes.jabref.org`
/// submodule (see `processResources` in `jabgui/build.gradle.kts`).
///
/// The community constants are a hand-maintained mirror of that submodule -- `ThemePresetTest`
/// fails when the two drift apart -- so that the set of themes is fixed at compile time and no
/// classpath scanning happens at startup. The two JabRef themes declare the complete `-color-*`
/// token contract; community themes only override what differs from the JabRef theme, so
/// [ThemeManager] layers them on top of it.
///
/// [impl->req~ux.themes.bundled-community-themes~1]
public enum ThemePreset {
    JABREF(Localization.lang("JabRef theme"), "jabref-theme.css"),
    EVERFOREST("Everforest", "community/everforest.css"),
    NORD("Nord", "community/nord.css"),
    PAPERS("Papers", "community/papers.css"),
    PRIMER(Localization.lang("Primer theme"), "primer-theme.css"),
    CHOCOLATE_HONEY("Chocolate Honey", "community/chocolate-honey.css"),
    DINOGIRLS_CHOCOLATEBROWN("Dino Girl\'s Chocolate Brown", "Dino Girl\'s Dark Salmon", "community/chocolatebrown-darksalmon-contrasttext.css"),
    DINOGIRLS_FUCHSIAPURPLE("Dino Girl\'s Fuchsia Purple", "Dino Girl\'s Japanese Sakura", "community/fuchsiapurple-japanesesakura-contrasttext.css"),
    DINOGIRLS_LIGHTBLUE("Dino Girl\'s Light Blue", "Dino Girl\'s Ice Age", "community/lightblue-iceage-contrasttext.css"),
    DINOGIRLS_LIGHTSEAGREEN("Dino Girl\'s Light Sea Green", "Dino Girl\'s Lime Green", "community/lightseagreen-limegreen-contrasttext.css"),
    DINOGIRLS_PREHISTORICAMBER("Dino Girl\'s Prehistoric Amber", "Dino Girl\'s Peach Orange", "community/prehistoricamber-peachorange-contrasttext.css"),
    DINOGIRLS_TWILIGHTLAVENDER("Dino Girl\'s Twilight Lavender", "Dino Girl\'s Neon", "community/twilightlavender-neon-contrasttext.css"),
    DINOGIRLS_WINERED("Dino Girl\'s Wine Red", "Dino Girl\'s Iced Strawberry", "community/winered-icedstrawberry-contrasttext.css");

    private final String darkName;
    private final String lightName;
    private final String css;

    private StyleSheet styleSheet;

    /// Community theme names are proper nouns and stay untranslated.
    ThemePreset(String themeName, String css) {
        this(themeName, themeName, css);
    }

    /// Dino Girl published her dark and light hues as separate themes; the port pairs each dark hue with
    /// the closest light one in a single file, so such a theme carries one name per color scheme.
    ThemePreset(String darkName, String lightName, String css) {
        this.darkName = darkName;
        this.lightName = lightName;
        this.css = css;
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

    /// The name matching the color scheme the user would see the theme in.
    public String getLocalizedName(ThemeColorScheme colorScheme) {
        boolean dark = switch (colorScheme) {
            case DARK ->
                    true;
            case LIGHT ->
                    false;
            case FOLLOW_SYSTEM ->
                    Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
        };
        return dark ? darkName : lightName;
    }

    public StyleSheet getStyleSheet() {
        if (styleSheet == null) {
            styleSheet = StyleSheet.create(css).orElseThrow();
        }
        return styleSheet;
    }
}
