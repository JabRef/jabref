package org.jabref.gui.theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StyleSheetFile extends StyleSheet {

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
    static final int MAX_IN_MEMORY_CSS_LENGTH = 48000;

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleSheetFile.class);

    private final URL url;
    private final Path path;

    private final AtomicReference<String> dataUrl = new AtomicReference<>();

    StyleSheetFile(URL url) {
        this.url = url;
        this.path = Path.of(URI.create(url.toExternalForm()));
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

    /**
     * This method allows callers to obtain the theme's additional stylesheet.
     *
     * @return the stylesheet location if there is an additional stylesheet present and available. The
     * location will be a local URL. Typically it will be a {@code 'data:'} URL where the CSS is embedded. However for
     * large themes it can be {@code 'file:'}.
     */
    @Override
    public String getWebEngineStylesheet() {
        if (Strings.isNullOrEmpty(dataUrl.get())) {
            reload();
        }

        if (Strings.isNullOrEmpty(dataUrl.get())) {
            URL stylesheet = getSceneStylesheet();
            return stylesheet == null ? "" : stylesheet.toExternalForm();
        }

        return dataUrl.get();
    }

    static Optional<String> getDataUrl(URL url) {
        try {
            URLConnection conn = url.openConnection();
            conn.connect();

            try (InputStream inputStream = conn.getInputStream()) {
                byte[] data = inputStream.readNBytes(MAX_IN_MEMORY_CSS_LENGTH);
                if (data.length < MAX_IN_MEMORY_CSS_LENGTH) {
                    String embeddedDataUrl = DATA_URL_PREFIX + Base64.getEncoder().encodeToString(data);
                    LOGGER.debug("Embedded css in data URL of length {}", embeddedDataUrl.length());
                    return Optional.of(embeddedDataUrl);
                } else {
                    LOGGER.debug("Not embedding css in data URL as the length is >= {}", MAX_IN_MEMORY_CSS_LENGTH);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not load css url {}", url, e);
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
