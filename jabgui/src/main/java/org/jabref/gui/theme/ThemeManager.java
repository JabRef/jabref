package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

/// Installs and manages style files and provides live reloading. JabRef provides themes and the ability
/// to add a custom stylesheet on top.
///
/// For a custom stylesheet, we will protect against removal of the CSS file, degrading as
/// gracefully as possible: the stylesheet is embedded as a `data:` URL, so scenes keep their
/// theme if the file becomes unavailable while the application is running. Large style sheets
/// are not URL-encoded to protect memory usage
/// (see [StyleSheetFile#MAX_IN_MEMORY_CSS_LENGTH]).
///
/// @see <a href="https://docs.jabref.org/advanced/custom-themes">Custom themes</a> in
/// the JabRef documentation.
public class ThemeManager {
    public static Map<String, Node> downloadIconTitleMap = Map.of(
            Localization.lang("Downloading"), IconTheme.JabRefIcons.DOWNLOAD.getGraphicNode()
    );
    public static final StyleSheet JABREF_BASE_STYLE_SHEET = StyleSheet.create("internal/jabref-base.css").orElseThrow();

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);

    private final WorkspacePreferences workspacePreferences;
    private final FileUpdateMonitor fileUpdateMonitor;

    private final FileUpdateListener baseCssLiveUpdate = this::baseCssLiveUpdate;
    private final FileUpdateListener cssLiveUpdate = this::cssLiveUpdate;
    private final FileUpdateListener customCssLiveUpdate = this::customCssLiveUpdate;

    private ThemePreset theme = ThemePreset.JABREF;
    private ThemeColorScheme colorScheme = ThemeColorScheme.FOLLOW_SYSTEM;
    private @Nullable StyleSheet customTheme;

    public ThemeManager(@NonNull WorkspacePreferences workspacePreferences,
                        @NonNull FileUpdateMonitor fileUpdateMonitor) {
        this.workspacePreferences = workspacePreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;

        BindingsHelper.subscribeFuture(workspacePreferences.themeProperty(), _ -> updateThemeSettings());
        BindingsHelper.subscribeFuture(workspacePreferences.colorSchemeProperty(), _ -> updateThemeSettings());
        BindingsHelper.subscribeFuture(workspacePreferences.customThemeProperty(), _ -> updateThemeSettings());
        BindingsHelper.subscribeFuture(workspacePreferences.shouldOverrideDefaultFontSizeProperty(), _ -> updateFontSettings());
        BindingsHelper.subscribeFuture(workspacePreferences.mainFontSizeProperty(), _ -> updateFontSettings());
        BindingsHelper.subscribeFuture(Platform.getPreferences().colorSchemeProperty(), _ -> updateThemeSettings());

        initializeWindowThemeUpdater();
        addStylesheetToWatchlist(JABREF_BASE_STYLE_SHEET, baseCssLiveUpdate);

        updateThemeSettings();
        updateFontSettings();
    }

    /// Installs the CSS on the given scene.
    ///
    /// The theme stylesheet comes first, the user's custom stylesheet on top of it, and the base
    /// stylesheet last -- the base sheet only maps JabRef's own selectors onto the color tokens the
    /// theme defines, so it has to win over both.
    public void updateCssOnScene(Scene scene) {
        List<String> toAdd = new ArrayList<>(3);

        toAdd.add(theme.getStyleSheet().getSceneStylesheetLocation());
        if (customTheme != null) {
            toAdd.add(customTheme.getSceneStylesheetLocation());
        }
        toAdd.add(JABREF_BASE_STYLE_SHEET.getSceneStylesheetLocation());

        scene.getStylesheets().setAll(toAdd.stream().filter(not(String::isEmpty)).toList());
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
            case null ->
                    null;
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

            updateColorSchemeOnAllScenes();

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
            updateCssOnAllScenes();
        }
    }

    private void updateFontSettings() {
        updateFontOnAllScenes();
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

    private void baseCssLiveUpdate() {
        JABREF_BASE_STYLE_SHEET.reload();
        LOGGER.debug("Updating base CSS for all scenes");
        UiTaskExecutor.runInJavaFXThread(this::updateCssOnAllScenes);
    }

    private void cssLiveUpdate() {
        theme.getStyleSheet().reload();
        LOGGER.debug("Updating theme CSS for all scenes");
        UiTaskExecutor.runInJavaFXThread(this::updateCssOnAllScenes);
    }

    private void customCssLiveUpdate() {
        if (customTheme == null) {
            return;
        }
        customTheme.reload();
        LOGGER.debug("Updating custom CSS for all scenes");
        UiTaskExecutor.runInJavaFXThread(this::updateCssOnAllScenes);
    }

    private void updateCssOnAllScenes() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::updateCssOnScene);
    }

    private void updateColorSchemeOnAllScenes() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::updateColorSchemeOnScene);
    }

    private void updateFontOnAllScenes() {
        Window.getWindows().stream()
              .map(Window::getScene)
              .filter(Objects::nonNull)
              .forEach(this::updateFontOnScene);
    }

    /// @return the currently active custom theme
    @VisibleForTesting
    StyleSheet getCustomTheme() {
        return this.customTheme;
    }
}
