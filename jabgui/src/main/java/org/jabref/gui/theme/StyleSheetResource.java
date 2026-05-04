package org.jabref.gui.theme;

import java.net.URL;

final class StyleSheetResource extends StyleSheet {

    private final URL url;

    StyleSheetResource(String name, URL url) {
        super(name);
        this.url = url;
    }

    @Override
    URL getSceneStylesheet() {
        return url;
    }

    @Override
    public String getWebEngineStylesheet() {
        return url.toExternalForm();
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
