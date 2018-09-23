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
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.util.OS;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class ManualConnectDialogView extends BaseDialog<Boolean> {

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

    private final DialogService dialogService;
    private OpenOfficePreferences preferences;
    private ManualConnectDialogViewModel viewModel;

    public ManualConnectDialogView(DialogService dialogService) {
        this.dialogService = dialogService;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (OS.WINDOWS || OS.OS_X) {
                    preferences.updateConnectionParams(viewModel.ooPathProperty().getValue(), viewModel.ooPathProperty().getValue(), viewModel.ooPathProperty().getValue());
                } else {
                    preferences.updateConnectionParams(viewModel.ooPathProperty().getValue(), viewModel.ooExecProperty().getValue(), viewModel.ooJarsProperty().getValue());
                }

                preferencesService.setOpenOfficePreferences(preferences);

                return true;
            }
            return null;
        });

    }

    @FXML
    private void initialize() {
        this.preferences = preferencesService.getOpenOfficePreferences();

        viewModel = new ManualConnectDialogViewModel(preferencesService.getOpenOfficePreferences(), dialogService);

        OOPathLabel.managedProperty().bind(ooPath.visibleProperty());
        ooPath.managedProperty().bind(ooPath.visibleProperty());
        ooPath.textProperty().bindBidirectional(viewModel.ooPathProperty());
        browseOOPath.managedProperty().bind(ooPath.visibleProperty());

        ooExecLabel.managedProperty().bind(ooExec.visibleProperty());
        ooExec.managedProperty().bind(ooExec.visibleProperty());
        ooExec.textProperty().bindBidirectional(viewModel.ooExecProperty());
        browseOOExec.managedProperty().bind(ooExec.visibleProperty());

        ooJarsLabel.managedProperty().bind(ooJars.visibleProperty());
        ooJars.managedProperty().bind(ooJars.visibleProperty());
        ooJars.textProperty().bind(viewModel.ooJarsProperty());
        browseOOJars.managedProperty().bind(ooJars.visibleProperty());

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
    void browseOOPath(ActionEvent event) {
        viewModel.browseOOPath();
    }

    @FXML
    void browseOOExec(ActionEvent event) {
        viewModel.browseOOExec();
    }

    @FXML
    void browseOOJars(ActionEvent event) {
        viewModel.browseOOJars();
    }

}
