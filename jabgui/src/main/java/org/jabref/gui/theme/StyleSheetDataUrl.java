package org.jabref.gui.theme;

import java.net.URL;

public class StyleSheetDataUrl extends StyleSheet {

    private final URL url;

    StyleSheetDataUrl(URL url) {
        this.url = url;
    }

    @Override
    URL getSceneStylesheet() {
        return url;
    }

    @Override
    void reload() {
        // nothing to do
    }

    @Override
    public String toString() {
        return "StyleSheet{" + getSceneStylesheet() + "}";
    }
}
