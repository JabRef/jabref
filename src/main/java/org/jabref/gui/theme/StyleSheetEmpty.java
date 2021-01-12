package org.jabref.gui.theme;

import java.net.URL;

final class StyleSheetEmpty extends StyleSheet {

    static final StyleSheetEmpty EMPTY = new StyleSheetEmpty();

    StyleSheetEmpty() {
    }

    @Override
    URL getSceneStylesheet() {
        return EMPTY_SCENE_CSS;
    }

    @Override
    public String getWebEngineStylesheet() {
        return EMPTY_WEBENGINE_CSS;
    }

    @Override
    void reload() {
        // nothing to do
    }
}
