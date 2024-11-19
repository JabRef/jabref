package org.jabref.gui.preferences.git;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
    @FXML private CheckBox frequencyLabel;
    @FXML private ComboBox<String> setPushFrequency;
    @FXML private Button authentifyButton;
    @FXML private ComboBox<String> authenticationMethod;
    @FXML private Button synchronizeButton;
    @FXML private TextField sshPath;
    @FXML private Label authentificationLabel;
    @Inject private DialogService dialogService;

    @FXML
    private TextField pushFrequencyInput;

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
            // Allow only digits
            if (newText.matches("\\d*")) {
                return change; // Accept the change if it matches digits
            }
            return null; // Reject the change
        }));

        enableGitSupport.selectedProperty().bindBidirectional(viewModel.gitSupportEnabledProperty());
//        frequencyLabel.selectedProperty().bindBidirectional(viewModel.frequencyLabelEnabledProperty());
//        frequencyLabel.disableProperty().bind(enableGitSupport.selectedProperty().not());
        synchronizeButton.disableProperty().bind(enableGitSupport.selectedProperty().not());
        authentifyButton.disableProperty().bind(enableGitSupport.selectedProperty().not());
        sshPath.disableProperty().bind(enableGitSupport.selectedProperty().not());
        sshPath.textProperty().bindBidirectional(viewModel.sshPathProperty());

        // System.out.println("LOG: USERNAME = " + preferences.getGitPreferences().getUsernameProperty());
        // System.out.println("GITTAB: SSH PATH = " + preferences.getGitPreferences().getSshPath());
    }
}
