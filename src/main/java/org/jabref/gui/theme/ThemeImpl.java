package org.jabref.gui.theme;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javafx.scene.Scene;

import javafx.scene.web.WebEngine;
import org.jabref.gui.util.Theme;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.AppearancePreferences;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs the style file and provides live reloading.
 * JabRef provides two inbuilt themes and a user customizable one: Light, Dark and Custom. The Light theme is basically
 * the base.css theme. Every other theme is loaded as an addition to base.css.
 *
 * For type Custom, Theme will protect against removal of the CSS file, degrading as gracefully as possible. If the file
 * becomes unavailable while the application is running, some Scenes that have not yet had the CSS installed may not be
 * themed. The PreviewViewer, which uses WebEngine, supports data URLs and so generally are not affected by removal
 * of the file; however Theme will not attempt to URL-encode large style sheets so as to protect
 * memory usage (see {@link StyleSheetFile#MAX_IN_MEMORY_CSS_LENGTH}.
 *
 * @see <a href="https://docs.jabref.org/advanced/custom-themes">Custom themes</a> in the Jabref documentation.
 */
public class ThemeImpl implements Theme {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeImpl.class);

    private final PreferencesService preferencesService;

    private final FileUpdateMonitor fileUpdateMonitor;

    private final Consumer<Runnable> updateRunner;

    private final StyleSheet baseStyleSheet;

    private final AtomicReference<ThemeState> state = new AtomicReference<>();

    private final Set<Scene> scenes = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<WebEngine> webEngines = Collections.newSetFromMap(new WeakHashMap<>());

    private static final class ThemeState {

        private final ThemePreference themePreference;

        private final StyleSheet additionalStylesheet;

        public ThemeState(ThemePreference themePreference) {
            this.themePreference = themePreference;
            this.additionalStylesheet = switch (themePreference.getType()) {
                case LIGHT -> StyleSheetEmpty.EMPTY;
                case DARK -> StyleSheet.create("Dark.css");
                case CUSTOM -> StyleSheet.create(themePreference.getName());
            };
        }
    }

    public ThemeImpl(ThemePreference themePreference, PreferencesService preferencesService, FileUpdateMonitor fileUpdateMonitor, Consumer<Runnable> updateRunner) {
        if (themePreference == null) {
            throw new IllegalArgumentException("Theme preference required");
        }
        if (preferencesService == null) {
            throw new IllegalArgumentException("Preference service required");
        }
        if (fileUpdateMonitor == null) {
            throw new IllegalArgumentException("File update monitor required");
        }
        if (updateRunner == null) {
            throw new IllegalArgumentException("Update runner required");
        }
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.updateRunner = updateRunner;

        this.baseStyleSheet = StyleSheet.create(BASE_CSS);

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

        baseCssLiveUpdate();
        updateThemePreference(themePreference);
    }

    @Override
    public void updateThemePreference(ThemePreference themePreference) {

        if (themePreference == null) {
            throw new IllegalArgumentException("Theme preference required");
        }

        ThemeState oldState = this.state.get();
        if (oldState != null) {

            if (oldState.themePreference.equals(themePreference)) {
                LOGGER.info("Not updating theme preference because it hasn't changed");
                return;
            }

            Path oldPath = oldState.additionalStylesheet.getWatchPath();
            if (oldPath != null) {
                fileUpdateMonitor.removeListener(oldPath, this::additionalCssLiveUpdate);
                LOGGER.info("No longer watch css {} for live updates", oldPath);
            }
        }
        ThemeState newState = new ThemeState(themePreference);
        Path newPath = newState.additionalStylesheet.getWatchPath();
        this.state.set(newState);

        if (newPath != null) {
            try {
                fileUpdateMonitor.addListenerForFile(newPath, this::additionalCssLiveUpdate);
                LOGGER.info("Watching additional css {} for live updates", newPath);
            } catch (IOException e) {
                LOGGER.warn("Cannot watch additional css path {} for live updates", newPath, e);
            }
        }

        additionalCssLiveUpdate();

        LOGGER.info("Theme set to {} with base css {} and additional css {}",
                themePreference, baseStyleSheet, state.get().additionalStylesheet);
    }

    @Override
    public void installCss(Scene scene) {
        AppearancePreferences appearancePreferences = preferencesService.getAppearancePreferences();
        updateRunner.accept(() -> {
            if (this.scenes.add(scene)) {
                updateBaseCss(scene);
                updateAdditionalCss(scene);
            }
            if (appearancePreferences.shouldOverrideDefaultFontSize()) {
                scene.getRoot().setStyle("-fx-font-size: " + appearancePreferences.getMainFontSize() + "pt;");
            }
        });
    }

    @Override
    public void installCss(WebEngine webEngine) {
        updateRunner.accept(() -> {
            if (this.webEngines.add(webEngine)) {
                webEngine.setUserStyleSheetLocation(state.get().additionalStylesheet.getWebEngineStylesheet());
            }
        });
    }

    private void baseCssLiveUpdate() {
        baseStyleSheet.reload();
        LOGGER.debug("Updating base CSS for {} scenes", scenes.size());
        updateRunner.accept(() -> scenes.forEach(this::updateBaseCss));
    }

    private void additionalCssLiveUpdate() {
        StyleSheet styleSheet = state.get().additionalStylesheet;
        styleSheet.reload();
        String newStyleSheetLocation = state.get().additionalStylesheet.getWebEngineStylesheet();

        LOGGER.debug("Updating additional CSS for {} scenes and {} web engines", scenes.size(), webEngines.size());
        updateRunner.accept(() -> {
            scenes.forEach(this::updateAdditionalCss);

            webEngines.forEach(webEngine -> {
                // force refresh by unloading style sheet, if the location hasn't changed
                if (newStyleSheetLocation.equals(webEngine.getUserStyleSheetLocation())) {
                    webEngine.setUserStyleSheetLocation(null);
                }
                webEngine.setUserStyleSheetLocation(newStyleSheetLocation);
            });
        });
    }

    private void updateBaseCss(Scene scene) {
        List<String> stylesheets = scene.getStylesheets();
        if (!stylesheets.isEmpty()) {
            stylesheets.remove(0);
        }
        stylesheets.add(0, baseStyleSheet.getSceneStylesheet().toExternalForm());
    }

    private void updateAdditionalCss(Scene scene) {
        List<String> stylesheets = scene.getStylesheets();
        if (stylesheets.size() == 2) {
            stylesheets.remove(1);
        }
        stylesheets.add(1, state.get().additionalStylesheet.getSceneStylesheet().toExternalForm());
    }

    @Override
    public ThemePreference getPreference() {
        return state.get().themePreference;
    }

    @Override
    @Deprecated(forRemoval = true) // TODO ThemeTest needs updating, and should be based on installCss(WebEngine) instead
    public Optional<String> getAdditionalStylesheet() {
        return Optional.of(state.get().additionalStylesheet.getWebEngineStylesheet());
    }
}
