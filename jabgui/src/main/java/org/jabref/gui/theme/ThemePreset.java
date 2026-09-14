package org.jabref.gui.theme;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.Nullable;

/// A theme the user can select. Every theme, JabRef's own included, is maintained on
/// <https://themes.jabref.org/> and bundled by the build from the `jabgui/src/main/themes.jabref.org`
/// submodule (see `processResources` in `jabgui/build.gradle.kts`); the JabRef repository holds no
/// theme of its own, only the base stylesheet that reads the tokens.
///
/// The constants are a hand-maintained mirror of that submodule -- `ThemePresetTest` fails when the
/// two drift apart -- so that the set of themes is fixed at compile time and no classpath scanning
/// happens at startup. The JabRef and Primer themes declare the complete `-color-*` token contract
/// and have no parent; the other themes only override what differs from their [#getParent()], the
/// JabRef theme, which [ThemeManager] installs beneath them.
///
/// [impl->req~ux.themes.bundled-community-themes~1]
@AllowedToUseClassGetResource("The previews are bundled next to the themes.")
public enum ThemePreset {
    JABREF(Localization.lang("JabRef theme"), "themes.jabref.org/jabref-theme.css", null),
    EVERFOREST("Everforest", "themes.jabref.org/everforest.css", JABREF),
    NORD("Nord", "themes.jabref.org/nord.css", JABREF),
    PAPERS("Papers", "themes.jabref.org/papers.css", JABREF),
    PRIMER("Primer", "themes.jabref.org/primer.css", null),
    CHOCOLATE_HONEY("Chocolate Honey", "themes.jabref.org/chocolate-honey.css", JABREF),
    DINOGIRLS_CHOCOLATEBROWN("Dino Girl's", "Chocolate Brown", "Dark Salmon", "themes.jabref.org/chocolatebrown-darksalmon-contrasttext.css", JABREF),
    DINOGIRLS_FUCHSIAPURPLE("Dino Girl's", "Fuchsia Purple", "Japanese Sakura", "themes.jabref.org/fuchsiapurple-japanesesakura-contrasttext.css", JABREF),
    DINOGIRLS_LIGHTBLUE("Dino Girl's", "Light Blue", "Ice Age", "themes.jabref.org/lightblue-iceage-contrasttext.css", JABREF),
    DINOGIRLS_LIGHTSEAGREEN("Dino Girl's", "Light Sea Green", "Lime Green", "themes.jabref.org/lightseagreen-limegreen-contrasttext.css", JABREF),
    DINOGIRLS_PREHISTORICAMBER("Dino Girl's", "Prehistoric Amber", "Peach Orange", "themes.jabref.org/prehistoricamber-peachorange-contrasttext.css", JABREF),
    DINOGIRLS_TWILIGHTLAVENDER("Dino Girl's", "Twilight Lavender", "Neon", "themes.jabref.org/twilightlavender-neon-contrasttext.css", JABREF),
    DINOGIRLS_WINERED("Dino Girl's", "Wine Red", "Iced Strawberry", "themes.jabref.org/winered-icedstrawberry-contrasttext.css", JABREF);

    private final String darkName;
    private final String lightName;
    private final String bothNames;
    private final String css;
    private final @Nullable ThemePreset parent;

    private StyleSheet styleSheet;

    /// @param parent the theme this one only overrides tokens of, `null` for a theme declaring the complete token contract
    ThemePreset(String themeName, String css, @Nullable ThemePreset parent) {
        this.darkName = themeName;
        this.lightName = themeName;
        this.bothNames = themeName;
        this.css = css;
        this.parent = parent;
    }

    /// Dino Girl published her dark and light hues as separate themes; the port pairs each dark hue with
    /// the closest light one in a single file, so such a theme carries one name per color scheme.
    /// Community theme names are proper nouns and stay untranslated.
    ThemePreset(String collection, String darkHue, String lightHue, String css, @Nullable ThemePreset parent) {
        this.darkName = collection + " " + darkHue;
        this.lightName = collection + " " + lightHue;
        this.bothNames = collection + " " + darkHue + " / " + lightHue;
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
        return bothNames;
    }

    /// The name matching the color scheme the user would see the theme in; when following the system,
    /// both hues are shown as "dark / light".
    ///
    /// @param colorScheme the scheme the theme is shown in; an unset one is read as [ThemeColorScheme#FOLLOW_SYSTEM], as everywhere else
    public String getLocalizedName(@Nullable ThemeColorScheme colorScheme) {
        return switch (Objects.requireNonNullElse(colorScheme, ThemeColorScheme.FOLLOW_SYSTEM)) {
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

    /// The bundled screenshot of this theme in the given color scheme, scaled to preview size by the
    /// build (`generateThemePreviews` in `jabgui/build.gradle.kts`). Following the system yields the dark one.
    public Optional<URL> getPreview(ThemeColorScheme colorScheme) {
        String baseName = css.substring(css.lastIndexOf('/') + 1, css.length() - ".css".length());
        String scheme = colorScheme == ThemeColorScheme.LIGHT ? "light" : "dark";
        return Optional.ofNullable(ThemePreset.class.getResource("preview/" + baseName + "-" + scheme + ".png"));
    }

    public StyleSheet getStyleSheet() {
        if (styleSheet == null) {
            styleSheet = StyleSheet.create(css).orElseThrow();
        }
        return styleSheet;
    }
}
