package org.jabref.gui.sharelatex;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.jabref.JabRefGUI;
import org.jabref.gui.AbstractController;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialog;
import org.jabref.gui.FXDialogService;
import org.jabref.logic.sharelatex.ShareLatexManager;
import org.jabref.logic.sharelatex.SharelatexConnectionProperties;
import org.jabref.logic.sharelatex.SharelatexPreferences;

public class ShareLatexLoginDialogController extends AbstractController<ShareLatexLoginDialogViewModel> {

    @FXML private TextField tbAddress;
    @FXML private TextField tbUsername;
    @FXML private PasswordField tbPassword;
    @FXML private Button btnLogin;
    @Inject private ShareLatexManager manager;

    private SharelatexConnectionProperties props;

    @FXML
    private void initialize() {
        viewModel = new ShareLatexLoginDialogViewModel();
    }

    @FXML
    private void closeDialog() {
        getStage().close();
    }

    @FXML
    private void signIn() {
        btnLogin.setText("Logging in....");
        try {
            String result = manager.login(tbAddress.getText(), tbUsername.getText(), tbPassword.getText());
            if (result.contains("incorrect")) {
                FXDialog dlg = new FXDialog(AlertType.ERROR);
                dlg.setContentText("Your email or password is incorrect. Please try again");
                dlg.showAndWait();
            } else {
                //TODO: Wait until pdf + injection stuff gets merged
                props = new SharelatexConnectionProperties(new SharelatexPreferences("default"));
                props.setUrl(tbAddress.getText());
                props.setUser(tbUsername.getText());
                props.setPassword(tbPassword.getText());

                manager.setConnectionProperties(props);

                ShareLatexProjectDialogView dlgprojects = new ShareLatexProjectDialogView();
                dlgprojects.show();
                closeDialog();
                JabRefGUI.getMainFrame().getSynchronizeWithSharelatexAction().setEnabled(false);

            }
        } catch (Exception e) {
            DialogService dlg = new FXDialogService();
            dlg.showErrorDialogAndWait(e);

        }

    }
}
