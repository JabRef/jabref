package org.jabref.gui.util;

import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;

import com.airhacks.afterburner.injection.Injector;

public class BaseDialog<T> extends Dialog<T> {

    protected BaseDialog() {
        dialogPaneProperty().addListener((_, _, newPane) -> {
            if (newPane != null) {
                setupKeyBindings(newPane);
            }
        });
        setupKeyBindings(getDialogPane());

        setDialogIcon(IconTheme.getJabRefIcon());

        Stage dialogWindow = getDialogWindow();
        dialogWindow.addEventHandler(WindowEvent.WINDOW_SHOWN, _ -> fitWindowToContent(this.getDialogPane()));

        setResizable(true);
    }

    public static boolean closeOnKeyBindingMatch(KeyEvent event, Dialog<?> dialog) {
        KeyBindingRepository keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);
        if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLOSE, event)) {
            dialog.close();
            event.consume();

            return true;
        }

        return false;
    }

    private Stage getDialogWindow() {
        return (Stage) getDialogPane().getScene().getWindow();
    }

    private void setupKeyBindings(DialogPane dialogPane) {
        dialogPane.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    }

    private void handleKeyEvent(KeyEvent event) {
        boolean closed = closeOnKeyBindingMatch(event, this);
        if (closed) {
            return;
        }

        KeyBindingRepository keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);
        if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.DEFAULT_DIALOG_ACTION, event)) {
            getDefaultButton().ifPresent(Button::fire);
            event.consume();
        }

        // all buttons in base dialogs react on enter
        if (event.getCode() == KeyCode.ENTER) {
            if (event.getTarget() instanceof Button button) {
                button.fire();
                event.consume();
            }
        }
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

    /// Fits the dialog window around its content.
    public static void fitWindowToContent(DialogPane pane) {
        for (ButtonType type : pane.getButtonTypes()) {
            if (pane.lookupButton(type) instanceof Button button) {
                // The button bar squeezes buttons into a width that is too small for a raised font size.
                button.setPrefWidth(Region.USE_COMPUTED_SIZE);
            }
        }

        // The new sizes are only known after the pane has been laid out again.
        Platform.runLater(() -> sizeDialog(pane));
    }

    /// Reapplies CSS and relayout the dialog again.
    private static void sizeDialog(DialogPane pane) {
        Scene scene = pane.getScene();
        if (scene == null || !(scene.getWindow() instanceof Stage stage)) {
            return;
        }
        clearSizeCache(pane);

        pane.applyCss();
        pane.layout();

        // Now that the sizes are the ones of the font the dialog is rendered in, the window can be
        // fitted around its content again.
        stage.sizeToScene();
    }

    private static void clearSizeCache(Parent parent) {
        parent.requestLayout();
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent childParent) {
                clearSizeCache(childParent);
            }
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
