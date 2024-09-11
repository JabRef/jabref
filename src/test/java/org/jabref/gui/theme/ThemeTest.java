package org.jabref.gui.theme;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeTest {
    @Test
    void lightThemeUsedWhenPathIsBlank() {
        Theme theme = new Theme("");

        assertEquals(Theme.Type.DEFAULT, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    void lightThemeUsedWhenPathIsBaseCss() {
        Theme theme = new Theme("Base.css");

        assertEquals(Theme.Type.DEFAULT, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    void darkThemeUsedWhenPathIsDarkCss() {
        Theme theme = new Theme("Dark.css");

        assertEquals(Theme.Type.EMBEDDED, theme.getType());
        assertTrue(theme.getAdditionalStylesheet().isPresent());
        assertEquals("Dark.css", theme.getAdditionalStylesheet().get().getWatchPath().getFileName().toString(),
                "expected dark theme stylesheet to be available");
    }

    @Test
    void customThemeIgnoredIfDirectory() {
        Theme theme = new Theme(".");

        assertEquals(Theme.Type.DEFAULT, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available when location is a directory");
    }

    @Test
    void customThemeIgnoredIfInvalidPath() {
        Theme theme = new Theme("\0\0\0");

        assertEquals(Theme.Type.DEFAULT, theme.getType());
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
