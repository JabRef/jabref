package org.jabref.gui.theme;

import java.net.URL;
import java.util.Optional;

final class StyleSheetDataUrl extends StyleSheet {

    private final URL url;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<String> dataUrl;

    StyleSheetDataUrl(URL url) {
        this.url = url;
        StyleSheetFile.embedDataUrl(url, embedded -> dataUrl = embedded);
    }

    @Override
    URL getSceneStylesheet() {
        return url;
    }

    @Override
    public Optional<String> getWebEngineStylesheet() {
        return dataUrl;
    }
}
