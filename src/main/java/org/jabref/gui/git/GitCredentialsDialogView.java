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

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class GitCredentialsDialogView extends BaseDialog<Void> {

    @FXML private ButtonType copyVersionButton;
    @FXML private TextArea textAreaVersions;


    public GitCredentialsDialogView() {
        this.setTitle(Localization.lang("Git credentials"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        this.setResizable(false);
    }
}
