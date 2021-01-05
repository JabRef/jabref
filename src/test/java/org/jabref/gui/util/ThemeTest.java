package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class ThemeTest {

    private Path tempFolder;

    private PreferencesService preferencesMock;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) {
        this.tempFolder = tempFolder;
        this.preferencesMock = mock(PreferencesService.class);
    }

    @Test
    public void lightThemeUsedWhenPathIsBlank() {
        Theme blankTheme = new Theme("", preferencesMock);
        assertEquals(Theme.Type.LIGHT, blankTheme.getType());
        blankTheme.ifAdditionalStylesheetPresent(location -> fail(
                "didn't expect consumer to be called; was called with CSS location " + location));
    }

    @Test
    public void lightThemeUsedWhenPathIsBaseCss() {
        Theme baseTheme = new Theme("Base.css", preferencesMock);
        assertEquals(Theme.Type.LIGHT, baseTheme.getType());
        baseTheme.ifAdditionalStylesheetPresent(location -> fail(
                "didn't expect consumer to be called; was called with CSS location " + location));
    }

    @Test
    public void darkThemeUsedWhenPathIsDarkCss() {
        // Dark theme is detected by name:
        Theme theme = new Theme("Dark.css", preferencesMock);
        assertEquals(Theme.Type.DARK, theme.getType());
        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        theme.ifAdditionalStylesheetPresent(location -> consumerCalled.set(true));
        assertTrue(consumerCalled.get(),
                "expected dark theme location to be offered to our test consumer");
    }

    @Test
    public void customThemeIgnoredIfDirectory() {
        Theme baseTheme = new Theme(tempFolder.toString(), preferencesMock);
        assertEquals(Theme.Type.CUSTOM, baseTheme.getType());
        baseTheme.ifAdditionalStylesheetPresent(location -> fail(
                "didn't expect consumer to be called when CSS location is a directory; was called with CSS location " + location));
    }

    @Test
    public void customThemeIgnoredIfInvalidPath() {
        Theme baseTheme = new Theme("\0\0\0", preferencesMock);
        assertEquals(Theme.Type.CUSTOM, baseTheme.getType());
        baseTheme.ifAdditionalStylesheetPresent(location -> fail(
                "didn't expect consumer to be called when CSS location is just some null terminators!"));
    }

    @Test
    public void customTheme() throws IOException {

        // Create a temporary custom theme. It can be empty as Theme does not inspect the contents.
        Path testCss = tempFolder.resolve("test.css");
        Files.createFile(testCss);

        // This is detected as a custom theme:
        Theme theme = new Theme(testCss.toString(), preferencesMock);
        assertEquals(Theme.Type.CUSTOM, theme.getType());
        assertEquals(testCss.toString(), theme.getCssPathString());

        // Consumer passed to ifAdditionalStylesheetPresent() should be called if theme is present:

        AtomicBoolean consumerCalled1 = new AtomicBoolean(false);
        theme.ifAdditionalStylesheetPresent(location -> consumerCalled1.set(true));
        assertTrue(consumerCalled1.get(),
                "expected custom theme location to be offered to our test consumer");

        Files.delete(testCss);

        // Consumer passed to ifAdditionalStylesheetPresent() should NOT be called if theme is missing.
        // It shouldn't matter whether the file existed at the time the Theme object was created (before or after)

        theme.ifAdditionalStylesheetPresent(location -> fail(
                "didn't expect consumer to be called after css was deleted; was called with CSS location " + location));

        Theme themeCreatedWhenAlreadyMissing = new Theme(testCss.toString(), preferencesMock);
        assertEquals(Theme.Type.CUSTOM, theme.getType());
        assertEquals(testCss.toString(), theme.getCssPathString());
        theme.ifAdditionalStylesheetPresent(location -> fail(
                "didn't expect consumer to be called; was called with CSS location " + location));

        // Check that the consumer is called once more, if the file is restored

        Files.createFile(testCss);

        AtomicBoolean consumerCalled2 = new AtomicBoolean(false);
        theme.ifAdditionalStylesheetPresent(location -> consumerCalled2.set(true));
        assertTrue(consumerCalled2.get(),
                "expected custom theme location to be offered to our test consumer");

        AtomicBoolean consumerCalled3 = new AtomicBoolean(false);
        themeCreatedWhenAlreadyMissing.ifAdditionalStylesheetPresent(location -> consumerCalled3.set(true));
        assertTrue(consumerCalled3.get(),
                "expected custom theme location to be offered to our test consumer");
    }
}
