package org.jabref.gui.theme;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeTest {
    @Test
    void systemThemeUsedWhenPathIsBlank() {
        Theme theme = new Theme("");

        assertEquals(Theme.Type.SYSTEM, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    void lightThemeUsedWhenPathIsLight() {
        Theme theme = Theme.light();

        assertEquals(Theme.Type.LIGHT, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    void darkThemeUsedWhenPathIsDark() {
        Theme theme = Theme.dark();

        assertEquals(Theme.Type.DARK, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    void customThemeIgnoredIfDirectory() {
        Theme theme = new Theme(".");

        assertEquals(Theme.Type.SYSTEM, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available when location is a directory");
    }

    @Test
    void customThemeIgnoredIfInvalidPath() {
        Theme theme = new Theme("\0\0\0");

        assertEquals(Theme.Type.SYSTEM, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet when CSS location is just some null terminators!");
    }

    @Test
    void customThemeIfFileNotFound() {
        Theme theme = new Theme("Idonotexist.css");

        assertEquals(Theme.Type.CUSTOM, theme.getType());
        assertTrue(theme.getAdditionalStylesheet().isPresent());
        assertEquals("Idonotexist.css", theme.getAdditionalStylesheet().get().getWatchPath().getFileName().toString());
    }
}
