package org.jabref.gui.openoffice;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.util.OS;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class ManualConnectDialogView extends BaseDialog<Void> {

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

    private final DirectoryDialogConfiguration dirDialogConfiguration;
    private final FileDialogConfiguration fileDialogConfiguration;
    private final DialogService dialogService;
    private OpenOfficePreferences preferences;

    public ManualConnectDialogView(DialogService dialogService) {
        this.dialogService = dialogService;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        final NativeDesktop nativeDesktop = JabRefDesktop.getNativeDesktop();

        dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                                                                           .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                                                                           .build();
        fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                       .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                                                                       .build();

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (OS.WINDOWS || OS.OS_X) {
                    preferences.updateConnectionParams(ooPath.getText(), ooPath.getText(), ooPath.getText());
                } else {
                    preferences.updateConnectionParams(ooPath.getText(), ooExec.getText(), ooJars.getText());
                }
            }
            return null;
        });

    }

    @FXML
    private void initialize() {

        preferences = preferencesService.getOpenOfficePreferences();
        ooPath.setText(preferences.getInstallationPath());
        ooExec.setText(preferences.getExecutablePath());
        ooJars.setText(preferences.getJarsPath());

        ooExecLabel.managedProperty().bind(ooExec.visibleProperty());
        ooExec.managedProperty().bind(ooExec.visibleProperty());
        browseOOExec.managedProperty().bind(ooExec.visibleProperty());

        OOPathLabel.managedProperty().bind(ooPath.visibleProperty());
        ooPath.managedProperty().bind(ooPath.visibleProperty());
        browseOOPath.managedProperty().bind(ooPath.visibleProperty());

        ooJarsLabel.managedProperty().bind(ooJars.visibleProperty());
        ooJars.managedProperty().bind(ooJars.visibleProperty());
        browseOOJars.managedProperty().bind(ooJars.visibleProperty());

        if (OS.WINDOWS || OS.OS_X) {
            ooExec.setVisible(true);
            ooPath.setVisible(false);
            ooJars.setVisible(false);
        } else {
            ooExec.setVisible(false);
            ooPath.setVisible(true);
            ooJars.setVisible(true);
        }

    }

    @FXML
    void browseOOExec(ActionEvent event) {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(f -> ooExec.setText(f.toAbsolutePath().toString()));
    }

    @FXML
    void browseOOJars(ActionEvent event) {
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration).ifPresent(f -> ooJars.setText(f.toAbsolutePath().toString()));
    }

    @FXML
    void browseOOPath(ActionEvent event) {
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration).ifPresent(f -> ooPath.setText(f.toAbsolutePath().toString()));
    }

}
