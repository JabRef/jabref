package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.scene.Scene;

import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.AppearancePreferences;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class ThemeManagerTest {
    private Path tempFolder;
    private PreferencesService preferencesService;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) {
        this.tempFolder = tempFolder;
        this.preferencesService = Mockito.mock(PreferencesService.class);
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
        when(preferencesService.getAppearancePreferences()).thenReturn(new AppearancePreferences(false, 0, new Theme(testCss.toString())));

        // This is detected as a custom theme:
        ThemeManager themeManager = new ThemeManager(preferencesService, new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(Theme.Type.CUSTOM, themeManager.getActiveTheme().getType());
        assertEquals(testCss.toString(), themeManager.getActiveTheme().getName());
        assertTrue(themeManager.getActiveTheme().getAdditionalStylesheet().isPresent());

        String testCssLocation1 = themeManager.getActiveTheme().getAdditionalStylesheet().get().getWebEngineStylesheet();
        assertTrue(StringUtil.isNotBlank(testCssLocation1), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                testCssLocation1);

        Files.delete(testCss);

        // Consumer passed to additionalStylesheet() should still return the data url even though file is deleted.
        // It shouldn't matter whether the file existed at the time the Theme object was created (before or after)

        String testCssLocation2 = themeManager.getActiveTheme().getAdditionalStylesheet().get().getWebEngineStylesheet();
        assertTrue(StringUtil.isNotBlank(testCssLocation2), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                testCssLocation2);

        when(preferencesService.getAppearancePreferences()).thenReturn(new AppearancePreferences(false, 0, new Theme(testCss.toString())));
        ThemeManager themeManagerCreatedWhenAlreadyMissing = new ThemeManager(preferencesService, new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(Theme.Type.CUSTOM, themeManager.getActiveTheme().getType());
        assertEquals(testCss.toString(), themeManager.getActiveTheme().getName());
        assertEquals(Optional.empty(), themeManagerCreatedWhenAlreadyMissing.getActiveTheme().getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available because it didn't exist when theme was created");

        // Check that the consumer is called once more, if the file is restored
        Files.writeString(testCss,
                "/* Biblatex Source Code */\n" +
                        ".code-area .text {\n" +
                        "    -fx-font-family: monospace;\n" +
                        "}", StandardOpenOption.CREATE);

        String testCssLocation3 = themeManager.getActiveTheme().getAdditionalStylesheet().get().getWebEngineStylesheet();
        assertTrue(StringUtil.isNotBlank(testCssLocation3), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                testCssLocation3);

        assertTrue(themeManagerCreatedWhenAlreadyMissing.getActiveTheme().getAdditionalStylesheet().isPresent(),
                "expected custom theme to be available");
        String testCssLocation4 = themeManagerCreatedWhenAlreadyMissing.getActiveTheme().getAdditionalStylesheet().get().getWebEngineStylesheet();
        assertTrue(StringUtil.isNotBlank(testCssLocation4), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                testCssLocation4);
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
        when(preferencesService.getAppearancePreferences()).thenReturn(new AppearancePreferences(false, 0, new Theme(testCss.toString())));

        // This is detected as a custom theme:
        ThemeManager themeManager = new ThemeManager(preferencesService, new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(Theme.Type.CUSTOM, themeManager.getActiveTheme().getType());
        assertEquals(testCss.toString(), themeManager.getActiveTheme().getName());

        Optional<String> testCssLocation1 = themeManager.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getWebEngineStylesheet);
        assertTrue(testCssLocation1.isPresent(), "expected custom theme location to be available");
        assertTrue(testCssLocation1.get().startsWith("file:"), "expected large custom theme to be a file");

        Files.move(testCss, testCss.resolveSibling("renamed.css"));

        // additionalStylesheet() will no longer offer the deleted stylesheet, because it's not been held in memory

        assertEquals(Optional.empty(), themeManager.getActiveTheme().getAdditionalStylesheet(),
                "didn't expect additional stylesheet after css was deleted");

        ThemeManager themeCreatedWhenAlreadyMissing = new ThemeManager(preferencesService, new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(Theme.Type.CUSTOM, themeManager.getActiveTheme().getType());
        assertEquals(testCss.toString(), themeManager.getActiveTheme().getName());
        assertEquals(Optional.empty(), themeCreatedWhenAlreadyMissing.getActiveTheme().getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available because it didn't exist when theme was created");

        // Check that it is available once more, if the file is restored

        Files.move(testCss.resolveSibling("renamed.css"), testCss);

        Optional<String> testCssLocation2 = themeManager.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getWebEngineStylesheet);
        assertTrue(testCssLocation2.isPresent(), "expected custom theme location to be available");
        assertTrue(testCssLocation2.get().startsWith("file:"), "expected large custom theme to be a file");

        Optional<String> testCssLocation3 = themeCreatedWhenAlreadyMissing.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getWebEngineStylesheet);
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
        when(preferencesService.getAppearancePreferences()).thenReturn(new AppearancePreferences(false, 0, new Theme(testCss.toString())));

        final ThemeManager themeManager;

        DefaultFileUpdateMonitor fileUpdateMonitor = null;
        Thread thread = null;
        try {

            fileUpdateMonitor = new DefaultFileUpdateMonitor();
            thread = new Thread(fileUpdateMonitor);
            thread.start();

            themeManager = new ThemeManager(preferencesService, fileUpdateMonitor, Runnable::run);
            assertEquals(Theme.Type.CUSTOM, themeManager.getActiveTheme().getType());
            assertEquals(testCss.toString(), themeManager.getActiveTheme().getName());

            Optional<String> testCssLocation1 = themeManager.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getWebEngineStylesheet);
            assertTrue(testCssLocation1.isPresent(), "expected custom theme location to be available");
            assertEquals(
                    "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==",
                    testCssLocation1.get());

            Scene scene = mock(Scene.class);
            when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());

            try {
                themeManager.installCss(scene);
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

        Optional<String> testCssLocation2 = themeManager.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getWebEngineStylesheet);
        assertTrue(testCssLocation2.isPresent(), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQW5kIG5vdyBmb3Igc29tZXRoaW5nIHNsaWdodGx5IGRpZmZlcmVudCAqLwouY29kZS1hcmVhIC50ZXh0IHsKICAgIC1meC1mb250LWZhbWlseTogc2VyaWY7Cn0=",
                testCssLocation2.get(),
                "stylesheet embedded in data: url should have reloaded");
    }
}
