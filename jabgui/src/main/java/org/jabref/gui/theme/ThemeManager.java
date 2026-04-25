package org.jabref.gui.theme;

import java.io.IOException;
import java.net.URL;
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
    public static Map<String, Node> getDownloadIconTitleMap = Map.of(
            Localization.lang("Downloading"), IconTheme.JabRefIcons.DOWNLOAD.getGraphicNode()
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);

    private final WorkspacePreferences workspacePreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final StyleSheet jabRefTheme;
    private Theme theme;
    private final Set<WebEngine> webEngines = Collections.newSetFromMap(new WeakHashMap<>());

    public ThemeManager(@NonNull WorkspacePreferences workspacePreferences,
                        @NonNull FileUpdateMonitor fileUpdateMonitor) {
        this.workspacePreferences = workspacePreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;

        this.jabRefTheme = Theme.getJabRefTheme();
        this.theme = workspacePreferences.getTheme();

        initializeWindowThemeUpdater();

        // Watching base CSS only works in development and test scenarios, where the build system exposes the CSS as a
        // file (e.g. for Gradle run task it will be in build/resources/main/org/jabref/gui/jabref-theme.css)
        addStylesheetToWatchlist(this.jabRefTheme, this::cssLiveUpdate);

        // Normally ThemeManager is only instantiated by JabGui and therefore already on the FX Thread,
        // but when it's called from a test (e.g. ThemeManagerTest) then it's not on the fx thread
        UiTaskExecutor.runNowOrInJavaFXThread(() -> {
            BindingsHelper.subscribeFuture(workspacePreferences.themeProperty(), _ -> updateThemeSettings());
            BindingsHelper.subscribeFuture(workspacePreferences.themeSyncOsProperty(), _ -> updateThemeSettings());
            BindingsHelper.subscribeFuture(workspacePreferences.shouldOverrideDefaultFontSizeProperty(), _ -> updateFontSettings());
            BindingsHelper.subscribeFuture(workspacePreferences.mainFontSizeProperty(), _ -> updateFontSettings());
            BindingsHelper.subscribeFuture(Platform.getPreferences().colorSchemeProperty(), _ -> updateThemeSettings());
            updateThemeSettings();
            applyFontToAllWindows();
        });
    }

    /// Installs the base and additional CSS files as stylesheets in the given scene.
    ///
    /// This method is primarily intended to be called by `JabRefGUI` during startup.
    /// Using `installCss` directly would cause a delay in theme application, resulting
    /// in a brief flash of the default JavaFX theme (Modena CSS) before the intended theme appears.
    public void installCssOnScene(Scene scene) {
        List<String> toAdd = new ArrayList<>(2);
        toAdd.add(jabRefTheme.getSceneStylesheet().toExternalForm());
        theme.getAdditionalStylesheet()
             .map(StyleSheet::getSceneStylesheet)
             .map(URL::toExternalForm)
             .ifPresent(toAdd::add);

        scene.getStylesheets().setAll(toAdd);
    }

    /// Installs the css file as a stylesheet in the given web engine. Changes in the
    /// css file lead to a redraw of the web engine using the new css file.
    ///
    /// @param webEngine the web engine to install the css into
    public void installCssOnWebEngine(WebEngine webEngine) {
        if (this.webEngines.add(webEngine)) {
            webEngine.setUserStyleSheetLocation(this.theme.getAdditionalStylesheet().isPresent() ?
                                                this.theme.getAdditionalStylesheet().get().getWebEngineStylesheet() : "");
        }
    }

    /// Updates the font size settings of a scene. Originally, this methods must be
    /// called by each Dialog, PopOver, or window when it's created. Now, this is done
    /// automatically when the scene is created.
    ///
    /// @param scene is the scene, the font size should be applied to
    private void updateFontStyle(@NonNull Scene scene) {
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
                            applyModeToWindow(newScene);
                            updateFontStyle(newScene);
                        }
                    });
                    Scene scene = window.getScene();
                    if (scene != null) {
                        applyModeToWindow(scene);
                        updateFontStyle(scene);
                    }
                }
            }
        };
        Window.getWindows().addListener(windowsListener);

        LOGGER.debug("Window theme monitoring initialized");
    }

    private void applyModeToWindow(Scene scene) {
        Theme.Type type = theme.getType();
        if (type == Theme.Type.CUSTOM) {
            return;
        }

        if (Objects.equals(type, Theme.Type.LIGHT)) {
            scene.getPreferences().setColorScheme(ColorScheme.LIGHT);
        } else if (Objects.equals(type, Theme.Type.DARK)) {
            scene.getPreferences().setColorScheme(ColorScheme.DARK);
        } else {
            scene.getPreferences().setColorScheme(null);
        }
    }

    private void updateThemeSettings() {
        Theme newTheme = workspacePreferences.getTheme();

        // In this case we let JavaFX decide and don't do any changes.
        if (workspacePreferences.shouldThemeSyncOs()) {
            newTheme = Theme.system();
        }

        if (newTheme.equals(this.theme)) {
            LOGGER.debug("Not updating newTheme because it hasn't changed");
        } else {
            newTheme.getAdditionalStylesheet().ifPresent(this::removeStylesheetFromWatchList);
        }

        boolean customCssChanged = false;
        if (theme.getType() == Theme.Type.CUSTOM || newTheme.getType() == Theme.Type.CUSTOM) {
            customCssChanged = true;
        }

        this.theme = newTheme;
        LOGGER.debug("Theme set to {} with base css {}", newTheme, jabRefTheme);

        this.theme.getAdditionalStylesheet().ifPresent(
                styleSheet -> addStylesheetToWatchlist(styleSheet, this::additionalCssLiveUpdate));

        if (customCssChanged) {
            reinstallCssToAllWindows();
        }
        applyModeToAllWindows();
    }

    private void reinstallCssToAllWindows() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::installCssOnScene);
    }

    private void updateFontSettings() {
        UiTaskExecutor.runNowOrInJavaFXThread(this::applyFontToAllWindows);
    }

    private void removeStylesheetFromWatchList(StyleSheet styleSheet) {
        Path oldPath = styleSheet.getWatchPath();
        if (oldPath != null) {
            fileUpdateMonitor.removeListener(oldPath, this::additionalCssLiveUpdate);
            LOGGER.info("No longer watch css {} for live updates", oldPath);
        }
    }

    private void addStylesheetToWatchlist(StyleSheet styleSheet, FileUpdateListener updateMethod) {
        Path watchPath = styleSheet.getWatchPath();
        if (watchPath != null) {
            try {
                fileUpdateMonitor.addListenerForFile(watchPath, updateMethod);
                LOGGER.info("Watching css {} for live updates", watchPath);
            } catch (IOException e) {
                LOGGER.warn("Cannot watch css path {} for live updates", watchPath, e);
            }
        }
    }

    private void cssLiveUpdate() {
        jabRefTheme.reload();
        if (jabRefTheme.getSceneStylesheet() == null) {
            LOGGER.error("Base stylesheet does not exist.");
        } else {
            LOGGER.debug("Updating base CSS for all scenes");
        }
        UiTaskExecutor.runInJavaFXThread(this::applyModeToAllWindows);
    }

    private void additionalCssLiveUpdate() {
        final String newStyleSheetLocation = this.theme.getAdditionalStylesheet().map(styleSheet -> {
            styleSheet.reload();
            return styleSheet.getWebEngineStylesheet();
        }).orElse("");

        LOGGER.debug("Updating additional CSS for all scenes and {} web engines", webEngines.size());

        UiTaskExecutor.runInJavaFXThread(() -> {
            webEngines.forEach(webEngine -> {
                // force refresh by unloading style sheet, if the location hasn't changed
                if (newStyleSheetLocation.equals(webEngine.getUserStyleSheetLocation())) {
                    webEngine.setUserStyleSheetLocation(null);
                }
                webEngine.setUserStyleSheetLocation(newStyleSheetLocation);
            });
        });
    }

    private void applyModeToAllWindows() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::applyModeToWindow);
    }

    private void applyFontToAllWindows() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::updateFontStyle);
    }

    /// @return the currently active theme
    @VisibleForTesting
    Theme getActiveTheme() {
        return this.theme;
    }
}
