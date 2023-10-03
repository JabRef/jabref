package org.jabref.gui.git;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class GitCredentialsDialogView extends BaseDialog<Void> {

    @FXML private ButtonType copyVersionButton;
    @FXML private TextArea textAreaVersions;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferencesService;



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
