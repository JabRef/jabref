package org.jabref.gui.theme;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThemeTest {

    @AfterAll
    public static void printCoverage() {
        Theme.printCoverage();
    }

    @Test
    public void lightThemeUsedWhenPathIsBlank() {
        Theme theme = new Theme("");

        assertEquals(Theme.Type.DEFAULT, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    public void lightThemeUsedWhenPathIsBaseCss() {
        Theme theme = new Theme("Base.css");

        assertEquals(Theme.Type.DEFAULT, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    public void darkThemeUsedWhenPathIsDarkCss() {
        Theme theme = new Theme("Dark.css");

        assertEquals(Theme.Type.EMBEDDED, theme.getType());
        assertTrue(theme.getAdditionalStylesheet().isPresent());
        assertEquals("Dark.css", theme.getAdditionalStylesheet().get().getWatchPath().getFileName().toString(),
                "expected dark theme stylesheet to be available");
    }

    @Test
    public void customThemeIgnoredIfDirectory() {
        Theme theme = new Theme(".");

        assertEquals(Theme.Type.DEFAULT, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available when location is a directory");
    }

    @Test
    public void customThemeIgnoredIfInvalidPath() {
        Theme theme = new Theme("\0\0\0");

        assertEquals(Theme.Type.DEFAULT, theme.getType());
        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet when CSS location is just some null terminators!");
    }

    @Test
    public void customThemeIfFileNotFound() {
        Theme theme = new Theme("Idonotexist.css");

        assertEquals(Theme.Type.CUSTOM, theme.getType());
        assertTrue(theme.getAdditionalStylesheet().isPresent());
        assertEquals("Idonotexist.css", theme.getAdditionalStylesheet().get().getWatchPath().getFileName().toString());
    }

    @Test
    public void thisEqualsO() {
        Theme theme = new Theme("test");
        assertTrue(theme.equals(theme));
    }

    @Test
    public void oEqualsNull() {
        Theme theme = new Theme("test");
        assertFalse(theme.equals(null));
    }

    @Test
    public void differentClass() {
        Theme theme = new Theme("test");
        String differentClassObject = "test";
        assertFalse(theme.equals(differentClassObject));
    }

    @Test
    public void differentType() {
        Theme theme1 = new Theme("test1");
        Theme theme2 = new Theme("test2");
        assertFalse(theme1.equals(theme2));
    }

    @Test
    public void sameTypeAndName() {
        Theme theme1 = new Theme("test");
        Theme theme2 = new Theme("test");
        assertTrue(theme1.equals(theme2));
    }

    @Test
    public void sameAttributesDifferentObjects() {
        Theme theme1 = new Theme("test");
        Theme theme2 = new Theme("test");
        assertTrue(theme1.equals(theme2));
    }

    @Test
    public void sameTypeDifferentName() {
        Theme theme1 = new Theme("test1");
        Theme theme2 = new Theme("test2");
        assertFalse(theme1.equals(theme2));
    }

}
