package org.jabref.gui.theme;

import java.net.URL;

final class StyleSheetDataUrl extends StyleSheet {

    private final URL url;

    private volatile String dataUrl;

    StyleSheetDataUrl(String name, URL url) {
        super(name);
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
        StyleSheetFile.getDataUrl(url).ifPresentOrElse(createdUrl -> dataUrl = createdUrl, () -> dataUrl = EMPTY_WEBENGINE_CSS);
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
