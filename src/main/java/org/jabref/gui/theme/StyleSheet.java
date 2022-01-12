package org.jabref.gui.theme;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.JabRefFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class StyleSheet {

    static final String DATA_URL_PREFIX = "data:text/css;charset=utf-8;base64,";
    static final String EMPTY_WEBENGINE_CSS = DATA_URL_PREFIX;

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
            } catch (MalformedURLException e) {
                LOGGER.warn("Cannot load additional css url {} because it is a malformed url: {}", name, e.getLocalizedMessage());
            }
        }

        if (styleSheetUrl.isEmpty()) {
            try {
                return Optional.of(new StyleSheetDataUrl(new URL(EMPTY_WEBENGINE_CSS)));
            } catch (MalformedURLException e) {
                return Optional.empty();
            }
        } else if ("file".equals(styleSheetUrl.get().getProtocol())) {
            StyleSheet styleSheet = new StyleSheetFile(styleSheetUrl.get());

            if (Files.isDirectory(styleSheet.getWatchPath())) {
                LOGGER.warn("Failed to loadCannot load additional css {} because it is a directory.", styleSheet.getWatchPath());
                return Optional.empty();
            }

            if (!Files.exists(styleSheet.getWatchPath())) {
                LOGGER.warn("Cannot load additional css {} because the file does not exist.", styleSheet.getWatchPath());
                // Should not return empty, since the user can create the file later.
            }

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
