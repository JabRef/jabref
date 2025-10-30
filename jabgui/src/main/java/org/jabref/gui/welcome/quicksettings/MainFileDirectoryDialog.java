package org.jabref.gui.welcome.quicksettings;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.gui.welcome.quicksettings.viewmodel.MainFileDirectoryDialogViewModel;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class MainFileDirectoryDialog extends FXDialog {
    @FXML private TextField pathField;
    @FXML private HelpButton helpButton;

    private MainFileDirectoryDialogViewModel viewModel;
    private final GuiPreferences preferences;
    private final DialogService dialogService;

    public MainFileDirectoryDialog(GuiPreferences preferences, DialogService dialogService) {
        super(Alert.AlertType.NONE, Localization.lang("Set main file directory"));

        this.preferences = preferences;
        this.dialogService = dialogService;

        setHeaderText(Localization.lang("Choose the default directory for storing attached files"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                viewModel.saveSettings();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new MainFileDirectoryDialogViewModel(preferences, dialogService);

        pathField.textProperty().bindBidirectional(viewModel.pathProperty());
        helpButton.setHelpUrl(URLs.FILE_LINKS_DOC);
    }

    @FXML
    private void browseDirectory() {
        viewModel.browseForDirectory();
    }
}
