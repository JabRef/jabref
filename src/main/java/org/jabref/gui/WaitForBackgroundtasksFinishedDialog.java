package org.jabref.gui;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.TaskProgressView;
import org.fxmisc.easybind.EasyBind;

/**
 * Dialog shown when closing of application needs to wait for some background tasks.
 */
public class WaitForBackgroundtasksFinishedDialog {

    private final DialogService dialogService;

    public WaitForBackgroundtasksFinishedDialog(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public boolean showAndWait(StateManager stateManager) {
        TaskProgressView taskProgressView = new TaskProgressView();
        EasyBind.listBind(taskProgressView.getTasks(), stateManager.getBackgroundTasks());
        taskProgressView.setRetainTasks(false);
        taskProgressView.setGraphicFactory(BackgroundTask.iconCallback);

        Label message = new Label(Localization.lang("Waiting for background tasks to finish. Quit anyway?"));

        VBox box = new VBox(taskProgressView, message);

        DialogPane contentPane = new DialogPane();
        contentPane.setContent(box);

        FXDialog alert = new FXDialog(Alert.AlertType.NONE, Localization.lang("Please wait..."));
        alert.setDialogPane(contentPane);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setResizable(true);

        stateManager.anyTaskRunningBinding.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                alert.setResult(ButtonType.YES);
                alert.close();
            }
        });

        Dialog<ButtonType> dialog = () -> alert.showAndWait();

        Optional<ButtonType> pressedButton = dialogService.showCustomDialogAndWait(dialog);

        return pressedButton.isPresent() && pressedButton.get() == ButtonType.YES;
    }
}
