package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemePresetTest {

    /// The submodule, relative to the module directory Gradle runs the tests in.
    private static final Path THEMES_JABREF_ORG = Path.of("src", "main", "themes.jabref.org", "themes");

    /// Offered by the submodule but deliberately not bundled: the grey-text DinoGirls variants read
    /// worse than their contrast-text twins, and the jabrefdark/jabreflight pair is JabRef's own look.
    private static final Set<String> NOT_BUNDLED = Set.of(
            "chocolatebrown-darksalmon-greytext.css",
            "fuchsiapurple-japanesesakura-greytext.css",
            "jabrefdark-jabreflight-contrasttext.css",
            "jabrefdark-jabreflight-greytext.css",
            "lightblue-iceage-greytext.css",
            "lightseagreen-limegreen-greytext.css",
            "prehistoricamber-peachorange-greytext.css",
            "twilightlavender-neon-greytext.css",
            "winered-icedstrawberry-greytext.css");

    /// The community constants of [ThemePreset] are written by hand so that the set of themes is
    /// fixed at compile time. This test is what keeps them in step with the submodule: after a
    /// submodule bump it fails until the enum lists exactly the two-scheme themes on offer -- or
    /// [#NOT_BUNDLED] names the ones left out on purpose.
    @Test
    void communityThemesMirrorTheSubmodule() throws IOException {
        assertTrue(Files.isDirectory(THEMES_JABREF_ORG), "Submodule not checked out: git submodule update --init");

        Set<String> offered = new TreeSet<>();
        try (Stream<Path> dirs = Files.list(THEMES_JABREF_ORG)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                // Single-scheme themes live in these two folders and cannot follow the color scheme.
                if (dir.endsWith("DarkTheme") || dir.endsWith("LightTheme")) {
                    continue;
                }
                try (Stream<Path> files = Files.list(dir)) {
                    files.map(file -> file.getFileName().toString())
                         .filter(name -> name.endsWith(".css"))
                         .forEach(offered::add);
                }
            }
        }

        assertEquals(Set.of(), NOT_BUNDLED.stream().filter(name -> !offered.contains(name)).collect(Collectors.toSet()),
                "NOT_BUNDLED names themes the submodule no longer offers");
        offered.removeAll(NOT_BUNDLED);

        Set<String> listed = new TreeSet<>();
        Arrays.stream(ThemePreset.values())
              .filter(theme -> !theme.isBuiltIn())
              .map(ThemePreset::getCommunityFileName)
              .forEach(listed::add);

        assertEquals(offered, listed, "ThemePreset does not list exactly the two-scheme themes of themes.jabref.org");
    }
}
