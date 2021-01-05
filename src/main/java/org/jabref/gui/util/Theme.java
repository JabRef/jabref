package org.jabref.gui.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;

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

    /* String, Path, and URL formats of the path to the css. These are determined at construction.
     Path and URL are only set if they are relevant and valid (i.e. no illegal characters).
     In general, use additionalCssToLoad(), to also ensure file existence checks are performed
     */
    private final String cssPathString;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Path> cssPath;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<URL> cssUrl;

    private final PreferencesService preferencesService;

    public Theme(String path, PreferencesService preferencesService) {
        this.cssPathString = path;
        this.preferencesService = preferencesService;

        if (StringUtil.isBlank(path) || BASE_CSS.equalsIgnoreCase(path)) {
            // Light theme
            this.type = Type.LIGHT;
            this.cssUrl = Optional.empty();
            this.cssPath = Optional.empty();
        } else {
            URL url = JabRefFrame.class.getResource(path);
            if (url != null) {
                // Embedded dark theme
                this.type = Type.DARK;
                this.cssPath = Optional.empty();
                this.cssUrl = Optional.of(url);
            } else {
                // Custom theme
                this.type = Type.CUSTOM;
                this.cssPath = cssStringToPath(path);
                // note that for an invalid path, the url will also be empty here:
                this.cssUrl = cssPath.map(Theme::cssPathToUrl);
            }

            LOGGER.debug("Theme is {}, additional css path is {}, url is {}",
                    this.type, cssPath.orElse(null), cssUrl.orElse(null));
        }
    }

    /**
     * Creates a Path from a path String
     *
     * @param pathString the path string
     * @return the path on the default file system, or empty if not valid for that file system (e.g. bad characters)
     */
    private static Optional<Path> cssStringToPath(String pathString) {
        try {
            return Optional.of(Path.of(pathString));
        } catch (InvalidPathException e) {
            LOGGER.warn("Cannot load additional css {} because it is an invalid path: {}", pathString, e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    /**
     * Creates a URL from a file system path. The scheme of the URL depends on the file system provider, but it will
     * generally be
     *
     * @param path the file system path
     * @return the URL for that file system path
     */
    private static URL cssPathToUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            LOGGER.warn("Cannot load additional css url {} because it is a malformed url: {}", path, e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Returns the additional CSS file or resource, after checking that it is accessible.
     *
     * Note that the file checks are immediately out of date, i.e. the CSS file could become unavailable between
     * the check and attempts to use that file. The checks are just on a best-effort basis.
     *
     * @return an optional providing the URL of the CSS file/resource, or empty
     */
    private Optional<URL> additionalCssToLoad() {
        // When we have a valid file system path, check that the CSS file is readable
        if (cssPath.isPresent()) {
            Path path = cssPath.get();

            if (!Files.exists(path)) {
                LOGGER.warn("Not loading additional css file {} because it could not be found", cssPath.get());
                return Optional.empty();
            }

            if (Files.isDirectory(path)) {
                LOGGER.warn("Not loading additional css file {} because it is a directory", cssPath.get());
                return Optional.empty();
            }
        }

        return cssUrl;
    }

    /**
     * Installs the base css file as a stylesheet in the given scene. Changes in the css file lead to a redraw of the
     * scene using the new css file.
     */
    public void installCss(Scene scene, FileUpdateMonitor fileUpdateMonitor) {
        AppearancePreferences appearancePreferences = preferencesService.getAppearancePreferences();

        addAndWatchForChanges(scene, JabRefFrame.class.getResource(BASE_CSS), fileUpdateMonitor, 0);
        additionalCssToLoad().ifPresent(file -> addAndWatchForChanges(scene, file, fileUpdateMonitor, 1));

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
                LOGGER.info("Enabling live reloading of css file {}", cssPath);
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

    public String getCssPathString() {
        return cssPathString;
    }

    /**
     * This method allows callers to consume the theme's additional stylesheet. The consumer is only called if there is
     * a stylesheet that is additional to the base stylesheet, and either embedded in the application or present on
     * the file system.
     *
     * @param consumer called with the stylesheet location if there is an additional stylesheet present. The location
     *                 is local URL (e.g. {@code 'data:'} or {@code 'file:'})
     */
    public void ifAdditionalStylesheetPresent(Consumer<String> consumer) {

        final Optional<String> location;
        if (type == Theme.Type.DARK) {
            // We need to load the css file manually, due to a bug in the jdk
            // https://bugs.openjdk.java.net/browse/JDK-8240969
            // TODO: Remove this workaround, and update javadoc to include jrt: URL prefix, as soon as openjfx 16 is released
            URL url = JabRefFrame.class.getResource(cssPathString);
            location = Optional.of("data:text/css;charset=utf-8;base64," +
                    Base64.getEncoder().encodeToString(StringUtil.getResourceFileAsString(url).getBytes()));
        } else {
            location = additionalCssToLoad().map(URL::toExternalForm);
        }

        location.ifPresent(consumer);
    }
}
