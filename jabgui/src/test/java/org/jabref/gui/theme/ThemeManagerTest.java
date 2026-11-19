package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.IkonliIcon;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
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
    void nullThemeSettingsFallBackToDefaults() {
        WorkspacePreferences workspacePreferences = WorkspacePreferences.getDefault();
        workspacePreferences.setTheme(null);
        workspacePreferences.setColorScheme(null);

        ThemeManager themeManager = createThemeManager(workspacePreferences);

        Scene scene = mock(Scene.class);
        when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());

        themeManager.updateCssOnScene(scene);

        assertEquals(List.of(
                ThemePreset.JABREF.getStyleSheet().getSceneStylesheetLocation(),
                ThemeManager.JABREF_BASE_STYLE_SHEET.getSceneStylesheetLocation()), scene.getStylesheets());
    }

    @Test
    void communityThemeIsInstalledOnTopOfItsParent() {
        WorkspacePreferences workspacePreferences = WorkspacePreferences.getDefault();
        workspacePreferences.setTheme(ThemePreset.NORD);

        ThemeManager themeManager = createThemeManager(workspacePreferences);

        Scene scene = mock(Scene.class);
        when(scene.getStylesheets()).thenReturn(FXCollections.observableArrayList());

        themeManager.updateCssOnScene(scene);

        assertEquals(List.of(
                ThemePreset.JABREF.getStyleSheet().getSceneStylesheetLocation(),
                ThemePreset.NORD.getStyleSheet().getSceneStylesheetLocation(),
                ThemeManager.JABREF_BASE_STYLE_SHEET.getSceneStylesheetLocation()), scene.getStylesheets());
    }

    /// An edit in the parent changes a community theme's look, so both files have to be watched.
    @Test
    void communityThemeWatchesItsParentForLiveUpdates() throws IOException {
        WorkspacePreferences workspacePreferences = WorkspacePreferences.getDefault();
        workspacePreferences.setTheme(ThemePreset.NORD);
        FileUpdateMonitor fileUpdateMonitor = mock(FileUpdateMonitor.class);

        createThemeManager(workspacePreferences, fileUpdateMonitor);

        Path parentPath = assertNotNullWatchPath(ThemePreset.JABREF);
        Path themePath = assertNotNullWatchPath(ThemePreset.NORD);
        verify(fileUpdateMonitor).addListenerForFile(eq(themePath), any());
        // The manager starts on the JabRef theme and switches, so its file is registered again as the parent.
        verify(fileUpdateMonitor, atLeastOnce()).addListenerForFile(eq(parentPath), any());
    }

    private static Path assertNotNullWatchPath(ThemePreset theme) {
        Path watchPath = theme.getStyleSheet().getWatchPath();
        assertNotNull(watchPath, theme + " is not a file, so live updates cannot be tested");
        return watchPath;
    }

    @Test
    void customThemeChangesFromBackgroundThreadAreAppliedOnJavaFxThread() throws IOException {
        WorkspacePreferences workspacePreferences = WorkspacePreferences.getDefault();
        FileUpdateMonitor fileUpdateMonitor = mock(FileUpdateMonitor.class);
        AtomicBoolean listenerAddedOnJavaFxThread = new AtomicBoolean();
        doAnswer(_ -> {
            listenerAddedOnJavaFxThread.set(Platform.isFxApplicationThread());
            return null;
        }).when(fileUpdateMonitor).addListenerForFile(any(), any());

        createThemeManager(workspacePreferences, fileUpdateMonitor);
        listenerAddedOnJavaFxThread.set(false);

        workspacePreferences.setCustomTheme(StyleSheet.create(tempFolder.resolve("custom.css").toString()));

        JavaFxExtension.awaitEvents();
        assertTrue(listenerAddedOnJavaFxThread.get());
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
        assertEquals("", customTheme.getSceneStylesheetLocation());

        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);

        assertCustomStyleSheet(styleSheet, customTheme, testCss);
        assertEquals(TEST_CSS_DATA, customTheme.getSceneStylesheetLocation());
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
        assertTrue(customTheme.getSceneStylesheetLocation().startsWith("file:"), "expected large custom theme to be a file");

        Files.move(largeCssTestFile, largeCssTestFile.resolveSibling("renamed.css"));

        assertEquals("", themeManager.getCustomTheme().getSceneStylesheetLocation(),
                "didn't expect a custom stylesheet after css was deleted");

        Files.move(largeCssTestFile.resolveSibling("renamed.css"), largeCssTestFile);

        assertCustomStyleSheet(styleSheet, customTheme, largeCssTestFile);
        String cssLocationAfterFileIsRestored = themeManager.getCustomTheme().getSceneStylesheetLocation();
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

        // theme stylesheet, custom stylesheet and base stylesheet
        assertEquals(3, scene.getStylesheets().size());
        assertTrue(scene.getStylesheets().contains(TEST_CSS_DATA));
    }

    @Test
    void liveReloadCssDataUrl() throws IOException {
        Path testCss = tempFolder.resolve("reload.css");
        Files.writeString(testCss, TEST_CSS_CONTENT, StandardOpenOption.CREATE);
        WorkspacePreferences workspacePreferences = mock(WorkspacePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(workspacePreferences.getTheme()).thenReturn(ThemePreset.JABREF);

        Optional<StyleSheet> styleSheet = StyleSheet.create(testCss.toString());
        when(workspacePreferences.getCustomTheme()).thenReturn(styleSheet);

        assertEquals(TEST_CSS_DATA, styleSheet.orElseThrow().getSceneStylesheetLocation());

        FileUpdateMonitor fileUpdateMonitor = mock(FileUpdateMonitor.class);

        createThemeManager(workspacePreferences, fileUpdateMonitor);

        ArgumentCaptor<FileUpdateListener> listenerCaptor = ArgumentCaptor.forClass(FileUpdateListener.class);
        verify(fileUpdateMonitor).addListenerForFile(eq(testCss), listenerCaptor.capture());

        Files.writeString(testCss, """
                /* And now for something slightly different */
                .code-area .text {
                    -fx-font-family: serif;
                }""", StandardOpenOption.CREATE);

        listenerCaptor.getValue().fileUpdated();

        assertEquals("data:text/css;charset=utf-8;base64,LyogQW5kIG5vdyBmb3Igc29tZXRoaW5nIHNsaWdodGx5IGRpZmZlcmVudCAqLwouY29kZS1hcmVhIC50ZXh0IHsKICAgIC1meC1mb250LWZhbWlseTogc2VyaWY7Cn0=",
                styleSheet.orElseThrow().getSceneStylesheetLocation(), "stylesheet embedded in data: url should have reloaded");
    }

    /// A third party can replace the scene root of a live window at any time -- ControlsFX injects its
    /// DecorationPane on the first validation decoration. The font size is carried by a style class on the
    /// root, so it has to move along instead of being stranded on the node that is no longer the root.
    @Test
    void fontSizeStyleClassFollowsSceneRootChange() {
        WorkspacePreferences workspacePreferences = WorkspacePreferences.getDefault();
        workspacePreferences.setShouldOverrideDefaultFontSize(true);
        workspacePreferences.setMainFontSize(16);
        createThemeManager(workspacePreferences);

        Parent initialRoot = new StackPane();
        AtomicReference<Stage> stage = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            Stage newStage = new Stage();
            newStage.setScene(new Scene(initialRoot));
            newStage.show();
            stage.set(newStage);
        });

        assertEquals(List.of("font-size-16"), fontSizeStyleClasses(initialRoot));

        Parent replacementRoot = new StackPane();
        JavaFxExtension.invokeAndWait(() -> {
            stage.get().getScene().setRoot(replacementRoot);
            stage.get().close();
        });

        assertEquals(List.of(), fontSizeStyleClasses(initialRoot), "the replaced root should not keep a stale font size");
        assertEquals(List.of("font-size-16"), fontSizeStyleClasses(replacementRoot));
    }

    private static List<String> fontSizeStyleClasses(Parent parent) {
        return parent.getStyleClass().stream().filter(styleClass -> styleClass.startsWith("font-size-")).toList();
    }

    private ThemeManager createThemeManager(WorkspacePreferences workspacePreferences) {
        return createThemeManager(workspacePreferences, new DummyFileUpdateMonitor());
    }

    private ThemeManager createThemeManager(WorkspacePreferences workspacePreferences, FileUpdateMonitor fileUpdateMonitor) {
        AtomicReference<ThemeManager> themeManager = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> themeManager.set(new ThemeManager(workspacePreferences, fileUpdateMonitor)));

        return themeManager.get();
    }

    /// Every [ThemeManager] created in this JVM watches all windows, so the test compares a window shown once
    /// with one shown three times instead of counting absolute updates.
    @Test
    void reshownWindowFollowsItsSceneOnce() {
        WorkspacePreferences workspacePreferences = WorkspacePreferences.getDefault();
        workspacePreferences.setShouldOverrideDefaultFontSize(true);
        workspacePreferences.setMainFontSize(16);
        createThemeManager(workspacePreferences);

        int shownOnce = styleClassChangesOnNewScene(1);
        assertTrue(shownOnce > 0, "new scene should receive the font style class");
        assertEquals(shownOnce, styleClassChangesOnNewScene(3));
    }

    @Test
    void reshownWindowRemovesPreviousSceneListenerBeforeRegisteringItAgain() {
        WorkspacePreferences workspacePreferences = WorkspacePreferences.getDefault();
        workspacePreferences.setShouldOverrideDefaultFontSize(true);
        workspacePreferences.setMainFontSize(16);
        createThemeManager(workspacePreferences);

        List<Integer> styleClassChanges = styleClassChangesAfterSceneReplacementOnReshownWindow(3);

        assertTrue(styleClassChanges.getFirst() > 0, "new scene should receive the font style class");
        assertEquals(List.of(styleClassChanges.getFirst(), styleClassChanges.getFirst(), styleClassChanges.getFirst()), styleClassChanges);
    }

    @Test
    void iconSizeFollowsFontSizeChanges() {
        var icon = (FontIcon) new IkonliIcon(MaterialDesignA.ACCOUNT).getGraphicNode();
        assertIconSizeFollowsFontSize(icon);
    }

    @Test
    void colouredThemeSizedIconSizeFollowsFontSizeChanges() {
        var icon = (FontIcon) new IkonliIcon(MaterialDesignA.ACCOUNT).withColor(Color.AQUA).getGraphicNode();
        assertIconSizeFollowsFontSize(icon);
    }

    @Test
    void iconLaterCreatedFollowsFontSizeChanges() {
        var workSpacePreferences = WorkspacePreferences.getDefault();
        workSpacePreferences.setShouldOverrideDefaultFontSize(true);
        var themeManager = createThemeManager(workSpacePreferences);
        changeFontSize(workSpacePreferences, 20);
        var icon = (FontIcon) new IkonliIcon(MaterialDesignA.ACCOUNT).getGraphicNode();
        showNode(themeManager, icon);

        changeFontSize(workSpacePreferences, 9);

        assertEquals(12, applyCssAndGetIconSize(icon)); // 9pt -> 12px
    }

    @Test
    void explicitlySizedIconKeepsItsSize() {
        var workSpacePreferences = WorkspacePreferences.getDefault();
        workSpacePreferences.setShouldOverrideDefaultFontSize(true);
        var themeManager = createThemeManager(workSpacePreferences);
        var icon = (FontIcon) new IkonliIcon(MaterialDesignA.ACCOUNT).withSize(27).getGraphicNode();
        showNode(themeManager, icon);

        changeFontSize(workSpacePreferences, 20);
        changeFontSize(workSpacePreferences, 9);

        assertEquals(27, applyCssAndGetIconSize(icon));
    }

    @Test
    void cssSizedIconKeepsItsSize() {
        var workSpacePreferences = WorkspacePreferences.getDefault();
        workSpacePreferences.setShouldOverrideDefaultFontSize(true);
        var themeManager = createThemeManager(workSpacePreferences);
        var icon = (FontIcon) IconTheme.JabRefIcons.OPEN_LINK.getGraphicNode(); // Like FieldValueCell#createOpenLinkButton
        icon.getStyleClass().add("action-icon");
        HBox cell = new HBox(icon);
        cell.getStyleClass().add("merge-field-value"); // From jabref-base.css
        showNode(themeManager, cell);

        changeFontSize(workSpacePreferences, 20);
        changeFontSize(workSpacePreferences, 9);

        assertEquals(14, applyCssAndGetIconSize(icon)); // 14px
    }

    @Test
    void defaultIconSizeMatchesIkonliDefault() {
        // IkonliIcon.DEFAULT_SIZE = 8
        assertEquals(8, new FontIcon().getIconSize(), "Default icon size of JabRef should match Ikonli default");
    }

    private void assertIconSizeFollowsFontSize(FontIcon icon) {
        var workSpacePreferences = WorkspacePreferences.getDefault();
        workSpacePreferences.setShouldOverrideDefaultFontSize(true);
        var themeManager = createThemeManager(workSpacePreferences);
        showNode(themeManager, icon);

        double sizeBefore = applyCssAndGetIconSize(icon);
        changeFontSize(workSpacePreferences, 20);
        double sizeIncrease = applyCssAndGetIconSize(icon);
        changeFontSize(workSpacePreferences, 9);
        double sizeAfter = applyCssAndGetIconSize(icon);

        assertTrue(sizeIncrease > sizeBefore);
        assertEquals(sizeBefore, sizeAfter);
    }

    private void showNode(ThemeManager themeManager, Node node) {
        JavaFxExtension.invokeAndWait(() -> {
            var scene = new Scene(new StackPane(node));
            themeManager.updateCssOnScene(scene);
            var stage = new Stage();
            stage.setScene(scene);
            stage.show();
        });
    }

    private static int styleClassChangesOnNewScene(int timesShown) {
        StackPane root = new StackPane();
        AtomicInteger styleClassChanges = new AtomicInteger();
        root.getStyleClass().addListener((ListChangeListener<String>) _ -> styleClassChanges.incrementAndGet());
        JavaFxExtension.invokeAndWait(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(new StackPane()));
            for (int i = 0; i < timesShown; i++) {
                stage.show();
                stage.hide();
            }
            stage.setScene(new Scene(root));
        });
        return styleClassChanges.get();
    }

    private static List<Integer> styleClassChangesAfterSceneReplacementOnReshownWindow(int timesShown) {
        List<Integer> styleClassChanges = new ArrayList<>(timesShown);
        JavaFxExtension.invokeAndWait(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(new StackPane()));
            for (int i = 0; i < timesShown; i++) {
                stage.show();
                stage.hide();

                StackPane replacementRoot = new StackPane();
                AtomicInteger replacementRootStyleClassChanges = new AtomicInteger();
                replacementRoot.getStyleClass().addListener((ListChangeListener<String>) _ -> replacementRootStyleClassChanges.incrementAndGet());
                stage.setScene(new Scene(replacementRoot));
                styleClassChanges.add(replacementRootStyleClassChanges.get());
            }
            stage.close();
        });
        return styleClassChanges;
    }

    private void assertCustomStyleSheet(Optional<StyleSheet> styleSheet, StyleSheet customTheme, Path customCss) {
        assertEquals(styleSheet.orElseThrow(), customTheme);
        assertEquals(customCss.toString(), customTheme.getName());
    }

    private Stage showIcon(ThemeManager themeManager, FontIcon icon) {
        AtomicReference<Stage> stage = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            var scene = new Scene(new StackPane(icon));
            themeManager.updateCssOnScene(scene);
            var newStage = new Stage();
            newStage.setScene(scene);
            newStage.show();
            stage.set(newStage);
        });
        return stage.get();
    }

    private void changeFontSize(WorkspacePreferences workspacePreferences, int fontSize) {
        JavaFxExtension.invokeAndWait(() -> workspacePreferences.setMainFontSize(fontSize));
        JavaFxExtension.awaitEvents();
    }

    private double applyCssAndGetIconSize(FontIcon icon) {
        AtomicReference<Double> size = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            icon.getScene().getRoot().applyCss();
            size.set(icon.getFont().getSize());
        });
        return size.get();
    }
}
