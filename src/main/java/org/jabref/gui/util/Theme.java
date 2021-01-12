package org.jabref.gui.util;

import java.util.Optional;

import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import org.jabref.gui.theme.ThemePreference;

public interface Theme {

    enum Type {
        LIGHT, DARK, CUSTOM
    }

    String BASE_CSS = "Base.css";

    /**
     * Installs the base css file as a stylesheet in the given scene. Changes in the css file lead to a redraw of the
     * scene using the new css file.
     *
     * @param scene the scene to install the css into
     */
    void installCss(Scene scene);

    /**
     * Installs the css file as a stylesheet in the given web engine. Changes in the css file lead to a redraw of the
     * web engine using the new css file.
     *
     * @param webEngine the web engine to install the css into
     */
    void installCss(WebEngine webEngine);

    void updateThemePreference(ThemePreference themePreference);

    ThemePreference getPreference();

    /**
     * This method allows callers to obtain the theme's additional stylesheet.
     *
     * @return called with the stylesheet location if there is an additional stylesheet present and available. The
     * location will be a local URL. Typically it will be a {@code 'data:'} URL where the CSS is embedded. However for
     * large themes it can be {@code 'file:'}.
     */
    Optional<String> getAdditionalStylesheet();
}
