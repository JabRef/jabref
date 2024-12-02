package org.jabref.gui.preferences.git;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class GitTab extends AbstractPreferenceTabView<GitViewModel> implements PreferencesTab {
    @FXML private CheckBox enableGitSupport;
    @FXML private CheckBox hostKeyCheck;
    @FXML private CheckBox pushFrequency;
    @FXML private TextField sshPath;
    @FXML private TextField pushFrequencyInput;
    @FXML private CheckBox sshEncrypted;
    @Inject private DialogService dialogService;

    public GitTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Git Support");
    }

    @Override
    public Node getStyleableNode() {
        return super.getStyleableNode();
    }

    @FXML
    private void authentify() {
        this.viewModel.authentify();
    }

    @FXML
    private void sshBrowse(ActionEvent event) {
        this.viewModel.sshBrowse();
        // System.out.println("Reached browse in gitTab");
    }

    public void initialize() {
        this.viewModel = new GitViewModel(preferences.getGitPreferences(), dialogService);
        pushFrequencyInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));

        enableGitSupport.selectedProperty().bindBidirectional(viewModel.gitSupportEnabledProperty());
        pushFrequency.disableProperty().bind(enableGitSupport.selectedProperty().not());
        pushFrequencyInput.disableProperty().bind(enableGitSupport.selectedProperty().not());

        sshPath.textProperty().bindBidirectional(viewModel.sshPathProperty());
        hostKeyCheck.selectedProperty().bindBidirectional(viewModel.getHostKeyCheckProperty());
        sshEncrypted.selectedProperty().bindBidirectional(viewModel.getSshEncryptedProperty());
        pushFrequency.selectedProperty().bindBidirectional(viewModel.getFrequencyLabelEnabledProperty());
        pushFrequencyInput.textProperty().bindBidirectional(viewModel.getPushFrequency());
    }
}
