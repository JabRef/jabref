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
    String getSceneStylesheetLocation() {
        return dataUrl;
    }

    @Override
    void reload() {
        StyleSheetFile.getDataUrl(url).ifPresentOrElse(createdUrl -> dataUrl = createdUrl, () -> dataUrl = DATA_URL_PREFIX);
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
