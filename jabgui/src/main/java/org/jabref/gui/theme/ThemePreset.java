package org.jabref.gui.theme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A theme the user can select: one of the two built-in themes, or one of the community themes
/// from <https://themes.jabref.org/> that the build bundles (see `generateCommunityThemes` in
/// `jabgui/build.gradle.kts`, which also writes the index read here).
///
/// Built-in themes declare the complete `-color-*` token contract. Community themes only override
/// what differs from the JabRef theme, so [ThemeManager] layers them on top of it.
@AllowedToUseClassGetResource("Reads the build-time index of the bundled community themes.")
public final class ThemePreset {
    public static final ThemePreset JABREF = new ThemePreset("JABREF", Localization.lang("JabRef theme"), "jabref-theme.css");
    public static final ThemePreset PRIMER = new ThemePreset("PRIMER", Localization.lang("Primer theme"), "primer-theme.css");

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemePreset.class);
    private static final String COMMUNITY_DIRECTORY = "community/";
    private static final String COMMUNITY_INDEX = COMMUNITY_DIRECTORY + "index.txt";

    private static List<ThemePreset> values;

    private final String preferenceName;
    private final String themeName;
    private final String css;

    private StyleSheet styleSheet;

    private ThemePreset(String preferenceName, String themeName, String css) {
        this.preferenceName = preferenceName;
        this.themeName = themeName;
        this.css = css;
    }

    public static List<ThemePreset> builtIn() {
        return List.of(JABREF, PRIMER);
    }

    /// The built-in themes followed by the bundled community themes, in a stable order.
    ///
    /// [impl->req~ux.themes.bundled-community-themes~1]
    public static synchronized List<ThemePreset> values() {
        if (values == null) {
            List<ThemePreset> all = new ArrayList<>(builtIn());
            all.addAll(communityThemes());
            values = List.copyOf(all);
        }
        return values;
    }

    /// One theme per line: `Display name|file.css`, the file living next to the index.
    private static List<ThemePreset> communityThemes() {
        InputStream index = ThemePreset.class.getResourceAsStream(COMMUNITY_INDEX);
        if (index == null) {
            LOGGER.warn("No bundled community themes found");
            return List.of();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(index, StandardCharsets.UTF_8))) {
            return reader.lines()
                         .filter(line -> line.contains("|"))
                         .map(line -> line.split("\\|", 2))
                         .map(parts -> new ThemePreset(parts[1], parts[0], COMMUNITY_DIRECTORY + parts[1]))
                         .toList();
        } catch (IOException e) {
            LOGGER.warn("Could not read the index of the bundled community themes", e);
            return List.of();
        }
    }

    /// @return the theme stored under that preference name, or the JabRef theme if there is none
    public static ThemePreset of(String themePreset) {
        return values().stream()
                       .filter(theme -> theme.preferenceName.equals(themePreset))
                       .findFirst()
                       .orElse(JABREF);
    }

    public boolean isBuiltIn() {
        return builtIn().contains(this);
    }

    public String getPreferenceName() {
        return preferenceName;
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

    @Override
    public boolean equals(Object o) {
        return o instanceof ThemePreset other && preferenceName.equals(other.preferenceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preferenceName);
    }

    @Override
    public String toString() {
        return preferenceName;
    }
}
