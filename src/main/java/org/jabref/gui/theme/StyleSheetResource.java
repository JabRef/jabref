package org.jabref.gui.theme;

import java.net.URL;
import java.util.Optional;

final class StyleSheetResource extends StyleSheet {

    private final URL url;

    StyleSheetResource(URL url) {
        this.url = url;
    }

    @Override
    URL getSceneStylesheet() {
        return url;
    }

    @Override
    public Optional<String> getWebEngineStylesheet() {
        return Optional.of(url.toExternalForm());
    }
}
