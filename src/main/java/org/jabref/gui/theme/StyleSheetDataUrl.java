package org.jabref.gui.theme;

import java.net.URL;

public class StyleSheetDataUrl extends StyleSheet {

    private final URL url;

    private volatile String dataUrl;

    StyleSheetDataUrl(URL url) {
        this.url = url;
        reload();
    }

    @Override
    URL getSceneStylesheet() {
        return url;
    }

    @Override
    public String getWebEngineStylesheet() {
        return dataUrl;
    }

    @Override
    void reload() {
        StyleSheetFile.embedDataUrl(url, embedded -> dataUrl = embedded.orElse(EMPTY_WEBENGINE_CSS));
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
