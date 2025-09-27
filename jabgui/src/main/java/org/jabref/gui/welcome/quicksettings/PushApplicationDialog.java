package org.jabref.gui.welcome.quicksettings;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.push.GuiPushToApplication;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.gui.welcome.components.PushToApplicationCell;
import org.jabref.gui.welcome.quicksettings.viewmodel.PushApplicationDialogViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.push.PushToApplicationDetector;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class PushApplicationDialog extends FXDialog {
    @FXML private ListView<GuiPushToApplication> applicationsList;
    @FXML private TextField pathField;
    @FXML private HelpButton helpButton;

    private PushApplicationDialogViewModel viewModel;
    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public PushApplicationDialog(GuiPreferences preferences, DialogService dialogService, TaskExecutor taskExecutor) {
        super(AlertType.NONE, Localization.lang("Configure push to applications"), true);

        this.preferences = preferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.setHeaderText(Localization.lang("Select your text editor or LaTeX application for pushing citations"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK && viewModel.isValidConfiguration()) {
                viewModel.saveSettings();
            } else if (button == ButtonType.CANCEL) {
                viewModel.cancelDetection();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new PushApplicationDialogViewModel(preferences, dialogService, taskExecutor);

        applicationsList.itemsProperty().bind(viewModel.applicationsProperty());
        applicationsList.setCellFactory(_ -> new PushToApplicationCell(viewModel.detectedApplications()));
        EasyBind.subscribe(applicationsList.getSelectionModel().selectedItemProperty(), viewModel::setSelectedApplication);

        pathField.textProperty().bindBidirectional(viewModel.pathProperty());
        pathField.textProperty().addListener((_, _, newText) -> updatePathValidation(newText));
        helpButton.setHelpUrl(URLs.PUSH_TO_APPLICATIONS_DOC);
    }

    private void updatePathValidation(String newText) {
        if (PushToApplicationDetector.isValidAbsolutePath(newText)) {
            pathField.getStyleClass().removeAll("invalid-path");
        } else {
            if (!pathField.getStyleClass().contains("invalid-path")) {
                pathField.getStyleClass().add("invalid-path");
            }
        }
    }

    @FXML
    private void browseApplication() {
        viewModel.browseForApplication();
    }
}
