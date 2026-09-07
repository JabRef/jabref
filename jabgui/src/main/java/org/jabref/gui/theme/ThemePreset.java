package org.jabref.gui.theme;

import java.util.Arrays;
import java.util.List;

import org.jabref.logic.l10n.Localization;

/// A theme the user can select: one of the two built-in themes, or one of the community themes
/// from <https://themes.jabref.org/> that the build bundles from the `jabgui/src/main/themes.jabref.org`
/// submodule (see `processResources` in `jabgui/build.gradle.kts`).
///
/// The community constants are a hand-maintained mirror of that submodule -- `ThemePresetTest`
/// fails when the two drift apart -- so that the set of themes is fixed at compile time and no
/// classpath scanning happens at startup. Built-in themes declare the complete `-color-*` token
/// contract; community themes only override what differs from the JabRef theme, so [ThemeManager]
/// layers them on top of it.
///
/// [impl->req~ux.themes.bundled-community-themes~1]
public enum ThemePreset {
    JABREF(Localization.lang("JabRef theme"), "jabref-theme.css", true),
    EVERFOREST("Everforest", "everforest.css"),
    NORD("Nord", "nord.css"),
    PRIMER(Localization.lang("Primer theme"), "primer-theme.css", true),
    CHOCOLATE_HONEY("Chocolate Honey", "chocolate-honey.css"),
    DINOGIRLS_CHOCOLATEBROWN("DinoGirls: Chocolate Brown / Dark Salmon", "chocolatebrown-darksalmon-contrasttext.css"),
    DINOGIRLS_FUCHSIAPURPLE("DinoGirls: Fuchsia Purple / Japanese Sakura", "fuchsiapurple-japanesesakura-contrasttext.css"),
    DINOGIRLS_LIGHTBLUE("DinoGirls: Light Blue / Ice Age", "lightblue-iceage-contrasttext.css"),
    DINOGIRLS_LIGHTSEAGREEN("DinoGirls: Light Sea Green / Lime Green", "lightseagreen-limegreen-contrasttext.css"),
    DINOGIRLS_PREHISTORICAMBER("DinoGirls: Prehistoric Amber / Peach Orange", "prehistoricamber-peachorange-contrasttext.css"),
    DINOGIRLS_TWILIGHTLAVENDER("DinoGirls: Twilight Lavender / Neon", "twilightlavender-neon-contrasttext.css"),
    DINOGIRLS_WINERED("DinoGirls: Wine Red / Iced Strawberry", "winered-icedstrawberry-contrasttext.css");

    private static final String COMMUNITY_DIRECTORY = "community/";

    private final String themeName;
    private final String css;
    private final boolean builtIn;

    private StyleSheet styleSheet;

    ThemePreset(String themeName, String css, boolean builtIn) {
        this.themeName = themeName;
        this.css = builtIn ? css : COMMUNITY_DIRECTORY + css;
        this.builtIn = builtIn;
    }

    /// A community theme; theme names are proper nouns and stay untranslated.
    ThemePreset(String themeName, String css) {
        this(themeName, css, false);
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

    public static List<ThemePreset> builtIn() {
        return Arrays.stream(values()).filter(ThemePreset::isBuiltIn).toList();
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public String getPreferenceName() {
        return name();
    }

    public String getLocalizedName() {
        return themeName;
    }

    /// The CSS file name of a community theme as it appears in the submodule, `null` for a built-in theme.
    String getCommunityFileName() {
        return builtIn ? null : css.substring(COMMUNITY_DIRECTORY.length());
    }

    public StyleSheet getStyleSheet() {
        if (styleSheet == null) {
            styleSheet = StyleSheet.create(css).orElseThrow();
        }
        return styleSheet;
    }
}
