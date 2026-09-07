package org.jabref.gui.theme;

import java.util.Arrays;
import java.util.List;

import org.jabref.logic.l10n.Localization;

/// A theme the user can select: one of the two built-in themes, or one of the community themes
/// from <https://themes.jabref.org/> that the build bundles from the `jabgui/themes.jabref.org`
/// submodule (see `processResources` in `jabgui/build.gradle.kts`).
///
/// The community constants are a hand-maintained mirror of that submodule -- `ThemePresetTest`
/// fails when the two drift apart -- so that the set of themes is fixed at compile time and no
/// classpath scanning happens at startup. Built-in themes declare the complete `-color-*` token
/// contract; community themes only override what differs from the JabRef theme, so [ThemeManager]
/// layers them on top of it.
public enum ThemePreset {
    JABREF(Localization.lang("JabRef theme"), "jabref-theme.css", true),
    PRIMER(Localization.lang("Primer theme"), "primer-theme.css", true),

    CHOCOLATE_HONEY("Chocolate Honey", "chocolate-honey.css"),
    DINOGIRLS_CHOCOLATEBROWN_CONTRASTTEXT("DinoGirls: chocolatebrown / darksalmon, contrast text", "chocolatebrown-darksalmon-contrasttext.css"),
    DINOGIRLS_CHOCOLATEBROWN_GREYTEXT("DinoGirls: chocolatebrown / darksalmon, grey text", "chocolatebrown-darksalmon-greytext.css"),
    DINOGIRLS_FUCHSIAPURPLE_CONTRASTTEXT("DinoGirls: fuchsiapurple / japanesesakura, contrast text", "fuchsiapurple-japanesesakura-contrasttext.css"),
    DINOGIRLS_FUCHSIAPURPLE_GREYTEXT("DinoGirls: fuchsiapurple / japanesesakura, grey text", "fuchsiapurple-japanesesakura-greytext.css"),
    DINOGIRLS_JABREFDARK_CONTRASTTEXT("DinoGirls: jabrefdark / jabreflight, contrast text", "jabrefdark-jabreflight-contrasttext.css"),
    DINOGIRLS_JABREFDARK_GREYTEXT("DinoGirls: jabrefdark / jabreflight, grey text", "jabrefdark-jabreflight-greytext.css"),
    DINOGIRLS_LIGHTBLUE_CONTRASTTEXT("DinoGirls: lightblue / iceage, contrast text", "lightblue-iceage-contrasttext.css"),
    DINOGIRLS_LIGHTBLUE_GREYTEXT("DinoGirls: lightblue / iceage, grey text", "lightblue-iceage-greytext.css"),
    DINOGIRLS_LIGHTSEAGREEN_CONTRASTTEXT("DinoGirls: lightseagreen / limegreen, contrast text", "lightseagreen-limegreen-contrasttext.css"),
    DINOGIRLS_LIGHTSEAGREEN_GREYTEXT("DinoGirls: lightseagreen / limegreen, grey text", "lightseagreen-limegreen-greytext.css"),
    DINOGIRLS_PREHISTORICAMBER_CONTRASTTEXT("DinoGirls: prehistoricamber / peachorange, contrast text", "prehistoricamber-peachorange-contrasttext.css"),
    DINOGIRLS_PREHISTORICAMBER_GREYTEXT("DinoGirls: prehistoricamber / peachorange, grey text", "prehistoricamber-peachorange-greytext.css"),
    DINOGIRLS_TWILIGHTLAVENDER_CONTRASTTEXT("DinoGirls: twilightlavender / neon, contrast text", "twilightlavender-neon-contrasttext.css"),
    DINOGIRLS_TWILIGHTLAVENDER_GREYTEXT("DinoGirls: twilightlavender / neon, grey text", "twilightlavender-neon-greytext.css"),
    DINOGIRLS_WINERED_CONTRASTTEXT("DinoGirls: winered / icedstrawberry, contrast text", "winered-icedstrawberry-contrasttext.css"),
    DINOGIRLS_WINERED_GREYTEXT("DinoGirls: winered / icedstrawberry, grey text", "winered-icedstrawberry-greytext.css"),
    EVERFOREST("Everforest", "everforest.css"),
    NORD("Nord", "nord.css");

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
