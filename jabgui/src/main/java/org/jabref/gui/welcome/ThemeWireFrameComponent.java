package org.jabref.gui.welcome;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import com.airhacks.afterburner.views.ViewLoader;

public class ThemeWireFrameComponent extends VBox {

    private final StringProperty themeType = new SimpleStringProperty();

    public ThemeWireFrameComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        themeType.addListener((_, _, newValue) -> {
            if (newValue != null) {
                updateTheme();
            }
        });
    }

    public ThemeWireFrameComponent(String themeType) {
        this();
        setThemeType(themeType);
    }

    public void setThemeType(String themeType) {
        this.themeType.set(themeType);
    }

    @FXML
    private void initialize() {
        if (themeType.get() != null) {
            updateTheme();
        }
    }

    private void updateTheme() {
        String theme = themeType.get();
        if (theme == null) {
            return;
        }

        getStyleClass().removeIf(styleClass ->
                styleClass.startsWith("wireframe-light") ||
                        styleClass.startsWith("wireframe-dark") ||
                        styleClass.startsWith("wireframe-custom"));

        getStyleClass().add("wireframe-" + theme);
    }
}
