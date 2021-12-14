package org.jabref.gui.theme;

import java.io.IOException;
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
import org.jabref.preferences.PreferencesService;

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

    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final Consumer<Runnable> updateRunner;

    private final StyleSheet baseStyleSheet;
    private Theme theme;

    private final Set<Scene> scenes = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<WebEngine> webEngines = Collections.newSetFromMap(new WeakHashMap<>());

    public ThemeManager(PreferencesService preferencesService,
                        FileUpdateMonitor fileUpdateMonitor,
                        Consumer<Runnable> updateRunner) {
        this.preferencesService = Objects.requireNonNull(preferencesService);
        this.fileUpdateMonitor = Objects.requireNonNull(fileUpdateMonitor);
        this.updateRunner = Objects.requireNonNull(updateRunner);

        this.baseStyleSheet = StyleSheet.create(Theme.BASE_CSS).get();
        this.theme = preferencesService.getAppearancePreferences().getTheme();

        // Watching base CSS only works in development and test scenarios, where the build system exposes the CSS as a
        // file (e.g. for Gradle run task it will be in build/resources/main/org/jabref/gui/Base.css)
        addStylesheetToWatchlist(this.baseStyleSheet, this::baseCssLiveUpdate);
        baseCssLiveUpdate();

        updateTheme();
    }

    public void updateTheme() {
        Theme newTheme = Objects.requireNonNull(preferencesService.getAppearancePreferences().getTheme());

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

    private void baseCssLiveUpdate() {
        baseStyleSheet.reload();
        LOGGER.debug("Updating base CSS for {} scenes", scenes.size());
        DefaultTaskExecutor.runInJavaFXThread(() ->
                updateRunner.accept(() -> scenes.forEach(this::updateBaseCss))
        );
    }

    private void additionalCssLiveUpdate() {
        String newStyleSheetLocation = "";
        if (this.theme.getAdditionalStylesheet().isPresent()) {
            StyleSheet styleSheet = this.theme.getAdditionalStylesheet().get();
            styleSheet.reload();
            newStyleSheetLocation = styleSheet.getWebEngineStylesheet();
        }

        LOGGER.debug("Updating additional CSS for {} scenes and {} web engines", scenes.size(), webEngines.size());

        final String finalNewStyleSheetLocation = newStyleSheetLocation;
        DefaultTaskExecutor.runInJavaFXThread(() ->
                updateRunner.accept(() -> {
                    scenes.forEach(this::updateAdditionalCss);

                    webEngines.forEach(webEngine -> {
                        // force refresh by unloading style sheet, if the location hasn't changed
                        if (webEngine.getUserStyleSheetLocation().equals(finalNewStyleSheetLocation)) {
                            webEngine.setUserStyleSheetLocation(null);
                        }
                        webEngine.setUserStyleSheetLocation(finalNewStyleSheetLocation);
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
        AppearancePreferences appearance = preferencesService.getAppearancePreferences();

        List<String> stylesheets = List.of(
                baseStyleSheet.getSceneStylesheet().toExternalForm(),
                appearance.getTheme().getAdditionalStylesheet().map(styleSheet -> styleSheet.getSceneStylesheet().toExternalForm()).orElse("")
        );

        scene.getStylesheets().setAll(stylesheets);

        if (appearance.shouldOverrideDefaultFontSize()) {
            scene.getRoot().setStyle("-fx-font-size: " + appearance.getMainFontSize() + "pt;");
        } else {
            scene.getRoot().setStyle("");
        }
    }

    public Theme getActiveTheme() {
        return this.theme;
    }
}
