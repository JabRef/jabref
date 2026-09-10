package org.jabref.gui.theme;

import java.util.List;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Shows the bundled screenshot of a theme in the selected color scheme; when the color scheme
/// follows the system, the dark and the light screenshot side by side.
///
/// Each screenshot sits in a frame of a fixed size, so that the surrounding form is laid out once
/// and does not jump when an image -- loaded in the background -- arrives or is switched.
@NullMarked
public class ThemePreviewView extends HBox {

    private static final double PREVIEW_WIDTH = 256;

    /// The screenshots are roughly 16:10; a box slightly taller than that leaves a little air
    /// around the ones that differ by a few pixels instead of cropping them.
    private static final double PREVIEW_HEIGHT = 160;

    private static final double FRAME_PADDING = 4;

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
                ImageView preview = new ImageView(new Image(url.toExternalForm(), PREVIEW_WIDTH, PREVIEW_HEIGHT, true, true, true));
                preview.setFitWidth(PREVIEW_WIDTH);
                preview.setFitHeight(PREVIEW_HEIGHT);
                preview.setPreserveRatio(true);
                getChildren().add(frame(preview));
            });
        }
        setVisible(!getChildren().isEmpty());
    }

    private static StackPane frame(ImageView preview) {
        double width = PREVIEW_WIDTH + 2 * FRAME_PADDING;
        double height = PREVIEW_HEIGHT + 2 * FRAME_PADDING;
        StackPane frame = new StackPane(preview);
        frame.setPadding(new Insets(FRAME_PADDING));
        frame.setMinSize(width, height);
        frame.setPrefSize(width, height);
        frame.setMaxSize(width, height);
        frame.getStyleClass().add("bordered");
        return frame;
    }
}
