package org.jabref.gui.git;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;

public class GitCredentialsDialogView extends BaseDialog<Void> {

    @FXML private ButtonType copyVersionButton;
    @FXML private TextArea textAreaVersions;
    private DialogService dialogService;
    private DialogPane pane;
    
    private ButtonType acceptButton;
    private ButtonType cancelButton;
    private TextField inputGitUsername;
    private PasswordField inputGitPassword;


    public GitCredentialsDialogView() {
        this.setTitle(Localization.lang("Git credentials"));
        this.dialogService = Injector.instantiateModelOrService(DialogService.class);

        // ViewLoader.view(this)
        //           .load()
        //           .setAsDialogPane(this);

        this.pane = new DialogPane();
        VBox vBox = new VBox();
        this.inputGitUsername = new TextField();
        this.inputGitPassword = new PasswordField();
        this.acceptButton = new ButtonType(Localization.lang("Accept"), ButtonBar.ButtonData.APPLY);
        this.cancelButton = new ButtonType(Localization.lang("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        vBox.getChildren().add(new Label(Localization.lang("Git username")));
        vBox.getChildren().add(this.inputGitUsername);
        vBox.getChildren().add(new Label(Localization.lang("Git password")));
        vBox.getChildren().add(this.inputGitPassword);

        this.pane.setContent(vBox);
        
    }

    public void showGitCredentialsDialog() {
        dialogService.showCustomDialogAndWait(Localization.lang("Git credentials"), this.pane, this.acceptButton, this.cancelButton);
    }

    public GitCredentials getCredentials() {
        dialogService.showCustomDialogAndWait(Localization.lang("Git credentials"), this.pane, this.acceptButton, this.cancelButton);
        GitCredentials gitCredentials = new GitCredentials(this.inputGitUsername.getText(), this.inputGitPassword.getText());

        return gitCredentials;
    }

    public String getGitPassword() {
        return this.inputGitPassword.getText();
    }

    public String getGitUsername() {
        return this.inputGitUsername.getText();
    }

    @FXML
    private void initialize() {
        this.setResizable(false);
    }
}
