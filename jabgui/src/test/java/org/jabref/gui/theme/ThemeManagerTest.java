package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;

import org.jabref.gui.WorkspacePreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(workspacePreferences.getTheme()).thenReturn(new Theme(testCss.toString()));

        ThemeManager themeManager = new ThemeManager(workspacePreferences, new DummyFileUpdateMonitor());

        assertEquals(Theme.Type.CUSTOM, themeManager.getActiveTheme().getType());
        assertEquals(testCss.toString(), themeManager.getActiveTheme().getName());
        Optional<String> cssLocation = themeManager.getActiveTheme()
                                                   .getAdditionalStylesheet()
                                                   .map(StyleSheet::getSceneStylesheetLocation);
        assertEquals(Optional.of(TEST_CSS_DATA), cssLocation);
    }

    @Test
    void customThemeAvailableEvenWhenDeleted() throws IOException {
        /* Create a temporary custom theme that is just a small snippet of CSS. There is no CSS
         validation (at the moment) but by making a valid CSS block we don't preclude adding validation later */
        Path testCss = tempFolder.resolve("test.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(new Theme(testCss.toString()));

        // The stylesheet is embedded as a data: URL before the file is deleted
        ThemeManager themeManagerCreatedBeforeFileDeleted = new ThemeManager(workspacePreferences, new DummyFileUpdateMonitor());

        Files.delete(testCss);

        Optional<String> cssLocationAfterDeletion = themeManagerCreatedBeforeFileDeleted.getActiveTheme()
                                                                                        .getAdditionalStylesheet()
                                                                                        .map(StyleSheet::getSceneStylesheetLocation);
        assertEquals(Optional.of(TEST_CSS_DATA), cssLocationAfterDeletion);
    }

    @Test
    void largeCustomThemeNotHeldInMemory() throws IOException {
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
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(new Theme(largeCssTestFile.toString()));

        // Large themes are not embedded; the plain file URL is used instead
        ThemeManager themeManager = new ThemeManager(workspacePreferences, new DummyFileUpdateMonitor());
        Optional<String> cssLocationBeforeRemoved = themeManager.getActiveTheme()
                                                                .getAdditionalStylesheet()
                                                                .map(StyleSheet::getSceneStylesheetLocation);
        assertTrue(cssLocationBeforeRemoved.isPresent(), "expected custom theme location to be available");
        assertTrue(cssLocationBeforeRemoved.get().startsWith("file:"), "expected large custom theme to be a file");

        Files.move(largeCssTestFile, largeCssTestFile.resolveSibling("renamed.css"));

        // Not held in memory: after removal of the file, no stylesheet location is offered
        assertEquals(Optional.of(""), themeManager.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getSceneStylesheetLocation),
                "didn't expect additional stylesheet after css was deleted");

        Files.move(largeCssTestFile.resolveSibling("renamed.css"), largeCssTestFile);

        // Check that it is available once more, if the file is restored
        Optional<String> cssLocationAfterFileIsRestored = themeManager.getActiveTheme().getAdditionalStylesheet().map(StyleSheet::getSceneStylesheetLocation);
        assertTrue(cssLocationAfterFileIsRestored.isPresent(), "expected custom theme location to be available");
        assertTrue(cssLocationAfterFileIsRestored.get().startsWith("file:"), "expected large custom theme to be a file");
    }

    @Test
    // @DisabledOnCIServer("Randomly fails on CI server")
    @Disabled("Randomly fails on CI server")
    void installThemeOnScene() throws IOException {
        Scene scene = mock(Scene.class);
        when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());
        when(scene.getRoot()).thenReturn(mock(Parent.class));

        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(new Theme(testCss.toString()));

        ThemeManager themeManager = new ThemeManager(workspacePreferences, new DummyFileUpdateMonitor());

        themeManager.installCssOnScene(scene);

        assertEquals(2, scene.getStylesheets().size());
        assertTrue(scene.getStylesheets().contains(TEST_CSS_DATA));
    }
}
