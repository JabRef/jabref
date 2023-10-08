package org.jabref.gui.preferences.git;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class GitTab extends AbstractPreferenceTabView<GitTabViewModel> {

    @FXML private TextField username;
    @FXML private PasswordField password;

    public GitTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Git");
    }

    @FXML
    private void initialize() {
        viewModel = new GitTabViewModel(preferencesService.getGitPreferences());

        username.textProperty().bindBidirectional(viewModel.getUsernameProperty());
        password.textProperty().bindBidirectional(viewModel.getPasswordProperty());
    }
}
