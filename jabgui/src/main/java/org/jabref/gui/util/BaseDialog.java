package org.jabref.gui.util;

import java.util.Optional;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.icon.IconTheme;

public class BaseDialog<T> extends Dialog<T> {

    protected BaseDialog() {
        dialogPaneProperty().addListener((_, _, newPane) -> {
            if (newPane != null) {
                setupButtonFix(newPane);
            }
        });
        setupButtonFix(getDialogPane());

        this.setOnShowing(_ -> applyButtonFix(this.getDialogPane()));

        setDialogIcon(IconTheme.getJabRefIcon());
        setResizable(true);
    }

    private Optional<Button> getDefaultButton() {
        return Optional.ofNullable((Button) getDialogPane().lookupButton(getDefaultButtonType()));
    }

    private ButtonType getDefaultButtonType() {
        return getDialogPane().getButtonTypes().stream()
                              .filter(buttonType -> buttonType.getButtonData().isDefaultButton())
                              .findFirst()
                              .orElse(ButtonType.OK);
    }

    private void setDialogIcon(Image image) {
        Stage dialogWindow = (Stage) getDialogPane().getScene().getWindow();
        dialogWindow.getIcons().add(image);
    }

    private void setupButtonFix(DialogPane dialogPane) {
        applyButtonFix(dialogPane);
        dialogPane.getButtonTypes().addListener((ListChangeListener<ButtonType>) _ -> applyButtonFix(dialogPane));
    }

    /// Applies a fix to prevent truncating ButtonBar buttons with larger font sizes
    public static void applyButtonFix(DialogPane pane) {
        for (ButtonType type : pane.getButtonTypes()) {
            Node node = pane.lookupButton(type);
            if (node instanceof Button button) {
                // Disabling uniform size prevents the ButtonBar from squeezing
                // buttons into a width that is slightly too small for 10pt or larger text.

                ButtonBar.setButtonUniformSize(button, false);
                button.setMinWidth(Region.USE_PREF_SIZE);
                button.setMaxWidth(Double.MAX_VALUE);

                // Re-trigger CSS to ensure prefWidth is calculated using the new font metrics
                button.applyCss();
            }
        }

        // Force the window to fit the new font content bounds
        if (pane.getScene() != null && pane.getScene().getWindow() != null) {
            pane.getScene().getWindow().sizeToScene();
        }
    }

    public static void bringToFront(Dialog<?> dialog) {
        // Using answers from: <https://stackoverflow.com/a/43007782> and <https://stackoverflow.com/a/48798192>.

        Window window = dialog.getDialogPane().getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.setAlwaysOnTop(true);
            stage.setAlwaysOnTop(false);
        }
    }
}
