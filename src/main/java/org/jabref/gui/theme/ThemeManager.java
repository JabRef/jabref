package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javafx.scene.Scene;
import javafx.scene.web.WebEngine;

import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.AppearancePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs and manages style files and provides live reloading. JabRef provides two inbuilt themes and a user
 * customizable one: Light, Dark and Custom. The Light theme is basically the base.css theme. Every other theme is
 * loaded as an addition to base.css.
 * <p>
 * For type Custom, Theme will protect against removal of the CSS file, degrading as gracefully as possible. If the file
 * becomes unavailable while the application is running, some Scenes that have not yet had the CSS installed may not be
 * themed. The PreviewViewer, which uses WebEngine, supports data URLs and so generally are not affected by removal of
 * the file; however Theme will not attempt to URL-encode large style sheets so as to protect memory usage (see {@link
 * StyleSheetFile#MAX_IN_MEMORY_CSS_LENGTH}.
 *
 * @see <a href="https://docs.jabref.org/advanced/custom-themes">Custom themes</a> in the Jabref documentation.
 */
public class ThemeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);

    private final FileUpdateMonitor fileUpdateMonitor;
    private final Consumer<Runnable> updateRunner;

    private final StyleSheet baseStyleSheet;
    private final AtomicReference<AppearancePreferences> currentAppearancePreferences = new AtomicReference<>();

    private final Set<Scene> scenes = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<WebEngine> webEngines = Collections.newSetFromMap(new WeakHashMap<>());

    public ThemeManager(AppearancePreferences initialPreferences,
                        FileUpdateMonitor fileUpdateMonitor,
                        Consumer<Runnable> updateRunner) {
        this.fileUpdateMonitor = Objects.requireNonNull(fileUpdateMonitor);
        this.updateRunner = Objects.requireNonNull(updateRunner);

        this.baseStyleSheet = StyleSheet.create(Theme.BASE_CSS).get();

        /* Watching base CSS only works in development and test scenarios, where the build system exposes the CSS as a
        file (e.g. for Gradle run task it will be in build/resources/main/org/jabref/gui/Base.css) */
        Path baseCssPath = this.baseStyleSheet.getWatchPath();
        if (baseCssPath != null) {
            try {
                fileUpdateMonitor.addListenerForFile(baseCssPath, this::baseCssLiveUpdate);
                LOGGER.info("Watching base css {} for live updates", baseCssPath);
            } catch (IOException e) {
                LOGGER.warn("Cannot watch base css path {} for live updates", baseCssPath, e);
            }
        }

        updatePreferences(Objects.requireNonNull(initialPreferences));
    }

    /**
     * This method allows callers to obtain the theme's additional stylesheet.
     *
     * @return called with the stylesheet location if there is an additional stylesheet present and available. The
     * location will be a local URL. Typically it will be a {@code 'data:'} URL where the CSS is embedded. However for
     * large themes it can be {@code 'file:'}.
     */
    Optional<StyleSheet> getAdditionalStylesheet() {
        return currentAppearancePreferences.get().getTheme().getAdditionalStylesheet();
    }

    public void updatePreferences(AppearancePreferences newPreferences) {
        Objects.requireNonNull(newPreferences);

        AppearancePreferences oldPreferences = this.currentAppearancePreferences.get();
        if (oldPreferences != null) {
            if (!newPreferences.equals(oldPreferences)) {
                LOGGER.info("Not updating appearance preferences because it hasn't changed");

                oldPreferences.getTheme().getAdditionalStylesheet().ifPresent(styleSheet -> {
                    Path oldPath = styleSheet.getWatchPath();
                    if (oldPath != null) {
                        fileUpdateMonitor.removeListener(oldPath, this::additionalCssLiveUpdate);
                        LOGGER.info("No longer watch css {} for live updates", oldPath);
                    }
                });
            }
        }

        this.currentAppearancePreferences.set(newPreferences);
        LOGGER.info("Theme set to {} with base css {}", newPreferences.getTheme(), baseStyleSheet);

        newPreferences.getTheme().getAdditionalStylesheet().ifPresent(styleSheet -> {
            Path newPath = styleSheet.getWatchPath();
            if (newPath != null && !Files.isDirectory(newPath) && Files.exists(newPath)) {
                try {
                    fileUpdateMonitor.addListenerForFile(newPath, this::additionalCssLiveUpdate);
                    LOGGER.info("Watching additional css {} for live updates", newPath);
                } catch (IOException e) {
                    LOGGER.warn("Cannot watch additional css path {} for live updates", newPath, e);
                }
            } else {
                LOGGER.warn("Cannot watch additional css path {} for live updates, since this is no valid file", newPath);
            }
        });

        additionalCssLiveUpdate();
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
                webEngine.setUserStyleSheetLocation(getAdditionalStylesheet().isPresent() ?
                        getAdditionalStylesheet().get().getWebEngineStylesheet() : "");
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
        if (getAdditionalStylesheet().isPresent()) {
            StyleSheet styleSheet = getAdditionalStylesheet().get();
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
        AppearancePreferences appearance = this.currentAppearancePreferences.get();
        List<String> stylesheets = scene.getStylesheets();
        if (stylesheets.size() == 2) {
            stylesheets.remove(1);
        }

        appearance.getTheme().getAdditionalStylesheet().ifPresent(
                styleSheet -> stylesheets.add(1, styleSheet.getSceneStylesheet().toExternalForm()));

        if (appearance.shouldOverrideDefaultFontSize()) {
            scene.getRoot().setStyle("-fx-font-size: " + appearance.getMainFontSize() + "pt;");
        } else {
            scene.getRoot().setStyle("");
        }
    }

    public AppearancePreferences getCurrentAppearancePreferences() {
        return currentAppearancePreferences.get();
    }
}
