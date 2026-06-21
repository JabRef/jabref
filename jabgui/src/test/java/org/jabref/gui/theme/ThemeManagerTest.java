package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;

import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void themeManagerUsesProvidedTheme() throws IOException {
        Path testCss = tempFolder.resolve("test.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(ThemePreset.JABREF);

        Optional<StyleSheet> styleSheet = StyleSheet.create(testCss.toString());
        when(workspacePreferences.getCustomTheme()).thenReturn(styleSheet);

        ThemeManager themeManager = createThemeManager(workspacePreferences);

        assertCustomStyleSheet(styleSheet, themeManager.getCustomTheme(), testCss);
    }

    @Test
    void customThemeAvailableEvenWhenDeleted() throws IOException {
        Path testCss = tempFolder.resolve("test.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(ThemePreset.JABREF);

        Optional<StyleSheet> styleSheet = StyleSheet.create(testCss.toString());
        when(workspacePreferences.getCustomTheme()).thenReturn(styleSheet);

        ThemeManager themeManager = createThemeManager(workspacePreferences);

        assertCustomStyleSheet(styleSheet, themeManager.getCustomTheme(), testCss);

        Files.delete(testCss);

        assertCustomStyleSheet(styleSheet, themeManager.getCustomTheme(), testCss);
    }

    @Test
    void customThemeBecomesAvailableAfterFileIsCreated() throws IOException {
        Path testCss = tempFolder.resolve("test.css");
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(ThemePreset.JABREF);

        Optional<StyleSheet> styleSheet = StyleSheet.create(testCss.toString());
        when(workspacePreferences.getCustomTheme()).thenReturn(styleSheet);

        ThemeManager themeManager = createThemeManager(workspacePreferences);

        StyleSheet customTheme = themeManager.getCustomTheme();
        assertCustomStyleSheet(styleSheet, customTheme, testCss);
        assertEquals("", customTheme.getWebEngineStylesheet());

        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);

        assertCustomStyleSheet(styleSheet, customTheme, testCss);
        assertEquals(TEST_CSS_DATA, customTheme.getWebEngineStylesheet());
    }

    @Test
    void largeCustomThemeNotHeldInMemory() throws IOException {
        // Create a temporary custom theme that is just a large comment over 48 kilobytes in size.
        Path largeCssTestFile = tempFolder.resolve("test.css");
        Files.createFile(largeCssTestFile);
        Files.writeString(largeCssTestFile, "/* ", StandardOpenOption.CREATE);
        final String testString = "ALL WORK AND NO PLAY MAKES JACK A DULL BOY\n";
        for (int i = 0; i <= (48000 / testString.length()); i++) {
            Files.writeString(largeCssTestFile, testString, StandardOpenOption.APPEND);
        }
        Files.writeString(largeCssTestFile, " */", StandardOpenOption.APPEND);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(ThemePreset.JABREF);

        Optional<StyleSheet> styleSheet = StyleSheet.create(largeCssTestFile.toString());
        when(workspacePreferences.getCustomTheme()).thenReturn(styleSheet);

        ThemeManager themeManager = createThemeManager(workspacePreferences);

        StyleSheet customTheme = themeManager.getCustomTheme();
        assertCustomStyleSheet(styleSheet, customTheme, largeCssTestFile);
        assertNotNull(customTheme, "expected custom theme location to be available");
        assertTrue(customTheme.getSceneStylesheet().toExternalForm().startsWith("file:"), "expected large custom theme to be a file");

        Files.move(largeCssTestFile, largeCssTestFile.resolveSibling("renamed.css"));

        assertEquals("", themeManager.getCustomTheme().getWebEngineStylesheet(),
                "didn't expect additional stylesheet after css was deleted");

        Files.move(largeCssTestFile.resolveSibling("renamed.css"), largeCssTestFile);

        assertCustomStyleSheet(styleSheet, customTheme, largeCssTestFile);
        String cssLocationAfterFileIsRestored = themeManager.getCustomTheme().getWebEngineStylesheet();
        assertNotNull(cssLocationAfterFileIsRestored, "expected custom theme location to be available");
        assertTrue(cssLocationAfterFileIsRestored.startsWith("file:"), "expected large custom theme to be a file");
    }

    @Test
    void installThemeOnScene() throws IOException {
        Scene scene = mock(Scene.class);
        when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());
        when(scene.getRoot()).thenReturn(mock(Parent.class));

        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(ThemePreset.JABREF);

        Optional<StyleSheet> styleSheet = StyleSheet.create(testCss.toString());
        when(workspacePreferences.getCustomTheme()).thenReturn(styleSheet);

        ThemeManager themeManager = createThemeManager(workspacePreferences);

        themeManager.updateCssOnScene(scene);

        assertEquals(3, scene.getStylesheets().size());
        assertTrue(scene.getStylesheets().contains(testCss.toUri().toURL().toExternalForm()));
    }

    @Test
    void installThemeOnWebEngine() throws IOException {
        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(ThemePreset.JABREF);

        Optional<StyleSheet> styleSheet = StyleSheet.create(testCss.toString());
        when(workspacePreferences.getCustomTheme()).thenReturn(styleSheet);

        ThemeManager themeManager = createThemeManager(workspacePreferences);

        CompletableFuture<String> webEngineStyleSheetLocation = new CompletableFuture<>();

        Platform.runLater(() -> {
            WebEngine webEngine = new WebEngine();
            themeManager.installCssOnWebEngine(webEngine);

            webEngineStyleSheetLocation.complete(webEngine.getUserStyleSheetLocation());
        });

        assertDoesNotThrow(() -> assertEquals(TEST_CSS_DATA, webEngineStyleSheetLocation.get()));
    }

    /// Since the DefaultFileUpdateMonitor runs in a separate thread we have to wait for some arbitrary number of msecs
    /// for the thread to start up and the changed css to reload.
    @Test
    void liveReloadCssDataUrl() throws IOException, InterruptedException {
        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(ThemePreset.JABREF);

        Optional<StyleSheet> styleSheet = StyleSheet.create(testCss.toString());
        when(workspacePreferences.getCustomTheme()).thenReturn(styleSheet);

        assertEquals(TEST_CSS_DATA, styleSheet.orElseThrow().getWebEngineStylesheet());

        DefaultFileUpdateMonitor fileUpdateMonitor = new DefaultFileUpdateMonitor();
        Thread thread = new Thread(fileUpdateMonitor);
        thread.start();

        // Wait for the watch service to start
        Thread.sleep(500);

        final ThemeManager themeManager = createThemeManager(workspacePreferences, fileUpdateMonitor);

        Scene scene = mock(Scene.class);
        when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());
        when(scene.getRoot()).thenReturn(mock(Parent.class));

        themeManager.updateCssOnScene(scene);

        Files.writeString(testCss, """
                /* And now for something slightly different */
                .code-area .text {
                    -fx-font-family: serif;
                }""", StandardOpenOption.CREATE);

        // Wait for the stylesheet to be reloaded
        Thread.sleep(500);

        fileUpdateMonitor.shutdown();
        thread.join();

        assertEquals("data:text/css;charset=utf-8;base64,LyogQW5kIG5vdyBmb3Igc29tZXRoaW5nIHNsaWdodGx5IGRpZmZlcmVudCAqLwouY29kZS1hcmVhIC50ZXh0IHsKICAgIC1meC1mb250LWZhbWlseTogc2VyaWY7Cn0=",
                styleSheet.orElseThrow().getWebEngineStylesheet(), "stylesheet embedded in data: url should have reloaded");
    }

    private ThemeManager createThemeManager(WorkspacePreferences workspacePreferences) {
        return createThemeManager(workspacePreferences, new DummyFileUpdateMonitor());
    }

    private ThemeManager createThemeManager(WorkspacePreferences workspacePreferences, FileUpdateMonitor fileUpdateMonitor) {
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<ThemeManager> themeManager = new AtomicReference<>();
        Platform.runLater(() -> {
            themeManager.set(new ThemeManager(workspacePreferences, fileUpdateMonitor));
            latch.countDown();
        });

        // Run in FX UI Thread and wait until finished.
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return themeManager.get();
    }

    private void assertCustomStyleSheet(Optional<StyleSheet> styleSheet, StyleSheet customTheme, Path customCss) {
        assertEquals(styleSheet.orElseThrow(), customTheme);
        assertEquals(customCss.toString(), customTheme.getName());
    }
}
