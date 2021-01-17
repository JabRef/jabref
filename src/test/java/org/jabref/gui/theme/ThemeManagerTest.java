package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.scene.Scene;

import org.jabref.gui.theme.ThemeManagerImpl;
import org.jabref.gui.theme.ThemePreference;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.gui.util.ThemeManager;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jabref.preferences.AppearancePreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThemeManagerTest {

    private Path tempFolder;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) {
        this.tempFolder = tempFolder;
    }

    @Test
    public void lightThemeUsedWhenPathIsBlank() {
        ThemeManager blankTheme = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference("")), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.LIGHT, blankTheme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertEquals(Optional.empty(), blankTheme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    public void lightThemeUsedWhenPathIsBaseCss() {
        ThemeManager baseTheme = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference("Base.css")), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.LIGHT, baseTheme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertEquals(Optional.empty(), baseTheme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available");
    }

    @Test
    public void darkThemeUsedWhenPathIsDarkCss() {
        // Dark theme is detected by name:
        ThemeManager theme = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference("Dark.css")), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.DARK, theme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertTrue(theme.getAdditionalStylesheet().isPresent(),
                "expected dark theme stylesheet to be available");
    }

    @Test
    public void customThemeIgnoredIfDirectory() {
        ThemeManager baseTheme = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference(tempFolder.toString())), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.CUSTOM, baseTheme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertEquals(Optional.empty(), baseTheme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available when location is a directory");
    }

    @Test
    public void customThemeIgnoredIfInvalidPath() {
        ThemeManager baseTheme = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference("\0\0\0")), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.CUSTOM, baseTheme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertEquals(Optional.empty(), baseTheme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet when CSS location is just some null terminators!");
    }

    @Test
    public void customThemeAvailableEvenWhenDeleted() throws IOException {

        /* Create a temporary custom theme that is just a small snippet of CSS. There is no CSS
         validation (at the moment) but by making a valid CSS block we don't preclude adding validation later */
        Path testCss = tempFolder.resolve("test.css");
        Files.writeString(testCss,
                "/* Biblatex Source Code */\n" +
                ".code-area .text {\n" +
                "    -fx-font-family: monospace;\n" +
                "}", StandardOpenOption.CREATE);

        // This is detected as a custom theme:
        ThemeManager theme = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference(testCss.toString())), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.CUSTOM, theme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertEquals(testCss.toString(), theme.getCurrentAppearancePreferences().getThemePreference().getName());

        Optional<String> testCssLocation1 = theme.getAdditionalStylesheet();
        assertTrue(testCssLocation1.isPresent(), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                testCssLocation1.get());

        Files.delete(testCss);

        // Consumer passed to additionalStylesheet() should still return the data url even though file is deleted.
        // It shouldn't matter whether the file existed at the time the Theme object was created (before or after)

        Optional<String> testCssLocation2 = theme.getAdditionalStylesheet();
        assertTrue(testCssLocation2.isPresent(), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                testCssLocation2.get());

        ThemeManager themeCreatedWhenAlreadyMissing = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference(testCss.toString())), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.CUSTOM, theme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertEquals(testCss.toString(), theme.getCurrentAppearancePreferences().getThemePreference().getName());
        assertEquals(Optional.empty(), themeCreatedWhenAlreadyMissing.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available because it didn't exist when theme was created");

        // Check that the consumer is called once more, if the file is restored
        Files.writeString(testCss,
                "/* Biblatex Source Code */\n" +
                        ".code-area .text {\n" +
                        "    -fx-font-family: monospace;\n" +
                        "}", StandardOpenOption.CREATE);

        Optional<String> testCssLocation3 = theme.getAdditionalStylesheet();
        assertTrue(testCssLocation3.isPresent(), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                testCssLocation3.get());

        Optional<String> testCssLocation4 = themeCreatedWhenAlreadyMissing.getAdditionalStylesheet();
        assertTrue(testCssLocation4.isPresent(), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                testCssLocation4.get());
    }

    @Test
    public void largeCustomThemeNotHeldInMemory() throws IOException {

        /* Create a temporary custom theme that is just a large comment over 48 kilobytes in size. There is no CSS
        validation (at the moment) but by making a valid CSS comment we don't preclude adding validation later */
        Path testCss = tempFolder.resolve("test.css");
        Files.createFile(testCss);
        Files.writeString(testCss, "/* ", StandardOpenOption.CREATE);
        final String testString = "ALL WORK AND NO PLAY MAKES JACK A DULL BOY\n";
        for (int i = 0; i <= (48000 / testString.length()); i++) {
            Files.writeString(testCss, testString, StandardOpenOption.APPEND);
        }
        Files.writeString(testCss, " */", StandardOpenOption.APPEND);

        // This is detected as a custom theme:
        ThemeManager theme = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference(testCss.toString())), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.CUSTOM, theme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertEquals(testCss.toString(), theme.getCurrentAppearancePreferences().getThemePreference().getName());

        Optional<String> testCssLocation1 = theme.getAdditionalStylesheet();
        assertTrue(testCssLocation1.isPresent(), "expected custom theme location to be available");
        assertTrue(testCssLocation1.get().startsWith("file:"), "expected large custom theme to be a file");

        Files.move(testCss, testCss.resolveSibling("renamed.css"));

        // additionalStylesheet() will no longer offer the deleted stylesheet, because it's not been held in memory

        assertEquals(Optional.empty(), theme.getAdditionalStylesheet(),
                "didn't expect additional stylesheet after css was deleted");

        ThemeManager themeCreatedWhenAlreadyMissing = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference(testCss.toString())), new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(ThemePreference.Type.CUSTOM, theme.getCurrentAppearancePreferences().getThemePreference().getType());
        assertEquals(testCss.toString(), theme.getCurrentAppearancePreferences().getThemePreference().getName());
        assertEquals(Optional.empty(), themeCreatedWhenAlreadyMissing.getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available because it didn't exist when theme was created");

        // Check that it is available once more, if the file is restored

        Files.move(testCss.resolveSibling("renamed.css"), testCss);

        Optional<String> testCssLocation2 = theme.getAdditionalStylesheet();
        assertTrue(testCssLocation2.isPresent(), "expected custom theme location to be available");
        assertTrue(testCssLocation2.get().startsWith("file:"), "expected large custom theme to be a file");

        Optional<String> testCssLocation3 = themeCreatedWhenAlreadyMissing.getAdditionalStylesheet();
        assertTrue(testCssLocation3.isPresent(), "expected custom theme location to be available");
        assertTrue(testCssLocation3.get().startsWith("file:"), "expected large custom theme to be a file");
    }

    /*
     TODO this test works great on a local Windows development machine, but currently fails in the github CI pipeline.
            Investigate why, and when resolved remove the @EnabledOnOs annotation that limits this test
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void liveReloadCssDataUrl() throws IOException, InterruptedException {

        /* Create a temporary custom theme that is just a small snippet of CSS. There is no CSS
         validation (at the moment) but by making a valid CSS block we don't preclude adding validation later */
        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss,
                "/* Biblatex Source Code */\n" +
                        ".code-area .text {\n" +
                        "    -fx-font-family: monospace;\n" +
                        "}", StandardOpenOption.CREATE);

        final ThemeManager theme;

        DefaultFileUpdateMonitor fileUpdateMonitor = null;
        Thread thread = null;
        try {

            fileUpdateMonitor = new DefaultFileUpdateMonitor();
            thread = new Thread(fileUpdateMonitor);
            thread.start();

            theme = new ThemeManagerImpl(new AppearancePreferences(false, 0, new ThemePreference(testCss.toString())), fileUpdateMonitor, Runnable::run);
            assertEquals(ThemePreference.Type.CUSTOM, theme.getCurrentAppearancePreferences().getThemePreference().getType());
            assertEquals(testCss.toString(), theme.getCurrentAppearancePreferences().getThemePreference().getName());

            Optional<String> testCssLocation1 = theme.getAdditionalStylesheet();
            assertTrue(testCssLocation1.isPresent(), "expected custom theme location to be available");
            assertEquals(
                    "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                    testCssLocation1.get());

            Scene scene = mock(Scene.class);
            when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());

            try {
                theme.installCss(scene);
            } catch (NullPointerException ex) {
                fail("Possible mocking issue due to NPE in installCss", ex);
            }

            Thread.sleep(2000);

            Files.writeString(testCss,
                    "/* And now for something slightly different */\n" +
                            ".code-area .text {\n" +
                            "    -fx-font-family: serif;\n" +
                            "}", StandardOpenOption.CREATE);

            Thread.sleep(2000);
        } finally {
            if (fileUpdateMonitor != null) {
                fileUpdateMonitor.shutdown();
            }
            if (thread != null) {
                thread.join();
            }
        }

        Optional<String> testCssLocation2 = theme.getAdditionalStylesheet();
        assertTrue(testCssLocation2.isPresent(), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQW5kIG5vdyBmb3Igc29tZXRoaW5nIHNsaWdodGx5IGRpZmZlcmVudCAqLwouY29kZS1hcmVhIC50ZXh0IHsKICAgIC1meC1mb250LWZhbWlseTogc2VyaWY7Cn0=",
                testCssLocation2.get(),
                "stylesheet embedded in data: url should have reloaded");
    }
}
