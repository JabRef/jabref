package org.jabref.gui.theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.util.URLUtil;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StyleSheetFile extends StyleSheet {

    /// A size limit above which Theme will not attempt to keep a data-embedded URL in memory for the CSS.
    ///
    /// Embedding the CSS in a `data:` URL has two functional benefits: the theme survives removal
    /// of the CSS file while the application is running, and — since the URL changes with the
    /// content — JavaFX's stylesheet cache cannot serve stale content after a live reload.
    ///
    /// If the CSS is over this limit, the plain file URL is used instead: nothing breaks as long
    /// as the file exists. Realistic custom themes fit comfortably within 48k (the dark custom
    /// theme in the JabRef documentation is 2k; jabref-theme.css is 33k). Note that Base-64
    /// encoding increases the memory footprint of the URL by a third.
    static final int MAX_IN_MEMORY_CSS_LENGTH = 48000;

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleSheetFile.class);

    private final URL url;
    private final Path path;

    private final AtomicReference<String> dataUrl = new AtomicReference<>();

    StyleSheetFile(String name, URL url) {
        super(name);
        this.url = url;
        this.path = Path.of(URLUtil.createUri(url.toExternalForm()));
        reload();
    }

    @Override
    Path getWatchPath() {
        return path;
    }

    @Override
    void reload() {
        getDataUrl(url).ifPresentOrElse(dataUrl::set, () -> dataUrl.set(""));
    }

    @Override
    public URL getSceneStylesheet() {
        return url;
    }

    /// @return the stylesheet location if the stylesheet is present and available. The location will be a local
    /// URL. Typically, it will be a `'data:'` URL where the CSS is embedded. However, for large themes it can be
    /// `'file:'`.
    @Override
    String getSceneStylesheetLocation() {
        if (Strings.isNullOrEmpty(dataUrl.get())) {
            reload();
        }
        if (Strings.isNullOrEmpty(dataUrl.get())) {
            return getStylesheetURL().map(URL::toExternalForm).orElse("");
        }
        return dataUrl.get();
    }

    static Optional<String> getDataUrl(URL url) {
        try {
            URLConnection connection = url.openConnection();
            connection.connect();

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] data = inputStream.readNBytes(MAX_IN_MEMORY_CSS_LENGTH);
                if (data.length < MAX_IN_MEMORY_CSS_LENGTH) {
                    String embeddedDataUrl = DATA_URL_PREFIX + Base64.getEncoder().encodeToString(data);
                    LOGGER.trace("Embedded css in data URL of length {}", embeddedDataUrl.length());
                    return Optional.of(embeddedDataUrl);
                } else {
                    LOGGER.trace("Not embedding css in data URL as the length is >= {}", MAX_IN_MEMORY_CSS_LENGTH);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not load css url {}", url, e);
        }

        return Optional.empty();
    }

    private Optional<URL> getStylesheetURL() {
        if (!Files.exists(path)) {
            LOGGER.warn("Cannot load custom css {} because the file does not exist.", path);
            return Optional.empty();
        }

        if (Files.isDirectory(path)) {
            LOGGER.warn("Cannot load custom css {} because it is a directory.", path);
            return Optional.empty();
        }

        return Optional.of(url);
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
