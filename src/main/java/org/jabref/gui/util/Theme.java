package org.jabref.gui.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.Scene;

import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.AppearancePreferences;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs the style file and provides live reloading.
 * JabRef provides two inbuilt themes and a user customizable one: Light, Dark and Custom. The Light theme is basically
 * the base.css theme. Every other theme is loaded as an addition to base.css.
 */
public class Theme {
    public enum Type {
        LIGHT, DARK, CUSTOM
    }

    public static final String BASE_CSS = "Base.css";

    private static final Logger LOGGER = LoggerFactory.getLogger(Theme.class);

    private final Type type;
    private final Path pathToCss;
    private final Optional<URL> additionalCssToLoad;
    private final PreferencesService preferencesService;

    public Theme(String path, PreferencesService preferencesService) {
        this.pathToCss = Path.of(path);
        this.preferencesService = preferencesService;

        if (StringUtil.isBlank(path) || BASE_CSS.equalsIgnoreCase(path)) {
            // Light theme
            this.type = Type.LIGHT;
            this.additionalCssToLoad = Optional.empty();
        } else {
            Optional<URL> cssResource = Optional.ofNullable(JabRefFrame.class.getResource(path));
            if (cssResource.isPresent()) {
                // Embedded dark theme
                this.type = Type.DARK;
            } else {
                // Custom theme
                this.type = Type.CUSTOM;
                if (Files.exists(pathToCss)) {
                    try {
                        cssResource = Optional.of(pathToCss.toUri().toURL());
                    } catch (MalformedURLException e) {
                        cssResource = Optional.empty();
                    }
                }
            }

            if (cssResource.isPresent()) {
                additionalCssToLoad = cssResource;
                LOGGER.debug("Using css {}", path);
            } else {
                additionalCssToLoad = Optional.empty();
                LOGGER.warn("Cannot load css {}", path);
            }
        }
    }

    /**
     * Installs the base css file as a stylesheet in the given scene. Changes in the css file lead to a redraw of the
     * scene using the new css file.
     */
    public void installCss(Scene scene, FileUpdateMonitor fileUpdateMonitor) {
        AppearancePreferences appearancePreferences = preferencesService.getAppearancePreferences();

        addAndWatchForChanges(scene, JabRefFrame.class.getResource(BASE_CSS), fileUpdateMonitor, 0);
        additionalCssToLoad.ifPresent(file -> addAndWatchForChanges(scene, file, fileUpdateMonitor, 1));

        if (appearancePreferences.shouldOverrideDefaultFontSize()) {
            scene.getRoot().setStyle("-fx-font-size: " + appearancePreferences.getMainFontSize() + "pt;");
        }
    }

    /**
     * StyleTester does not create a Globals object, so we need to wrap this method so the style tester can provide
     * its own fileUpdateMonitor, but the main codebase is not spammed.
     *
     * @deprecated Remove as soon {@link Globals} refactored.
     *
     * @param scene the scene the css should be applied to.
     */
    @Deprecated
    public void installCss(Scene scene) {
        installCss(scene, Globals.getFileUpdateMonitor());
    }

    private void addAndWatchForChanges(Scene scene, URL cssFile, FileUpdateMonitor fileUpdateMonitor, int index) {
        scene.getStylesheets().add(index, cssFile.toExternalForm());

        try {
            // If the file is an ordinary file (i.e. not part of a java runtime bundle), we watch it for changes and turn on live reloading
            URI cssUri = cssFile.toURI();
            if (!cssUri.toString().contains("jrt")) {
                LOGGER.debug("CSS URI {}", cssUri);

                Path cssPath = Path.of(cssUri).toAbsolutePath();
                LOGGER.info("Enabling live reloading of {}", cssPath);
                fileUpdateMonitor.addListenerForFile(cssPath, () -> {
                    LOGGER.info("Reload css file {}", cssFile);
                    DefaultTaskExecutor.runInJavaFXThread(() -> {
                        scene.getStylesheets().remove(cssFile.toExternalForm());
                        scene.getStylesheets().add(index, cssFile.toExternalForm());
                    });
                });
            }
        } catch (IOException | URISyntaxException | UnsupportedOperationException e) {
            LOGGER.error("Could not watch css file for changes {}", cssFile, e);
        }
    }

    public Type getType() {
        return type;
    }

    public Path getPath() {
        return pathToCss;
    }
}
