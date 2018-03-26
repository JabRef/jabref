package org.jabref.gui.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javafx.scene.Parent;
import javafx.scene.Scene;

import org.jabref.gui.JabRefFrame;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs the Main.css style file and provides live reloading. The live reloading has to be turned on by setting
 * the <code>-Djabref.main.css</code> property. There two possible modes: (1) When only <code>-Djabref.main.css</code>
 * is specified, then the standard <code>Main.css</code> that is found will be watched and on changes in that file,
 * the style-sheet will be reloaded and changes are immediately visible.
 * <p>
 * When working from an IDE, this usually means that the <code>Main.css</code> is located in the build folder. To use
 * the css-file that is located in the sources directly, the full path can be given as value:
 * <p>
 * <code>-Djabref.main.css="/path/to/src/Main.css"</code>
 */
public class ThemeLoader {

    private static final String DEFAULT_PATH_MAIN_CSS = JabRefFrame.class.getResource("Main.css").toExternalForm();
    private static final String CSS_SYSTEM_PROPERTY = System.getProperty("jabref.main.css");
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeLoader.class);
    private final FileUpdateMonitor fileUpdateMonitor;

    public ThemeLoader(FileUpdateMonitor fileUpdateMonitor) {
        this.fileUpdateMonitor = Objects.requireNonNull(fileUpdateMonitor);
    }

    /**
     * Installs the base css file as a stylesheet in the given scene.
     * Changes in the css file lead to a redraw of the scene using the new css file.
     */
    public void installBaseCss(Scene scene) {
        String pathToMainCss = DEFAULT_PATH_MAIN_CSS;
        if (CSS_SYSTEM_PROPERTY != null) {
            final Path path = Paths.get(CSS_SYSTEM_PROPERTY);
            if ("Main.css".matches(path.getFileName().toString()) && Files.isReadable(path)) {
                pathToMainCss = path.toUri().toString();
            }
        }
        installCss(scene, pathToMainCss);
    }

    private void installCss(Scene scene, String cssUrl) {
        scene.getStylesheets().add(0, cssUrl);
        try {
            // If -Djabref.main.css is defined and the resources are not part of a .jar bundle,
            // we watch the file for changes and turn on live reloading
            if (!cssUrl.startsWith("jar:") && CSS_SYSTEM_PROPERTY != null) {
                Path cssFile = Paths.get(new URL(cssUrl).toURI());
                LOGGER.info("Enabling live reloading of " + cssFile);
                fileUpdateMonitor.addListenerForFile(cssFile, () -> {
                    LOGGER.info("Reload css file " + cssFile);

                    DefaultTaskExecutor.runInJavaFXThread(() -> {
                                scene.getStylesheets().remove(cssUrl);
                                scene.getStylesheets().add(0, cssUrl);
                            }
                    );
                });
            }
        } catch (URISyntaxException | IOException e) {
            LOGGER.error("Could not watch css file for changes " + cssUrl, e);
        }
    }

    /**
     * @deprecated you should never need to add css to a control, add it to the scene containing the control
     */
    @Deprecated
    public void installBaseCss(Parent control) {
        control.getStylesheets().add(0, DEFAULT_PATH_MAIN_CSS);
    }
}
