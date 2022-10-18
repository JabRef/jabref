package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;

import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.AppearancePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class ThemeManagerTest {

    private static final String TEST_CSS_DATA = "data:text/css;charset=utf-8;base64,LyogQmlibGF0ZXggU291cmNlIENvZGUgKi8KLmNvZGUtYXJlYSAudGV4dCB7CiAgICAtZngtZm9udC1mYW1pbHk6IG1vbm9zcGFjZTsKfQ==";
    private static final String TEST_CSS_CONTENT = """
            /* Biblatex Source Code */
            .code-area .text {
                -fx-font-family: monospace;
            }""";

    private Path tempFolder;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) {
        this.tempFolder = tempFolder;
    }

    @Test
    public void themeManagerUsesProvidedTheme() throws IOException {
        Path testCss = tempFolder.resolve("test.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        AppearancePreferences appearancePreferences =
                new AppearancePreferences(false, 0, new Theme(testCss.toString()), false);

        ThemeManager themeManager = new ThemeManager(appearancePreferences, new DummyFileUpdateMonitor(), Runnable::run);

        assertEquals(Theme.Type.CUSTOM, themeManager.getActiveTheme().getType());
        assertEquals(testCss.toString(), themeManager.getActiveTheme().getName());
        Optional<String> cssLocationBeforeDeletion = themeManager.getActiveTheme()
                                                                 .getAdditionalStylesheet()
                                                                 .map(StyleSheet::getWebEngineStylesheet);
        assertTrue(cssLocationBeforeDeletion.isPresent(), "expected custom theme location to be available");
        assertEquals(TEST_CSS_DATA, cssLocationBeforeDeletion.get());
    }

    @Test
    public void customThemeAvailableEvenWhenDeleted() throws IOException {
        /* Create a temporary custom theme that is just a small snippet of CSS. There is no CSS
         validation (at the moment) but by making a valid CSS block we don't preclude adding validation later */
        Path testCss = tempFolder.resolve("test.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        AppearancePreferences appearancePreferences =
                new AppearancePreferences(false, 0, new Theme(testCss.toString()), false);

        // ActiveTheme should provide the additionalStylesheet that was created before
        ThemeManager themeManagerCreatedBeforeFileDeleted = new ThemeManager(appearancePreferences, new DummyFileUpdateMonitor(), Runnable::run);

        Files.delete(testCss);

        // ActiveTheme should keep the additionalStylesheet in memory and provide it
        Optional<String> cssLocationAfterDeletion = themeManagerCreatedBeforeFileDeleted.getActiveTheme()
                                                                                        .getAdditionalStylesheet()
                                                                                        .map(StyleSheet::getWebEngineStylesheet);
        assertTrue(cssLocationAfterDeletion.isPresent(), "expected custom theme location to be available");
        assertEquals(TEST_CSS_DATA, cssLocationAfterDeletion.get());
    }

    /**
     * This test was orinially part of a more complex test. After a major refactor and simplification of the theme
     * subsystem, it was decided to drop this functionality in particular, as there is no use case for it and removing
     * this does simplify the implementation of the theme system.
     * See https://github.com/JabRef/jabref/pull/7336#issuecomment-874267375
     *
     * @throws IOException when the testfile cannot be created
     */
    @Disabled
    @Test
    public void customThemeBecomesAvailableAfterFileIsCreated() throws IOException {
        Path testCss = tempFolder.resolve("test.css");
        AppearancePreferences appearancePreferences =
                new AppearancePreferences(false, 0, new Theme(testCss.toString()), false);

        // ActiveTheme should provide no additionalStylesheet when no file exists
        ThemeManager themeManagerCreatedBeforeFileExists = new ThemeManager(appearancePreferences, new DummyFileUpdateMonitor(), Runnable::run);
        assertEquals(Optional.empty(), themeManagerCreatedBeforeFileExists.getActiveTheme()
                                                                          .getAdditionalStylesheet(),
                "didn't expect additional stylesheet to be available because it didn't exist when theme was created");

        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);

        // ActiveTheme should provide an additionalStylesheet after the file was created
        Optional<String> cssLocationAfterFileCreated = themeManagerCreatedBeforeFileExists.getActiveTheme()
                                                                                          .getAdditionalStylesheet()
                                                                                          .map(StyleSheet::getWebEngineStylesheet);
        assertTrue(cssLocationAfterFileCreated.isPresent(), "expected custom theme location to be available");
        assertEquals(TEST_CSS_DATA, cssLocationAfterFileCreated.get());
    }

    @Test
    public void largeCustomThemeNotHeldInMemory() throws IOException {
        /* Create a temporary custom theme that is just a large comment over 48 kilobytes in size. There is no CSS
        validation (at the moment) but by making a valid CSS comment we don't preclude adding validation later */
        Path largeCssTestFile = tempFolder.resolve("test.css");
        Files.createFile(largeCssTestFile);
        Files.writeString(largeCssTestFile, "/* ", StandardOpenOption.CREATE);
        final String testString = "ALL WORK AND NO PLAY MAKES JACK A DULL BOY\n";
        for (int i = 0; i <= (48000 / testString.length()); i++) {
            Files.writeString(largeCssTestFile, testString, StandardOpenOption.APPEND);
        }
        Files.writeString(largeCssTestFile, " */", StandardOpenOption.APPEND);
        AppearancePreferences appearancePreferences =
                new AppearancePreferences(false, 0, new Theme(largeCssTestFile.toString()), false);

        // ActiveTheme should provide the large additionalStylesheet that was created before
        ThemeManager themeManager = new ThemeManager(appearancePreferences, new DummyFileUpdateMonitor(), Runnable::run);
        Optional<String> cssLocationBeforeRemoved = themeManager.getActiveTheme()
                                                                .getAdditionalStylesheet()
                                                                .map(StyleSheet::getWebEngineStylesheet);
        assertTrue(cssLocationBeforeRemoved.isPresent(), "expected custom theme location to be available");
        assertTrue(cssLocationBeforeRemoved.get().startsWith("file:"), "expected large custom theme to be a file");

        Files.move(largeCssTestFile, largeCssTestFile.resolveSibling("renamed.css"));

        // getAdditionalStylesheet() should no longer offer the deleted stylesheet as it is not been held in memory
        assertEquals(themeManager.getActiveTheme().getAdditionalStylesheet().get().getWebEngineStylesheet(), "",
                "didn't expect additional stylesheet after css was deleted");

        Files.move(largeCssTestFile.resolveSibling("renamed.css"), largeCssTestFile);

        // Check that it is available once more, if the file is restored
        Optional<String> cssLocationAfterFileIsRestored = themeManager.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getWebEngineStylesheet);
        assertTrue(cssLocationAfterFileIsRestored.isPresent(), "expected custom theme location to be available");
        assertTrue(cssLocationAfterFileIsRestored.get().startsWith("file:"), "expected large custom theme to be a file");
    }

    @Test
    public void installThemeOnScene() throws IOException {
        Scene scene = mock(Scene.class);
        when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());
        when(scene.getRoot()).thenReturn(mock(Parent.class));

        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        AppearancePreferences appearancePreferences =
                new AppearancePreferences(false, 0, new Theme(testCss.toString()), false);

        ThemeManager themeManager = new ThemeManager(appearancePreferences, new DummyFileUpdateMonitor(), Runnable::run);

        themeManager.installCss(scene);

        assertEquals(2, scene.getStylesheets().size());
        assertTrue(scene.getStylesheets().contains(testCss.toUri().toURL().toExternalForm()));
    }

    @Test
    public void installThemeOnWebEngine() throws IOException {
        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        AppearancePreferences appearancePreferences =
                new AppearancePreferences(false, 0, new Theme(testCss.toString()), false);

        ThemeManager themeManager = new ThemeManager(appearancePreferences, new DummyFileUpdateMonitor(), Runnable::run);

        CompletableFuture<String> webEngineStyleSheetLocation = new CompletableFuture<>();

        Platform.runLater(() -> {
            WebEngine webEngine = new WebEngine();
            themeManager.installCss(webEngine);

            webEngineStyleSheetLocation.complete(webEngine.getUserStyleSheetLocation());
        });

        try {
            assertEquals(webEngineStyleSheetLocation.get(), TEST_CSS_DATA);
        } catch (InterruptedException | ExecutionException e) {
            fail(e);
        }
    }

    /**
     * Since the DefaultFileUpdateMonitor runs in a separate thread we have to wait for some arbitrary number of msecs
     * for the thread to start up and the changed css to reload.
     */
    @Test
    public void liveReloadCssDataUrl() throws IOException, InterruptedException {
        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        AppearancePreferences appearancePreferences =
                new AppearancePreferences(false, 0, new Theme(testCss.toString()), false);

        final ThemeManager themeManager;

        DefaultFileUpdateMonitor fileUpdateMonitor = new DefaultFileUpdateMonitor();
        Thread thread = new Thread(fileUpdateMonitor);
        thread.start();

        // Wait for the watch service to start
        Thread.sleep(500);

        themeManager = new ThemeManager(appearancePreferences, fileUpdateMonitor, Runnable::run);

        Scene scene = mock(Scene.class);
        when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());
        when(scene.getRoot()).thenReturn(mock(Parent.class));

        themeManager.installCss(scene);

        Files.writeString(testCss, """
                /* And now for something slightly different */
                .code-area .text {
                    -fx-font-family: serif;
                }""", StandardOpenOption.CREATE);

        // Wait for the stylesheet to be reloaded
        Thread.sleep(500);

        fileUpdateMonitor.shutdown();
        thread.join();

        Optional<String> testCssLocation2 = themeManager.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getWebEngineStylesheet);
        assertTrue(testCssLocation2.isPresent(), "expected custom theme location to be available");
        assertEquals(
                "data:text/css;charset=utf-8;base64,LyogQW5kIG5vdyBmb3Igc29tZXRoaW5nIHNsaWdodGx5IGRpZmZlcmVudCAqLwouY29kZS1hcmVhIC50ZXh0IHsKICAgIC1meC1mb250LWZhbWlseTogc2VyaWY7Cn0=",
                testCssLocation2.get(),
                "stylesheet embedded in data: url should have reloaded");
    }
}
