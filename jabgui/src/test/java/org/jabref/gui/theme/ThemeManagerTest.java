package org.jabref.gui.theme;

import java.io.IOException;
import java.net.URL;
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
        Optional<URL> cssLocation = themeManager.getActiveTheme()
                                                .getAdditionalStylesheet()
                                                .map(StyleSheet::getSceneStylesheet);
        assertTrue(cssLocation.isPresent(), "expected custom theme location to be available");
        assertEquals(testCss.toUri().toURL(), cssLocation.get());
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
        assertTrue(scene.getStylesheets().contains(testCss.toUri().toURL().toExternalForm()));
    }
}
