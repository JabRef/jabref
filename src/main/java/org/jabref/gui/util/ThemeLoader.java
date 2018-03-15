package org.jabref.gui.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javafx.scene.Parent;
import javafx.scene.Scene;

import org.jabref.gui.AbstractView;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThemeLoader {

    private static final String pathDefaultMainCss = AbstractView.class.getResource("Main.css").toExternalForm();
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
        installCss(scene, pathDefaultMainCss);
    }

    private void installCss(Scene scene, String cssUrl) {
        scene.getStylesheets().add(0, cssUrl);
        try {
            // If the resources are part of a .jar bundle, then we don't want to watch it for changes
            if (!cssUrl.startsWith("jar:")) {
                Path cssFile = Paths.get(new URL(cssUrl).toURI());
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
        control.getStylesheets().add(0, pathDefaultMainCss);
    }
}
