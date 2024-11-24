package org.jabref.gui.preferences.git;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.git.GitPreferences;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class AuthentifyDialogView extends BaseDialog<GitViewModel> {

    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private ButtonType save;
    private AuthentifyDialogViewModel viewModel;

    private final DialogService dialogService;
    private final GitPreferences preferences;

    public AuthentifyDialogView(GitPreferences gitPreferences, DialogService dialogservice) {
        this.dialogService = dialogservice;
        this.preferences = gitPreferences;

        this.setTitle(Localization.lang("Git Authentication"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setResultConverter(button -> {
            if (button == save) {
                return viewModel.saveCredentials();
            } else {
                return null;
            }
        });
    }

    @FXML
    private void initialize() {
        viewModel = new AuthentifyDialogViewModel(dialogService, preferences);
        username.textProperty().bindBidirectional(viewModel.getUsername());
        password.textProperty().bindBidirectional(viewModel.getPassword());
    }
}
