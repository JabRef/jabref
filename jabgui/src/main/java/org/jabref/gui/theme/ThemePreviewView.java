package org.jabref.gui.theme;

import java.util.List;

import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Shows the bundled screenshot of a theme in the selected color scheme; when the color scheme
/// follows the system, the dark and the light screenshot side by side.
@NullMarked
public class ThemePreviewView extends HBox {

    private static final double PREVIEW_WIDTH = 280;

    public ThemePreviewView() {
        setSpacing(8);
        managedProperty().bind(visibleProperty());
    }

    public void bind(ObservableValue<ThemePreset> theme, ObservableValue<ThemeColorScheme> colorScheme) {
        theme.addListener((_, _, newTheme) -> show(newTheme, colorScheme.getValue()));
        colorScheme.addListener((_, _, newColorScheme) -> show(theme.getValue(), newColorScheme));
        show(theme.getValue(), colorScheme.getValue());
    }

    private void show(@Nullable ThemePreset theme, @Nullable ThemeColorScheme colorScheme) {
        getChildren().clear();
        if (theme == null || colorScheme == null) {
            setVisible(false);
            return;
        }
        List<ThemeColorScheme> schemes = colorScheme == ThemeColorScheme.FOLLOW_SYSTEM
                                         ? List.of(ThemeColorScheme.DARK, ThemeColorScheme.LIGHT)
                                         : List.of(colorScheme);
        for (ThemeColorScheme scheme : schemes) {
            theme.getPreview(scheme).ifPresent(url -> {
                ImageView preview = new ImageView(new Image(url.toExternalForm(), true));
                preview.setFitWidth(PREVIEW_WIDTH);
                preview.setPreserveRatio(true);
                getChildren().add(preview);
            });
        }
        setVisible(!getChildren().isEmpty());
    }
}
