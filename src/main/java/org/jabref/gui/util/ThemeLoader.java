package org.jabref.gui.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.Scene;

import org.jabref.gui.JabRefFrame;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs the style file and provides live reloading.
 * <p>
 * The live reloading has to be turned on by setting the <code>-Djabref.theme.css</code> property.
 * There two possible modes:
 * (1) When only <code>-Djabref.theme.css</code> is specified, then the standard <code>Base.css</code> that is found will be watched
 * and on changes in that file, the style-sheet will be reloaded and changes are immediately visible.
 * (2) When a path to a css file is passed to <code>-Djabref.theme.css</code>, then the given style is loaded in addition to the base css file.
 * Changes in the specified css file lead to an immediate redraw of the interface.
 * <p>
 * When working from an IDE, this usually means that the <code>Base.css</code> is located in the build folder.
 * To use the css-file that is located in the sources directly, the full path can be given as value for the "VM option":
 * <code>-Djabref.theme.css="/path/to/src/Base.css"</code>
 */
public class ThemeLoader {

    public static final String MAIN_CSS = "Base.css";
    public static final String DARK_CSS = "Dark.css";

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeLoader.class);
    private final Optional<URL> additionalCssToLoad;
    private final FileUpdateMonitor fileUpdateMonitor;

    public ThemeLoader(FileUpdateMonitor fileUpdateMonitor, JabRefPreferences jabRefPreferences) {
        this.fileUpdateMonitor = Objects.requireNonNull(fileUpdateMonitor);

        String cssVmArgument = System.getProperty("jabref.theme.css");
        String cssPreferences = jabRefPreferences.get(JabRefPreferences.FX_THEME);
        if (StringUtil.isNotBlank(cssVmArgument)) {
            // First priority: VM argument
            LOGGER.info("Using css from VM option: " + cssVmArgument);
            URL cssVmUrl = null;
            try {
                cssVmUrl = Paths.get(cssVmArgument).toUri().toURL();
            } catch (MalformedURLException e) {
                LOGGER.warn("Cannot load css " + cssVmArgument, e);
            }
            additionalCssToLoad = Optional.ofNullable(cssVmUrl);
        } else if (StringUtil.isNotBlank(cssPreferences) && !MAIN_CSS.equalsIgnoreCase(cssPreferences)) {
            // Otherwise load css from preference
            URL cssResource = JabRefFrame.class.getResource(cssPreferences);
            if (cssResource != null) {
                LOGGER.debug("Using css " + cssResource);
                additionalCssToLoad = Optional.of(cssResource);
            } else {
                additionalCssToLoad = Optional.empty();
                LOGGER.warn("Cannot load css " + cssPreferences);
            }
        } else {
            additionalCssToLoad = Optional.empty();
        }
    }

    /**
     * Installs the base css file as a stylesheet in the given scene. Changes in the css file lead to a redraw of the
     * scene using the new css file.
     */
    public void installCss(Scene scene, JabRefPreferences preferences) {
        addAndWatchForChanges(scene, JabRefFrame.class.getResource(MAIN_CSS), 0);
        additionalCssToLoad.ifPresent(file -> addAndWatchForChanges(scene, file, 1));

        preferences.getFontSize().ifPresent(size -> scene.getRoot().setStyle("-fx-font-size: " + size + "pt;"));
    }

    private void addAndWatchForChanges(Scene scene, URL cssFile, int index) {
        scene.getStylesheets().add(index, cssFile.toExternalForm());

        try {
            // If the file is an ordinary file (i.e. not part of a java runtime bundle), we watch it for changes and turn on live reloading
            URI cssUri = cssFile.toURI();
            if (!cssUri.toString().contains("jrt")) {
                LOGGER.debug("CSS URI {}", cssUri);

                Path cssPath = Paths.get(cssUri).toAbsolutePath();
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
            LOGGER.error("Could not watch css file for changes " + cssFile, e);
        }
    }
}
