package org.jabref.gui.theme;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.JabRefFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class StyleSheet {

    static final URL EMPTY_CSS = JabRefFrame.class.getResource("Empty.css");

    static final Logger LOGGER = LoggerFactory.getLogger(StyleSheet.class);

    abstract URL getSceneStylesheet();

    abstract Optional<String> getWebEngineStylesheet();

    Path getWatchPath() {
        return null;
    }

    static StyleSheet create(String name) {
        URL url = JabRefFrame.class.getResource(name);
        if (url == null) {
            try {
                url = Path.of(name).toUri().toURL();
            } catch (InvalidPathException e) {
                LOGGER.warn("Cannot load additional css {} because it is an invalid path: {}", name, e.getLocalizedMessage());
                url = null;
            } catch (MalformedURLException e) {
                LOGGER.warn("Cannot load additional css url {} because it is a malformed url: {}", name, e.getLocalizedMessage());
                url = null;
            }
        }
        if (url == null) {
            return StyleSheetEmpty.EMPTY;
        } else if ("file".equals(url.getProtocol())) {
            return new StyleSheetFile(url);
        } else {
            /*
            TODO: embedding CSS in a data URL is only desirable in file URLs, as protection against the file being
             removed. This is built into StyleSheetFile (see StyleSheetFile.MAX_IN_MEMORY_CSS_LENGTH for details on
             caching). However, there is a bug in OpenJFX, in that WebEngine does not recognise jrt URLs (modular java
             runtime URLs). This is detailed in https://bugs.openjdk.java.net/browse/JDK-8240969.
             When we upgrade to OpenJFX 16, we no longer need to wrap built in themes as data URLs. Note that we
             already do not wrap the base stylesheet, because it is not used to style the WebEngine. WebEngine is
             used for the Preview Viewer and this does not use the base CSS.
             */
            if ("Base.css".equals(name)) {
                return new StyleSheetResource(url);
            } else {
                return new StyleSheetDataUrl(url);
            }
        }
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
