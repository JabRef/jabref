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
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.annotations.VisibleForTesting;
import com.pixelduke.window.ThemeWindowManager;
import com.pixelduke.window.ThemeWindowManagerFactory;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs and manages style files and provides live reloading. JabRef provides two
 * inbuilt themes and a user customizable one: Light, Dark and Custom. The Light theme
 * is basically the base.css theme. Every other theme is loaded as an addition to
 * base.css.
 * <p>
 * For type Custom, Theme will protect against removal of the CSS file, degrading as
 * gracefully as possible. If the file becomes unavailable while the application is
 * running, some Scenes that have not yet had the CSS installed may not be themed. The
 * PreviewViewer, which uses WebEngine, supports data URLs and so generally is not
 * affected by removal of the file; however Theme package will not attempt to URL-encode
 * large style sheets so as to protect memory usage (see
 * {@link StyleSheetFile#MAX_IN_MEMORY_CSS_LENGTH}).
 *
 * @see <a href="https://docs.jabref.org/advanced/custom-themes">Custom themes</a> in
 * the Jabref documentation.
 */
public class ThemeManager {
    public static Map<String, Node> getDownloadIconTitleMap = Map.of(
            Localization.lang("Downloading"), IconTheme.JabRefIcons.DOWNLOAD.getGraphicNode()
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);

    private final WorkspacePreferences workspacePreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ThemeWindowManager themeWindowManager;
    private final StyleSheet baseStyleSheet;
    private Theme theme;
    private boolean isDarkMode;
    private final Set<WebEngine> webEngines = Collections.newSetFromMap(new WeakHashMap<>());

    public ThemeManager(@NonNull WorkspacePreferences workspacePreferences,
                        @NonNull FileUpdateMonitor fileUpdateMonitor) {
        this.workspacePreferences = workspacePreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        // Always returns something even if the native library is not available - see https://github.com/dukke/FXThemes/issues/15
        this.themeWindowManager = ThemeWindowManagerFactory.create();

        this.baseStyleSheet = StyleSheet.create(Theme.BASE_CSS).get();
        this.theme = workspacePreferences.getTheme();
        this.isDarkMode = Theme.EMBEDDED_DARK_CSS.equals(this.theme.getName());

        initializeWindowThemeUpdater();

        // Watching base CSS only works in development and test scenarios, where the build system exposes the CSS as a
        // file (e.g. for Gradle run task it will be in build/resources/main/org/jabref/gui/Base.css)
        addStylesheetToWatchlist(this.baseStyleSheet, this::baseCssLiveUpdate);
        baseCssLiveUpdate();

        if (Platform.isFxApplicationThread()) {
            BindingsHelper.subscribeFuture(workspacePreferences.themeProperty(), _ -> updateThemeSettings());
            BindingsHelper.subscribeFuture(workspacePreferences.themeSyncOsProperty(), _ -> updateThemeSettings());
            BindingsHelper.subscribeFuture(workspacePreferences.shouldOverrideDefaultFontSizeProperty(), _ -> updateFontSettings());
            BindingsHelper.subscribeFuture(workspacePreferences.mainFontSizeProperty(), _ -> updateFontSettings());
            BindingsHelper.subscribeFuture(Platform.getPreferences().colorSchemeProperty(), _ -> updateThemeSettings());
            updateThemeSettings();
        } else {
            // Normally ThemeManager is only instantiated by JabGui and therefore already on the FX Thread, but when it's called from a test (e.g. ThemeManagerTest) then it's not on the fx thread
            UiTaskExecutor.runInJavaFXThread(() -> {
                BindingsHelper.subscribeFuture(workspacePreferences.themeProperty(), _ -> updateThemeSettings());
                BindingsHelper.subscribeFuture(workspacePreferences.themeSyncOsProperty(), _ -> updateThemeSettings());
                BindingsHelper.subscribeFuture(workspacePreferences.shouldOverrideDefaultFontSizeProperty(), _ -> updateFontSettings());
                BindingsHelper.subscribeFuture(workspacePreferences.mainFontSizeProperty(), _ -> updateFontSettings());
                BindingsHelper.subscribeFuture(Platform.getPreferences().colorSchemeProperty(), _ -> updateThemeSettings());
                updateThemeSettings();
            });
        }
    }

    /// Installs the base and additional CSS files as stylesheets in the given scene.
    ///
    /// This method is primarily intended to be called by `JabRefGUI` during startup.
    /// Using `installCss` directly would cause a delay in theme application, resulting
    /// in a brief flash of the default JavaFX theme (Modena CSS) before the intended theme appears.
    public void installCssImmediately(Scene scene) {
        List<String> toAdd = new ArrayList<>(2);
        toAdd.add(baseStyleSheet.getSceneStylesheet().toExternalForm());
        theme.getAdditionalStylesheet()
             .map(StyleSheet::getSceneStylesheet)
             .map(URL::toExternalForm)
             .ifPresent(toAdd::add);

        scene.getStylesheets().clear();
        scene.getStylesheets().addAll(toAdd);
    }

    /// Registers a runnable on JavaFX thread to install the base and additional css files as stylesheets in the given scene.
    public void installCss(@NonNull Scene scene) {
        // Because of race condition in JavaFX, IndexOutOfBounds will be thrown, despite
        // all the invocation to this method come directly from the UI thread
        UiTaskExecutor.runInJavaFXThread(() -> installCssImmediately(scene));
    }

    /// Installs the css file as a stylesheet in the given web engine. Changes in the
    /// css file lead to a redraw of the web engine using the new css file.
    ///
    /// @param webEngine the web engine to install the css into
    public void installCss(WebEngine webEngine) {
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
        if (workspacePreferences.shouldOverrideDefaultFontSize()) {
            scene.getRoot().setStyle("-fx-font-size: " + workspacePreferences.getMainFontSize() + "pt;");
        } else {
            scene.getRoot().setStyle("-fx-font-size: " + workspacePreferences.getDefaultFontSize() + "pt;");
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
                            installCss(newScene);
                            updateFontStyle(newScene);
                        }
                    });
                    Scene scene = window.getScene();
                    if (scene != null) {
                        installCss(scene);
                        updateFontStyle(scene);
                    }
                    if (window instanceof Stage stage) {
                        stage.showingProperty().addListener(_ -> applyDarkModeToWindow(stage));
                    }
                }
            }
        };
        Window.getWindows().addListener(windowsListener);

        // Apply styles to all windows that *already exist*
        applyCssAndFontToAllWindows();
        applyDarkModeToAllWindows();
        LOGGER.debug("Window theme monitoring initialized");
    }

    private void applyDarkModeToWindow(Stage stage) {
        if (stage == null || !stage.isShowing()) {
            return;
        }
        try {
            themeWindowManager.setDarkModeForWindowFrame(stage, isDarkMode);
            LOGGER.debug("Applied {} mode to window: {}", isDarkMode ? "dark" : "light", stage);
        } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
            // We need to handle these exceptions because the native library may not be available on all platforms (e.g., x86).
            // See https://github.com/dukke/FXThemes/issues/13 for details.
            LOGGER.debug("Failed to set dark mode for window frame (likely due to native library compatibility issues on intel)", e);
        }
    }

    private void applyDarkModeToAllWindows() {
        Window.getWindows().stream()
              .filter(Window::isShowing)
              .filter(window -> window instanceof Stage)
              .map(window -> (Stage) window)
              .forEach(this::applyDarkModeToWindow);
    }

    private void updateThemeSettings() {
        Theme theme = workspacePreferences.getTheme();

        if (workspacePreferences.themeSyncOsProperty().getValue()) {
            if (Platform.getPreferences().getColorScheme() == ColorScheme.DARK) {
                theme = Theme.dark();
            } else {
                theme = Theme.light();
            }
        }

        if (theme.equals(this.theme)) {
            LOGGER.info("Not updating theme because it hasn't changed");
        } else {
            this.theme.getAdditionalStylesheet().ifPresent(this::removeStylesheetFromWatchList);
        }

        this.theme = theme;
        LOGGER.info("Theme set to {} with base css {}", theme, baseStyleSheet);

        boolean isDarkTheme = Theme.EMBEDDED_DARK_CSS.equals(theme.getName());
        if (this.isDarkMode != isDarkTheme) {
            this.isDarkMode = isDarkTheme;
            applyDarkModeToAllWindows();
        }

        this.theme.getAdditionalStylesheet().ifPresent(
                styleSheet -> addStylesheetToWatchlist(styleSheet, this::additionalCssLiveUpdate));

        additionalCssLiveUpdate();
        updateFontSettings();
    }

    private void updateFontSettings() {
        UiTaskExecutor.runInJavaFXThread(this::applyCssAndFontToAllWindows);
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

    private void baseCssLiveUpdate() {
        baseStyleSheet.reload();
        if (baseStyleSheet.getSceneStylesheet() == null) {
            LOGGER.error("Base stylesheet does not exist.");
        } else {
            LOGGER.debug("Updating base CSS for all scenes");
        }
        UiTaskExecutor.runInJavaFXThread(this::applyCssAndFontToAllWindows);
    }

    private void additionalCssLiveUpdate() {
        final String newStyleSheetLocation = this.theme.getAdditionalStylesheet().map(styleSheet -> {
            styleSheet.reload();
            return styleSheet.getWebEngineStylesheet();
        }).orElse("");

        LOGGER.debug("Updating additional CSS for all scenes and {} web engines", webEngines.size());

        UiTaskExecutor.runInJavaFXThread(() -> {
            applyCssAndFontToAllWindows();
            webEngines.forEach(webEngine -> {
                // force refresh by unloading style sheet, if the location hasn't changed
                if (newStyleSheetLocation.equals(webEngine.getUserStyleSheetLocation())) {
                    webEngine.setUserStyleSheetLocation(null);
                }
                webEngine.setUserStyleSheetLocation(newStyleSheetLocation);
            });
        });
    }

    private void applyCssAndFontToAllWindows() {
        Window.getWindows().stream()
              .filter(Window::isShowing)
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(scene -> {
                  installCss(scene);
                  updateFontStyle(scene);
              });
    }

    /**
     * @return the currently active theme
     */
    @VisibleForTesting
    Theme getActiveTheme() {
        return this.theme;
    }
}
