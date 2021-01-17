package org.jabref.gui.theme;

import java.io.IOException;
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

import org.jabref.gui.util.ThemeManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.AppearancePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs and manages style files and provides live reloading.
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
public class ThemeManagerImpl implements ThemeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManagerImpl.class);

    private final FileUpdateMonitor fileUpdateMonitor;

    private final Consumer<Runnable> updateRunner;

    private final StyleSheet baseStyleSheet;

    private final AtomicReference<AppearancePreferences> appearancePreferences = new AtomicReference<>();

    private final Set<Scene> scenes = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<WebEngine> webEngines = Collections.newSetFromMap(new WeakHashMap<>());

    public ThemeManagerImpl(AppearancePreferences initialAppearance, FileUpdateMonitor fileUpdateMonitor, Consumer<Runnable> updateRunner) {
        this.fileUpdateMonitor = Objects.requireNonNull(fileUpdateMonitor);
        this.updateRunner = Objects.requireNonNull(updateRunner);

        this.baseStyleSheet = StyleSheet.create(ThemePreference.BASE_CSS);

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

        updateAppearancePreferences(Objects.requireNonNull(initialAppearance));
    }

    private StyleSheet additionalStylesheet() {
        return appearancePreferences.get().getThemePreference().getAdditionalStylesheet();
    }

    @Override
    public void updateAppearancePreferences(AppearancePreferences newPreferences) {

        if (newPreferences == null) {
            throw new IllegalArgumentException("Theme preference required");
        }

        AppearancePreferences oldPreferences = this.appearancePreferences.get();
        if (oldPreferences != null) {
            if (!newPreferences.equals(oldPreferences)) {
                LOGGER.info("Not updating appearance preferences because it hasn't changed");

                Path oldPath = oldPreferences.getThemePreference().getAdditionalStylesheet().getWatchPath();
                if (oldPath != null) {
                    fileUpdateMonitor.removeListener(oldPath, this::additionalCssLiveUpdate);
                    LOGGER.info("No longer watch css {} for live updates", oldPath);
                }
            }
        }
        Path newPath = newPreferences.getThemePreference().getAdditionalStylesheet().getWatchPath();
        this.appearancePreferences.set(newPreferences);

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
                newPreferences.getThemePreference(), baseStyleSheet, additionalStylesheet());
    }

    @Override
    public void installCss(Scene scene) {
        updateRunner.accept(() -> {
            if (this.scenes.add(scene)) {
                updateBaseCss(scene);
                updateAdditionalCss(scene);
            }
        });
    }

    @Override
    public void installCss(WebEngine webEngine) {
        updateRunner.accept(() -> {
            if (this.webEngines.add(webEngine)) {
                webEngine.setUserStyleSheetLocation(additionalStylesheet().getWebEngineStylesheet());
            }
        });
    }

    private void baseCssLiveUpdate() {
        baseStyleSheet.reload();
        LOGGER.debug("Updating base CSS for {} scenes", scenes.size());
        updateRunner.accept(() -> scenes.forEach(this::updateBaseCss));
    }

    private void additionalCssLiveUpdate() {
        StyleSheet styleSheet = additionalStylesheet();
        styleSheet.reload();
        String newStyleSheetLocation = styleSheet.getWebEngineStylesheet();

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
        AppearancePreferences appearance = this.appearancePreferences.get();
        List<String> stylesheets = scene.getStylesheets();
        if (stylesheets.size() == 2) {
            stylesheets.remove(1);
        }
        stylesheets.add(1,
                appearance.getThemePreference().getAdditionalStylesheet().getSceneStylesheet().toExternalForm());

        if (appearance.shouldOverrideDefaultFontSize()) {
            scene.getRoot().setStyle("-fx-font-size: " + appearance.getMainFontSize() + "pt;");
        } else {
            scene.getRoot().setStyle("");
        }
    }

    @Override
    public AppearancePreferences getCurrentAppearancePreferences() {
        return appearancePreferences.get();
    }

    @Override
    @Deprecated(forRemoval = true) // TODO ThemeTest needs updating, and should be based on installCss(WebEngine) instead
    public Optional<String> getAdditionalStylesheet() {
        return Optional.of(additionalStylesheet().getWebEngineStylesheet());
    }
}
