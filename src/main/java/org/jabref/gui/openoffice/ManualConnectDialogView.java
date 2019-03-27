package org.jabref.gui.openoffice;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class ManualConnectDialogView extends BaseDialog<Boolean> {

    @Inject private final DialogService dialogService;
    @FXML private Label OOPathLabel;
    @FXML private TextField ooPath;
    @FXML private Button browseOOPath;
    @FXML private Button browseOOExec;
    @FXML private Button browseOOJars;
    @FXML private TextField ooExec;
    @FXML private TextField ooJars;
    @FXML private Label ooExecLabel;
    @FXML private Label ooJarsLabel;
    @Inject private PreferencesService preferencesService;
    private ManualConnectDialogViewModel viewModel;

    public ManualConnectDialogView(DialogService dialogService) {
        this.dialogService = dialogService;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                viewModel.save();
                return true;
            }
            return null;
        });

        setTitle(Localization.lang("Set connection parameters"));
    }

    @FXML
    private void initialize() {

        viewModel = new ManualConnectDialogViewModel(preferencesService, dialogService);

        ooPath.textProperty().bindBidirectional(viewModel.ooPathProperty());
        ooExec.textProperty().bindBidirectional(viewModel.ooExecProperty());
        ooJars.textProperty().bind(viewModel.ooJarsProperty());

        if (OS.WINDOWS || OS.OS_X) {
            ooPath.setVisible(true);
            ooExec.setVisible(false);
            ooJars.setVisible(false);
        } else {
            ooPath.setVisible(false);
            ooExec.setVisible(false);
            ooJars.setVisible(true);
        }
    }

    @FXML
    private void browseOOPath(ActionEvent event) {
        viewModel.browseOOPath();
    }

    @FXML
    private void browseOOExec(ActionEvent event) {
        viewModel.browseOOExec();
    }

    @FXML
    private void browseOOJars(ActionEvent event) {
        viewModel.browseOOJars();
    }
}
