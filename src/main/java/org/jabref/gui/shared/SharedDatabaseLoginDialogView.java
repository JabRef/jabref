package org.jabref.gui.shared;

import java.nio.file.Path;
import java.util.Optional;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class SharedDatabaseLoginDialogView extends BaseDialog<Void> {

    @FXML private ComboBox<DBMSType> cmbDbType;
    @FXML private TextField tbHost;
    @FXML private TextField tbDb;
    @FXML private TextField tbPort;
    @FXML private TextField tbUser;
    @FXML private PasswordField tbPwd;
    @FXML private CheckBox chkRememberPassword;
    @FXML private TextField tbFolder;
    @FXML private Button btnBrowse;

    @Inject private DialogService dialogService;

    private final SharedDatabaseLoginDialogViewModel viewModel;

    @FXML
    private void initialize() {
        //todo
    }

    public SharedDatabaseLoginDialogView() {
        this.setTitle(Localization.lang("Connect to shared database"));
        this.setResizable(true);

        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        viewModel = new SharedDatabaseLoginDialogViewModel();

        ViewLoader.view(this)
                  .load()
                  .setAsContent(this.getDialogPane());
    }

    @FXML
    void openFileDialog(ActionEvent event) {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                                               .addExtensionFilter(FileType.BIBTEX_DB)
                                                                                               .withDefaultExtension(FileType.BIBTEX_DB)
                                                                                               .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
                                                                                               .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);
    }

}
