package org.jabref.gui.theme;

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
    PRIMER(Localization.lang("Primer theme"), "primer-theme.css"),
    CHOCOLATE_HONEY("Chocolate Honey", "community/chocolate-honey.css"),
    DINOGIRLS_CHOCOLATEBROWN("DinoGirls: Chocolate Brown / Dark Salmon", "community/chocolatebrown-darksalmon-contrasttext.css"),
    DINOGIRLS_FUCHSIAPURPLE("DinoGirls: Fuchsia Purple / Japanese Sakura", "community/fuchsiapurple-japanesesakura-contrasttext.css"),
    DINOGIRLS_LIGHTBLUE("DinoGirls: Light Blue / Ice Age", "community/lightblue-iceage-contrasttext.css"),
    DINOGIRLS_LIGHTSEAGREEN("DinoGirls: Light Sea Green / Lime Green", "community/lightseagreen-limegreen-contrasttext.css"),
    DINOGIRLS_PREHISTORICAMBER("DinoGirls: Prehistoric Amber / Peach Orange", "community/prehistoricamber-peachorange-contrasttext.css"),
    DINOGIRLS_TWILIGHTLAVENDER("DinoGirls: Twilight Lavender / Neon", "community/twilightlavender-neon-contrasttext.css"),
    DINOGIRLS_WINERED("DinoGirls: Wine Red / Iced Strawberry", "community/winered-icedstrawberry-contrasttext.css");

    private final String themeName;
    private final String css;

    private StyleSheet styleSheet;

    /// Community theme names are proper nouns and stay untranslated.
    ThemePreset(String themeName, String css) {
        this.themeName = themeName;
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

    public String getLocalizedName() {
        return themeName;
    }

    public StyleSheet getStyleSheet() {
        if (styleSheet == null) {
            styleSheet = StyleSheet.create(css).orElseThrow();
        }
        return styleSheet;
    }
}
