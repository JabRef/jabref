package org.jabref.gui;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;

/**
 * This class provides a super class for all dialogs implemented in JavaFX.
 * It mimics the behavior of a Swing JDialog which means once a object of this class
 * is shown all Swing windows will be blocked and stay in the background. Since this
 * class extends from a JavaFX {@link Alert} it behaves as a normal dialog towards all
 * windows in the JavaFX thread.
 * <p>
 * To create a custom JavaFX dialog one should create an instance of this class and set a dialog
 * pane through the inherited {@link Dialog#setDialogPane(DialogPane)} method.
 * The dialog can be shown via {@link Dialog#show()} or {@link Dialog#showAndWait()}.
 *
 * The layout of the pane should be defined in an external fxml file and loaded it via the
 * {@link FXMLLoader}.
 */
public class FXDialog extends Alert {

    /**
     * The WindowAdapter will be added to all Swing windows once an instance
     * of this class is shown and redirects the focus towards this instance.
     * The WindowAdapter will be removed once the instance of this class gets hidden.
     *
     */
    private final WindowAdapter fxOverSwingHelper = new WindowAdapter() {

        @Override
        public void windowActivated(WindowEvent e) {
            Platform.runLater(() -> {
                Stage fxDialogWindow = getDialogWindow();
                fxDialogWindow.toFront();
                fxDialogWindow.requestFocus();
            });
        }

        @Override
        public void windowGainedFocus(WindowEvent e) {
            Platform.runLater(() -> {
                Stage fxDialogWindow = getDialogWindow();
                fxDialogWindow.toFront();
                fxDialogWindow.requestFocus();
            });
        }
    };

    public FXDialog(AlertType type, String title, Image image, boolean isModal) {
        this(type, title, isModal);
        setDialogIcon(image);
    }

    public FXDialog(AlertType type, String title, Image image) {
        this(type, title, true);
        setDialogIcon(image);
    }

    public FXDialog(AlertType type, String title, boolean isModal) {
        this(type, isModal);
        setTitle(title);
    }

    public FXDialog(AlertType type, String title) {
        this(type);
        setTitle(title);
    }

    public FXDialog(AlertType type, boolean isModal) {
        super(type);

        setDialogIcon(IconTheme.getJabRefImageFX());

        Stage dialogWindow = getDialogWindow();
        dialogWindow.setOnCloseRequest(evt -> this.close());
        if (isModal) {
            initModality(Modality.APPLICATION_MODAL);
        } else {
            initModality(Modality.NONE);
        }
        dialogWindow.setOnShown(evt -> {
            setSwingWindowsEnabledAndFocusable(!isModal);
            setLocationRelativeToMainWindow();
        });
        dialogWindow.setOnHiding(evt -> setSwingWindowsEnabledAndFocusable(true));

        dialogWindow.setOnCloseRequest(evt -> this.close());

        dialogWindow.getScene().setOnKeyPressed(event -> {
            KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
            if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLOSE_DIALOG, event)) {
                dialogWindow.close();
            }
        });
    }

    public FXDialog(AlertType type) {
        this(type, true);
    }

    public void setDialogIcon(Image image) {
        Stage fxDialogWindow = getDialogWindow();
        fxDialogWindow.getIcons().add(image);
    }

    private Stage getDialogWindow() {
        return (Stage) getDialogPane().getScene().getWindow();
    }

    private void setSwingWindowsEnabledAndFocusable(boolean enabled) {
        for (Window swingWindow : Window.getWindows()) {
            swingWindow.setEnabled(enabled);
            if (!enabled) {
                swingWindow.addWindowListener(fxOverSwingHelper);
            } else {
                swingWindow.removeWindowListener(fxOverSwingHelper);
            }
        }
    }

    private void setLocationRelativeToMainWindow() {
        double mainWindowX = JabRefGUI.getMainFrame().getLocationOnScreen().getX();
        double mainWindowY = JabRefGUI.getMainFrame().getLocationOnScreen().getY();
        double mainWindowWidth = JabRefGUI.getMainFrame().getSize().getWidth();
        double mainWindowHeight = JabRefGUI.getMainFrame().getSize().getHeight();

        setX((mainWindowX + (mainWindowWidth / 2)) - (getWidth() / 2));
        setY((mainWindowY + (mainWindowHeight / 2)) - (getHeight() / 2));
    }

}
