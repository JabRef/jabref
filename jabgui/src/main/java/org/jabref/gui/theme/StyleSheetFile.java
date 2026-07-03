package org.jabref.gui.theme;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.util.URLUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StyleSheetFile extends StyleSheet {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleSheetFile.class);

    private final URL url;
    private final Path path;

    StyleSheetFile(URL url) {
        this.url = url;
        this.path = Path.of(URLUtil.createUri(url.toExternalForm()));
    }

    @Override
    Path getWatchPath() {
        return path;
    }

    @Override
    void reload() {
        // Scenes reference the stylesheet by its file URL; nothing is cached here
    }

    @Override
    public URL getSceneStylesheet() {
        if (!Files.exists(path)) {
            LOGGER.warn("Cannot load additional css {} because the file does not exist.", path);
            return null;
        }

        if (Files.isDirectory(path)) {
            LOGGER.warn("Failed to loadCannot load additional css {} because it is a directory.", path);
            return null;
        }

        return url;
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
