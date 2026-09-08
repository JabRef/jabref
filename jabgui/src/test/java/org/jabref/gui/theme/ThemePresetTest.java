package org.jabref.gui.theme;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.jabref.architecture.AllowedToUseClassGetResource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@AllowedToUseClassGetResource("Lists the bundled theme files next to the theme package.")
class ThemePresetTest {

    /// Where the build copies the community themes to, relative to the theme package.
    private static final String COMMUNITY_DIRECTORY = "community/";

    /// The community constants of [ThemePreset] are written by hand so that the set of themes is
    /// fixed at compile time; which files of the `themes.jabref.org` submodule get bundled is decided
    /// by the excludes in `jabgui/build.gradle.kts`. This test keeps the two in step: after a
    /// submodule bump that brings a new theme, it fails until the theme is either listed in the enum
    /// or excluded from the build.
    @Test
    void communityConstantsMatchTheBundledFiles() throws IOException, URISyntaxException {
        var communityDirectory = ThemePreset.class.getResource(COMMUNITY_DIRECTORY);
        assertNotNull(communityDirectory, "No community themes bundled -- is the themes.jabref.org submodule checked out?");

        Set<String> bundled = new TreeSet<>();
        try (Stream<Path> files = Files.list(Path.of(communityDirectory.toURI()))) {
            files.map(file -> file.getFileName().toString())
                 .filter(name -> name.endsWith(".css"))
                 .forEach(bundled::add);
        }

        Set<String> listed = new TreeSet<>();
        Arrays.stream(ThemePreset.values())
              .map(theme -> theme.getStyleSheet().getName())
              .filter(css -> css.startsWith(COMMUNITY_DIRECTORY))
              .map(css -> css.substring(COMMUNITY_DIRECTORY.length()))
              .forEach(listed::add);

        assertEquals(bundled, listed, "ThemePreset does not list exactly the bundled community themes");
    }
}
