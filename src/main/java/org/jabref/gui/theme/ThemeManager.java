package org.jabref.gui.theme;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import javafx.scene.Scene;
import javafx.scene.web.WebEngine;

import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.AppearancePreferences;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs and manages style files and provides live reloading. JabRef provides two inbuilt themes and a user
 * customizable one: Light, Dark and Custom. The Light theme is basically the base.css theme. Every other theme is
 * loaded as an addition to base.css.
 * <p>
 * For type Custom, Theme will protect against removal of the CSS file, degrading as gracefully as possible. If the file
 * becomes unavailable while the application is running, some Scenes that have not yet had the CSS installed may not be
 * themed. The PreviewViewer, which uses WebEngine, supports data URLs and so generally is not affected by removal of
 * the file; however Theme package will not attempt to URL-encode large style sheets so as to protect memory usage (see
 * {@link StyleSheetFile#MAX_IN_MEMORY_CSS_LENGTH}).
 *
 * @see <a href="https://docs.jabref.org/advanced/custom-themes">Custom themes</a> in the Jabref documentation.
 */
public class ThemeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);

    private final AppearancePreferences appearancePreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final Consumer<Runnable> updateRunner;

    private final StyleSheet baseStyleSheet;
    private Theme theme;

    private final Set<Scene> scenes = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<WebEngine> webEngines = Collections.newSetFromMap(new WeakHashMap<>());

    public ThemeManager(AppearancePreferences appearancePreferences,
                        FileUpdateMonitor fileUpdateMonitor,
                        Consumer<Runnable> updateRunner) {
        this.appearancePreferences = Objects.requireNonNull(appearancePreferences);
        this.fileUpdateMonitor = Objects.requireNonNull(fileUpdateMonitor);
        this.updateRunner = Objects.requireNonNull(updateRunner);

        this.baseStyleSheet = StyleSheet.create(Theme.BASE_CSS).get();
        this.theme = appearancePreferences.getTheme();

        // Watching base CSS only works in development and test scenarios, where the build system exposes the CSS as a
        // file (e.g. for Gradle run task it will be in build/resources/main/org/jabref/gui/Base.css)
        addStylesheetToWatchlist(this.baseStyleSheet, this::baseCssLiveUpdate);
        baseCssLiveUpdate();

        EasyBind.subscribe(appearancePreferences.themeProperty(), theme -> updateThemeSettings());
        EasyBind.subscribe(appearancePreferences.shouldOverrideDefaultFontSizeProperty(), should -> updateFontSettings());
        EasyBind.subscribe(appearancePreferences.mainFontSizeProperty(), size -> updateFontSettings());
    }

    private void updateThemeSettings() {
        Theme newTheme = Objects.requireNonNull(appearancePreferences.getTheme());

        if (newTheme.equals(theme)) {
            LOGGER.info("Not updating theme because it hasn't changed");
        } else {
            theme.getAdditionalStylesheet().ifPresent(this::removeStylesheetFromWatchList);
        }

        this.theme = newTheme;
        LOGGER.info("Theme set to {} with base css {}", newTheme, baseStyleSheet);

        this.theme.getAdditionalStylesheet().ifPresent(
                styleSheet -> addStylesheetToWatchlist(styleSheet, this::additionalCssLiveUpdate));

        additionalCssLiveUpdate();
    }

    private void updateFontSettings() {
        DefaultTaskExecutor.runInJavaFXThread(() ->
                updateRunner.accept(() -> scenes.forEach(this::updateFontStyle)));
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
            LOGGER.debug("Updating base CSS for {} scenes", scenes.size());
        }

        DefaultTaskExecutor.runInJavaFXThread(() ->
                updateRunner.accept(() -> scenes.forEach(this::updateBaseCss))
        );
    }

    private void additionalCssLiveUpdate() {
        final String newStyleSheetLocation = this.theme.getAdditionalStylesheet().map(styleSheet -> {
            styleSheet.reload();
            return styleSheet.getWebEngineStylesheet();
        }).orElse("");

        LOGGER.debug("Updating additional CSS for {} scenes and {} web engines", scenes.size(), webEngines.size());

        DefaultTaskExecutor.runInJavaFXThread(() ->
                updateRunner.accept(() -> {
                    scenes.forEach(this::updateAdditionalCss);

                    webEngines.forEach(webEngine -> {
                        // force refresh by unloading style sheet, if the location hasn't changed
                        if (newStyleSheetLocation.equals(webEngine.getUserStyleSheetLocation())) {
                            webEngine.setUserStyleSheetLocation(null);
                        }
                        webEngine.setUserStyleSheetLocation(newStyleSheetLocation);
                    });
                })
        );
    }

    private void updateBaseCss(Scene scene) {
        List<String> stylesheets = scene.getStylesheets();
        if (!stylesheets.isEmpty()) {
            stylesheets.remove(0);
        }

        stylesheets.add(0, baseStyleSheet.getSceneStylesheet().toExternalForm());
    }

    private void updateAdditionalCss(Scene scene) {
        scene.getStylesheets().setAll(List.of(
                baseStyleSheet.getSceneStylesheet().toExternalForm(),
                appearancePreferences.getTheme()
                                     .getAdditionalStylesheet().map(styleSheet -> {
                                         URL stylesheetUrl = styleSheet.getSceneStylesheet();
                                         if (stylesheetUrl != null) {
                                             return stylesheetUrl.toExternalForm();
                                         } else {
                                             return "";
                                         }
                                     })
                                     .orElse("")
        ));
    }

    /**
     * Installs the base css file as a stylesheet in the given scene. Changes in the css file lead to a redraw of the
     * scene using the new css file.
     *
     * @param scene the scene to install the css into
     */
    public void installCss(Scene scene) {
        updateRunner.accept(() -> {
            if (this.scenes.add(scene)) {
                updateBaseCss(scene);
                updateAdditionalCss(scene);
            }
        });
    }

    /**
     * Installs the css file as a stylesheet in the given web engine. Changes in the css file lead to a redraw of the
     * web engine using the new css file.
     *
     * @param webEngine the web engine to install the css into
     */
    public void installCss(WebEngine webEngine) {
        updateRunner.accept(() -> {
            if (this.webEngines.add(webEngine)) {
                webEngine.setUserStyleSheetLocation(this.theme.getAdditionalStylesheet().isPresent() ?
                        this.theme.getAdditionalStylesheet().get().getWebEngineStylesheet() : "");
            }
        });
    }

    /**
     * Updates the font size settings of a scene. This method needs to be called from every custom dialog constructor,
     * since javafx overwrites the style if applied before showing the dialog
     *
     * @param scene is the scene, the font size should be applied to
     */
    public void updateFontStyle(Scene scene) {
        if (appearancePreferences.shouldOverrideDefaultFontSize()) {
            scene.getRoot().setStyle("-fx-font-size: " + appearancePreferences.getMainFontSize() + "pt;");
        } else {
            scene.getRoot().setStyle("");
        }
    }

    /**
     * @return the currently active theme
     */
    public Theme getActiveTheme() {
        return this.theme;
    }
}
