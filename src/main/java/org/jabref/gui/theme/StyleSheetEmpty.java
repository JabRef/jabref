package org.jabref.gui.theme;

import java.net.URL;
import java.util.Optional;

final class StyleSheetEmpty extends StyleSheet {

    static final StyleSheetEmpty EMPTY = new StyleSheetEmpty();

    StyleSheetEmpty() {
    }

    @Override
    URL getSceneStylesheet() {
        return EMPTY_CSS;
    }

    @Override
    public Optional<String> getWebEngineStylesheet() {
        return Optional.empty();
    }
}
