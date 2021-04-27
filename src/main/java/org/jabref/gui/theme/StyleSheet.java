package org.jabref.gui.theme;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.JabRefFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class StyleSheet {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleSheet.class);

    abstract URL getSceneStylesheet();

    abstract String getWebEngineStylesheet();

    Path getWatchPath() {
        return null;
    }

    abstract void reload();

    static Optional<StyleSheet> create(String name) {
        Optional<URL> styleSheetUrl = Optional.ofNullable(JabRefFrame.class.getResource(name));

        if (styleSheetUrl.isEmpty()) {
            try {
                styleSheetUrl = Optional.of(Path.of(name).toUri().toURL());
            } catch (InvalidPathException e) {
                LOGGER.warn("Cannot load additional css {} because it is an invalid path: {}", name, e.getLocalizedMessage());
                return Optional.empty();
            } catch (MalformedURLException e) {
                LOGGER.warn("Cannot load additional css url {} because it is a malformed url: {}", name, e.getLocalizedMessage());
                return Optional.empty();
            }
        }

        try {
            Path path = Path.of(styleSheetUrl.get().toURI());
            if (Files.isDirectory(path)) {
                LOGGER.warn("Cannot load additional css {} because it is a directory.", name);
                return Optional.empty();
            } else if (!Files.exists(path)) {
                LOGGER.warn("Cannot load additional css {} because the file does not exist.", name);
                return Optional.empty();
            }
        } catch (URISyntaxException ignored) {
            // JVM is reformatting a url its validity already is checked above
        }

        if ("file".equals(styleSheetUrl.get().getProtocol())) {
            return Optional.of(new StyleSheetFile(styleSheetUrl.get()));
        } else {
            return Optional.of(new StyleSheetResource(styleSheetUrl.get()));
        }
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
