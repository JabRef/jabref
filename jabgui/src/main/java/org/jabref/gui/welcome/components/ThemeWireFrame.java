package org.jabref.gui.welcome.components;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import org.jabref.gui.theme.ThemeTypes;

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

    public void setThemeType(@NonNull ThemeTypes themeType) {
        getStyleClass().removeIf(styleClass ->
                styleClass.startsWith("wireframe-light") ||
                        styleClass.startsWith("wireframe-dark") ||
                        styleClass.startsWith("wireframe-custom"));

        String themeClassName = switch (themeType) {
            case LIGHT ->
                    "wireframe-light";
            case DARK ->
                    "wireframe-dark";
            case CUSTOM ->
                    "wireframe-custom";
        };
        getStyleClass().add(themeClassName);
    }
}
