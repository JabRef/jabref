package org.jabref.gui.theme;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.jabref.architecture.AllowedToUseClassGetResource;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@AllowedToUseClassGetResource("Lists the bundled theme files next to the theme package.")
class ThemePresetTest {

    /// Where the build copies the themes to, relative to the theme package; named like the submodule.
    private static final String THEMES_DIRECTORY = "themes.jabref.org/";

    /// The constants of [ThemePreset] are written by hand so that the set of themes is
    /// fixed at compile time; which files of the `themes.jabref.org` submodule get bundled is decided
    /// by the excludes in `jabgui/build.gradle.kts`. This test keeps the two in step: after a
    /// submodule bump that brings a new theme, it fails until the theme is either listed in the enum
    /// or excluded from the build.
    @Test
    void constantsMatchTheBundledFiles() throws IOException, URISyntaxException {
        URL themesDirectory = ThemePreset.class.getResource(THEMES_DIRECTORY);
        assertNotNull(themesDirectory, "No themes bundled, although processResources refuses to run without them");

        Set<String> bundled = new TreeSet<>();
        try (Stream<Path> files = Files.list(Path.of(themesDirectory.toURI()))) {
            files.map(file -> file.getFileName().toString())
                 .filter(name -> name.endsWith(".css"))
                 .forEach(bundled::add);
        }

        Set<String> listed = new TreeSet<>();
        Arrays.stream(ThemePreset.values())
              .map(theme -> theme.getStyleSheet().getName())
              .filter(css -> css.startsWith(THEMES_DIRECTORY))
              .map(css -> css.substring(THEMES_DIRECTORY.length()))
              .forEach(listed::add);

        assertEquals(bundled, listed, "ThemePreset does not list exactly the bundled themes");
    }

    /// The stored color scheme can be absent, and the preferences read that as "follow system".
    @Test
    void unsetColorSchemeNamesAThemeAsFollowSystemDoes() {
        assertEquals(ThemePreset.DINOGIRLS_WINERED.getLocalizedName(ThemeColorScheme.FOLLOW_SYSTEM),
                ThemePreset.DINOGIRLS_WINERED.getLocalizedName(null));
    }

    @ParameterizedTest
    @EnumSource(ThemePreset.class)
    void everyThemeHasAPreviewPerColorScheme(ThemePreset theme) {
        assertTrue(theme.getPreview(ThemeColorScheme.DARK).isPresent(), theme + " has no dark preview");
        assertTrue(theme.getPreview(ThemeColorScheme.LIGHT).isPresent(), theme + " has no light preview");
    }
}
