package org.jabref.gui.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
 *
 * For type Custom, Theme will protect against removal of the CSS file, degrading as gracefully as possible. If the file
 * becomes unavailable while the application is running, some Scenes that have not yet had the CSS installed may not be
 * themed. The PreviewViewer, which uses WebEngine, supports data URLs and so generally are not affected by removal
 * of the file; however Theme will not attempt to URL-encode large style sheets so as to protect
 * memory usage (see {@link Theme#MAX_IN_MEMORY_CSS_LENGTH}.
 *
 * @see <a href="https://docs.jabref.org/advanced/custom-themes">Custom themes</a> in the Jabref documentation.
 */
public class Theme {
    public enum Type {
        LIGHT, DARK, CUSTOM
    }

    public static final String BASE_CSS = "Base.css";

    /**
     * A size limit above which Theme will not attempt to keep a data-embedded URL in memory for the CSS.
     *
     * It's tolerable for CSS to exceed this limit; the functional benefit of the encoded CSS is in some edge
     * case error handling. Specifically, having a reference to a data-embedded URL means that the Preview Viewer
     * isn't impacted if the source CSS file is removed while the application is running.
     *
     * If the CSS is over this limit, then the user won't see any functional impact, as long as the file exists. Only if
     * it becomes unavailable, might there be some impact. First, the Preview Viewer when created might not be themed.
     * Second, there is a very small chance of uncaught exceptions. Theme makes a best effort to avoid this:
     * it checks for CSS file existence before passing it to the Preview Viewer for theming. Still, as file existence
     * checks are immediately out of date, it can't be perfectly ruled out.
     *
     * At the time of writing this comment:
     *
     * <ul>
     * <li>src/main/java/org/jabref/gui/Base.css is 33k</li>
     * <li>src/main/java/org/jabref/gui/Dark.css is 4k</li>
     * <li>The dark custom theme in the Jabref documentation is 2k, see
     * <a href="https://docs.jabref.org/advanced/custom-themes">Custom themes</a></li>
     * </ul>
     *
     * So realistic custom themes will fit comfortably within 48k, even if they are modified copies of the base theme.
     *
     * Note that Base-64 encoding will increase the memory footprint of the URL by a third.
     */
    private static final int MAX_IN_MEMORY_CSS_LENGTH = 48000;

    private static final Logger LOGGER = LoggerFactory.getLogger(Theme.class);

    private final Type type;

    /* String, Path, and URL formats of the path to the css. These are determined at construction.
     Path and URL are only set if they are relevant and valid (i.e. no illegal characters).
     In general, use additionalCssToLoad(), to also ensure file existence checks are performed
     */
    private final String cssPathString;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<URL> cssUrl;

    private final AtomicReference<Optional<String>> cssDataUrlString = new AtomicReference<>(Optional.empty());

    private final PreferencesService preferencesService;

    public Theme(String path, PreferencesService preferencesService) {
        this.cssPathString = path;
        this.preferencesService = preferencesService;

        if (StringUtil.isBlank(path) || BASE_CSS.equalsIgnoreCase(path)) {
            // Light theme
            this.type = Type.LIGHT;
            this.cssUrl = Optional.empty();
        } else {
            URL url = JabRefFrame.class.getResource(path);
            if (url != null) {
                // Embedded dark theme
                this.type = Type.DARK;
            } else {
                // Custom theme
                this.type = Type.CUSTOM;

                try {
                    url = Path.of(path).toUri().toURL();
                } catch (InvalidPathException e) {
                    LOGGER.warn("Cannot load additional css {} because it is an invalid path: {}", path, e.getLocalizedMessage());
                    url = null;
                } catch (MalformedURLException e) {
                    LOGGER.warn("Cannot load additional css url {} because it is a malformed url: {}", path, e.getLocalizedMessage());
                    url = null;
                }
            }

            this.cssUrl = Optional.ofNullable(url);
            LOGGER.debug("Theme is {}, additional css url is {}", this.type, cssUrl.orElse(null));
        }

        additionalCssToLoad().ifPresent(this::loadCssToMemory);
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
        // Check external sources of CSS to make sure they are available:
        if (isAdditionalCssExternal()) {
            Optional<Path> cssPath = cssUrl.map(url -> Path.of(URI.create(url.toExternalForm())));
            // No need to return explicitly return Optional.empty() if Path is invalid; the URL will be empty anyway
            if (cssPath.isPresent()) {
                // When we have a valid file system path, check that the CSS file is readable
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
        }

        return cssUrl;
    }

    private boolean isAdditionalCssExternal() {
        return cssUrl.isPresent() && "file".equals(cssUrl.get().getProtocol());
    }

    /**
     * Creates a data-embedded URL from a file (or resource) URL.
     *
     * TODO: this is only desirable for file URLs, as protection against the file being removed (see
     *       {@link #MAX_IN_MEMORY_CSS_LENGTH} for details). However, there is a bug in OpenJFX, in that it does not
     *       recognise jrt URLs (modular java runtime URLs). This is detailed in
     *       <a href="https://bugs.openjdk.java.net/browse/JDK-8240969">JDK-8240969</a>.
     *       When we upgrade to OpenJFX 16, we should limit loadCssToMemory to external URLs i.e. check
     *       {@link #isAdditionalCssExternal()}. Also rename to loadExternalCssToMemory() and reword the
     *       javadoc, for clarity.
     *
     * @param url the URL of the resource to convert into a data: url
     */
    private void loadCssToMemory(URL url) {
        try {
            URLConnection conn = url.openConnection();
            conn.connect();

            try (InputStream inputStream = conn.getInputStream()) {
                byte[] data = inputStream.readNBytes(MAX_IN_MEMORY_CSS_LENGTH);
                if (data.length < MAX_IN_MEMORY_CSS_LENGTH) {
                    String embeddedDataUrl = "data:text/css;charset=utf-8;base64," +
                            Base64.getEncoder().encodeToString(data);
                    LOGGER.debug("Embedded css in data URL of length {}", embeddedDataUrl.length());
                    cssDataUrlString.set(Optional.of(embeddedDataUrl));
                } else {
                    LOGGER.debug("Not embedding css in data URL as the length is >= {}", MAX_IN_MEMORY_CSS_LENGTH);
                    cssDataUrlString.set(Optional.empty());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not load css url {} into memory", url, e);
        }
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
                    additionalCssToLoad().ifPresent(this::loadCssToMemory);
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

    /**
     * @return the Theme type
     */
    public Type getType() {
        return type;
    }

    /**
     * Provides the raw, configured custom CSS location. This should be a file system path, but the raw string is
     * returned even if it is not valid in some way. For this reason, the main use case for this getter is to
     * storing or display the user preference, rather than to read and use the CSS file.
     *
     * @return the raw configured CSS location
     */
    public String getCssPathString() {
        return cssPathString;
    }

    /**
     * This method allows callers to obtain the theme's additional stylesheet.
     *
     * @return called with the stylesheet location if there is an additional stylesheet present and available. The
     * location will be a local URL. Typically it will be a {@code 'data:'} URL where the CSS is embedded. However for
     * large themes it can be {@code 'file:'}.
     */
    public Optional<String> getAdditionalStylesheet() {
        if (cssDataUrlString.get().isEmpty()) {
            additionalCssToLoad().ifPresent(this::loadCssToMemory);
        }
        return cssDataUrlString.get().or(() -> additionalCssToLoad().map(URL::toExternalForm));
    }
}
