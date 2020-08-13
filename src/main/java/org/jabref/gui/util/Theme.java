package org.jabref.gui.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.Scene;

import org.jabref.gui.JabRefFrame;
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
public enum Theme {
    LIGHT("Light", ""),
    DARK("Dark", "Dark.css"),
    CUSTOM("Custom", "");

    public static final String BASE_CSS = "Base.css";

    private static final Logger LOGGER = LoggerFactory.getLogger(Theme.class);

    private static Optional<URL> additionalCssToLoad = Optional.empty(); // default: Light theme
    private static FileUpdateMonitor fileUpdateMonitor;

    private String name;
    private Path additionalCssPath;

    Theme(String name, String path) {
        this.name = name;
        this.additionalCssPath = Path.of(path);
    }

    public static void initialize(FileUpdateMonitor fileUpdateMonitor, PreferencesService preferences) {
        Theme.fileUpdateMonitor = Objects.requireNonNull(fileUpdateMonitor);

        Theme theme = preferences.getTheme();

        if (theme != LIGHT) {
            Optional<URL> cssResource = Optional.empty();
            if (theme == DARK) {
                cssResource = Optional.ofNullable(JabRefFrame.class.getResource(theme.getPath().toString()));
            } else if (theme == Theme.CUSTOM) {
                try {
                    cssResource = Optional.of(theme.getPath().toUri().toURL());
                } catch (MalformedURLException e) {
                    // do nothing
                }
            }

            if (cssResource.isPresent()) {
                additionalCssToLoad = cssResource;
                LOGGER.debug("Using css {}", cssResource);
            } else {
                additionalCssToLoad = Optional.empty();
                LOGGER.warn("Cannot load css {}", theme);
            }
        }
    }

    public static void setCustomPath(Path path) {
        CUSTOM.additionalCssPath = path;
    }

    /**
     * Installs the base css file as a stylesheet in the given scene. Changes in the css file lead to a redraw of the
     * scene using the new css file.
     */
    public static void installCss(Scene scene, PreferencesService preferences) {
        AppearancePreferences appearancePreferences = preferences.getAppearancePreferences();

        addAndWatchForChanges(scene, JabRefFrame.class.getResource(BASE_CSS), 0);
        additionalCssToLoad.ifPresent(file -> addAndWatchForChanges(scene, file, 1));

        if (appearancePreferences.shouldOverrideDefaultFontSize()) {
            scene.getRoot().setStyle("-fx-font-size: " + appearancePreferences.getMainFontSize() + "pt;");
        }
    }

    private static void addAndWatchForChanges(Scene scene, URL cssFile, int index) {
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

    public String getName() {
        return name;
    }

    public Path getPath() {
        return additionalCssPath;
    }
}
