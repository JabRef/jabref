package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.stage.Window;

import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Installs and manages style files and provides live reloading. JabRef provides two
/// inbuilt themes and a user customizable one: Light, Dark and Custom. The Light and Dark theme
/// is basically the jabref-theme.css theme. Every other theme is loaded as an addition to
/// jabref-theme.css.
///
/// For type Custom, Theme will protect against removal of the CSS file, degrading as
/// gracefully as possible. If the file becomes unavailable while the application is
/// running, some Scenes that have not yet had the CSS installed may not be themed. The
/// PreviewViewer, which uses WebEngine, supports data URLs and so generally is not
/// affected by removal of the file; however Theme package will not attempt to URL-encode
/// large style sheets so as to protect memory usage (see
/// {@link StyleSheetFile#MAX_IN_MEMORY_CSS_LENGTH}).
///
/// @see <a href="https://docs.jabref.org/advanced/custom-themes">Custom themes</a> in
/// the Jabref documentation.
public class ThemeManager {
    public static Map<String, Node> downloadIconTitleMap = Map.of(
            Localization.lang("Downloading"), IconTheme.JabRefIcons.DOWNLOAD.getGraphicNode()
    );
    public static final StyleSheet JABREF_BASE_STYLE_SHEET = StyleSheet.create("internal/jabref-base.css").orElseThrow();

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);

    private final WorkspacePreferences workspacePreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final Set<WebEngine> webEngines = Collections.newSetFromMap(new WeakHashMap<>());

    private final FileUpdateListener cssLiveUpdate = this::cssLiveUpdate;
    private final FileUpdateListener customCssLiveUpdate = this::customCssLiveUpdate;

    private ThemePreset theme;
    private ThemeColorScheme colorScheme = ThemeColorScheme.FOLLOW_SYSTEM;
    private StyleSheet customTheme;

    public ThemeManager(@NonNull WorkspacePreferences workspacePreferences,
                        @NonNull FileUpdateMonitor fileUpdateMonitor) {
        this.workspacePreferences = workspacePreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;

        initializeWindowThemeUpdater();

        BindingsHelper.subscribeFuture(workspacePreferences.themeProperty(), _ -> updateThemeSettings());
        BindingsHelper.subscribeFuture(workspacePreferences.colorSchemeProperty(), _ -> updateThemeSettings());
        BindingsHelper.subscribeFuture(workspacePreferences.customThemeProperty(), _ -> updateThemeSettings());
        BindingsHelper.subscribeFuture(workspacePreferences.shouldOverrideDefaultFontSizeProperty(), _ -> updateFontSettings());
        BindingsHelper.subscribeFuture(workspacePreferences.mainFontSizeProperty(), _ -> updateFontSettings());
        BindingsHelper.subscribeFuture(Platform.getPreferences().colorSchemeProperty(), _ -> updateThemeSettings());
        updateThemeSettings();
        updateFontSettings();
    }

    /// Installs the CSS on the given scene
    public void updateCssOnScene(Scene scene) {
        List<String> toAdd = new ArrayList<>(3);

        toAdd.add(theme.getStyleSheet().getSceneStylesheet().toExternalForm());
        if (customTheme != null) {
            toAdd.add(customTheme.getSceneStylesheet().toExternalForm());
        }
        toAdd.add(JABREF_BASE_STYLE_SHEET.getSceneStylesheet().toExternalForm());

        scene.getStylesheets().setAll(toAdd);
    }

    /// Installs the css file as a stylesheet in the given web engine. Changes in the
    /// css file lead to a redraw of the web engine using the new css file.
    ///
    /// @param webEngine the web engine to install the css into
    public void installCssOnWebEngine(WebEngine webEngine) {
        if (this.webEngines.add(webEngine)) {
            webEngine.setUserStyleSheetLocation(customTheme != null ? customTheme.getWebEngineStylesheet() : "");
        }
    }

    /// Updates the font size settings of a scene. Originally, this methods must be
    /// called by each Dialog, PopOver, or window when it's created. Now, this is done
    /// automatically when the scene is created.
    ///
    /// @param scene is the scene, the font size should be applied to
    private void updateFontOnScene(@NonNull Scene scene) {
        UiTaskExecutor.runNowOrInJavaFXThread(() -> updateFontStyleForScene(scene));
    }

    private void updateFontStyleForScene(@NonNull Scene scene) {
        if (workspacePreferences.shouldOverrideDefaultFontSize()) {
            LOGGER.debug("Overriding font size with user preference to {}pt", workspacePreferences.getMainFontSize());
            scene.getRoot().setStyle("-fx-font-size: " + workspacePreferences.getMainFontSize() + "pt;");
        } else {
            int mainFontSize = WorkspacePreferences.getDefault().getMainFontSize();
            LOGGER.debug("Using default font size of {}pt", mainFontSize);
            scene.getRoot().setStyle("-fx-font-size: " + mainFontSize + "pt;");
        }
    }

    private void initializeWindowThemeUpdater() {
        ListChangeListener<Window> windowsListener = change -> {
            while (change.next()) {
                if (!change.wasAdded()) {
                    continue;
                }
                for (Window window : change.getAddedSubList()) {
                    window.sceneProperty().addListener((_, _, newScene) -> {
                        if (newScene != null) {
                            updateColorSchemeOnScene(newScene);
                            updateFontOnScene(newScene);
                        }
                    });
                    Scene scene = window.getScene();
                    if (scene != null) {
                        updateColorSchemeOnScene(scene);
                        updateFontOnScene(scene);
                    }
                }
            }
        };
        Window.getWindows().addListener(windowsListener);

        LOGGER.debug("Window theme monitoring initialized");
    }

    private void updateColorSchemeOnScene(Scene scene) {
        ColorScheme javafxColorScheme = switch (colorScheme) {
            case FOLLOW_SYSTEM ->
                    null;
            case LIGHT ->
                    ColorScheme.LIGHT;
            case DARK ->
                    ColorScheme.DARK;
        };

        scene.getPreferences().setColorScheme(javafxColorScheme);
    }

    private void updateThemeSettings() {
        ThemePreset newTheme = workspacePreferences.getTheme();

        boolean cssChanged = false;
        if (theme != newTheme) {
            if (theme != null) {
                removeStylesheetFromWatchList(theme.getStyleSheet(), cssLiveUpdate);
            }
            addStylesheetToWatchlist(newTheme.getStyleSheet(), cssLiveUpdate);

            cssChanged = true;
            theme = newTheme;

            LOGGER.debug("Theme set to {}", newTheme);
        }

        ThemeColorScheme newColorScheme = workspacePreferences.getColorScheme();
        if (colorScheme != newColorScheme) {
            colorScheme = newColorScheme;

            updateModeToAllScenes();

            LOGGER.debug("Color Scheme set to {}", newColorScheme);
        }

        StyleSheet newCustomTheme = workspacePreferences.getCustomTheme().orElse(null);
        if (!Objects.equals(customTheme, newCustomTheme)) {
            if (customTheme != null) {
                removeStylesheetFromWatchList(customTheme, customCssLiveUpdate);
            }
            if (newCustomTheme != null) {
                addStylesheetToWatchlist(newCustomTheme, customCssLiveUpdate);
            }

            customTheme = newCustomTheme;

            cssChanged = true;

            LOGGER.debug("Custom Theme set to {}", newCustomTheme);
        }

        if (cssChanged) {
            updateCssToAllScenes();
        }
    }

    private void updateFontSettings() {
        updateFontToAllScenes();
    }

    private void removeStylesheetFromWatchList(StyleSheet styleSheet, FileUpdateListener updateMethod) {
        Path oldPath = styleSheet.getWatchPath();
        if (oldPath != null) {
            fileUpdateMonitor.removeListener(oldPath, updateMethod);
            LOGGER.info("No longer watch css {} for live updates", oldPath);
        }
    }

    private void addStylesheetToWatchlist(StyleSheet styleSheet, FileUpdateListener updateMethod) {
        Path watchPath = styleSheet.getWatchPath();
        if (watchPath == null) {
            return;
        }

        try {
            fileUpdateMonitor.addListenerForFile(watchPath, updateMethod);
            LOGGER.info("Watching css {} for live updates", watchPath);
        } catch (IOException e) {
            LOGGER.warn("Cannot watch css path {} for live updates", watchPath, e);
        }
    }

    private void cssLiveUpdate() {
        UiTaskExecutor.runInJavaFXThread(this::updateModeToAllScenes);
    }

    private void customCssLiveUpdate() {
        customTheme.reload();

        LOGGER.debug("Updating additional CSS for all scenes and {} web engines", webEngines.size());

        UiTaskExecutor.runInJavaFXThread(() -> {
            webEngines.forEach(webEngine -> {
                String newStyleSheetLocation = customTheme.getWebEngineStylesheet();
                // force refresh by unloading style sheet, if the location hasn't changed
                if (newStyleSheetLocation.equals(webEngine.getUserStyleSheetLocation())) {
                    webEngine.setUserStyleSheetLocation(null);
                }
                webEngine.setUserStyleSheetLocation(newStyleSheetLocation);
            });
        });
    }

    private void updateCssToAllScenes() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::updateCssOnScene);
    }

    private void updateModeToAllScenes() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::updateColorSchemeOnScene);
    }

    private void updateFontToAllScenes() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::updateFontOnScene);
    }

    /// @return the currently active theme
    @VisibleForTesting
    ThemePreset getTheme() {
        return this.theme;
    }

    /// @return the currently active custom theme
    @VisibleForTesting
    StyleSheet getCustomTheme() {
        return this.customTheme;
    }
}
