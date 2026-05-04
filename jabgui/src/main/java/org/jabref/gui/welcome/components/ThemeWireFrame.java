package org.jabref.gui.welcome.components;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import org.jabref.gui.theme.ThemeColorScheme;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NonNull;

public class ThemeWireFrame extends VBox {
    public ThemeWireFrame() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
    }

    public void setThemeColorScheme(@NonNull ThemeColorScheme themeColorScheme) {
        getStyleClass().removeIf(styleClass ->
                styleClass.startsWith("wireframe-light") ||
                        styleClass.startsWith("wireframe-dark") ||
                        styleClass.startsWith("wireframe-custom"));

        String themeClassName = switch (themeColorScheme) {
            case LIGHT ->
                    "wireframe-light";
            case DARK ->
                    "wireframe-dark";
            case FOLLOW_SYSTEM ->
                    "wireframe-custom";
        };
        getStyleClass().add(themeClassName);
    }
}
