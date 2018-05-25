package org.jabref.gui.shared;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedDatabaseLoginDialogView extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedDatabaseLoginDialogView.class);

    @FXML private ComboBox<DBMSType> cmbDbType;
    @FXML private TextField tbHost;
    @FXML private TextField tbDb;
    @FXML private TextField tbPort;
    @FXML private TextField tbUser;
    @FXML private PasswordField tbPwd;
    @FXML private CheckBox chkRememberPassword;
    @FXML private TextField tbFolder;
    @FXML private Button btnBrowse;
    @FXML private CheckBox chkAutosave;
    @FXML private ButtonType connectButton;

    @Inject private DialogService dialogService;

    private final SharedDatabasePreferences prefs = new SharedDatabasePreferences();

    private final JabRefFrame frame;

    private DBMSConnectionProperties connectionProperties;
    private SharedDatabaseLoginDialogViewModel viewModel;

    public SharedDatabaseLoginDialogView(JabRefFrame frame) {
        this.frame = frame;
        this.setTitle(Localization.lang("Connect to shared database"));

        ViewLoader.view(this)
                  .load()
                  .setAsContent(this.getDialogPane());

        ControlHelper.setAction(connectButton, this.getDialogPane(), event -> openDatabase());

    }

    @FXML
    private void openDatabase() {

        try {
            checkFields();

            connectionProperties = new DBMSConnectionProperties();
            connectionProperties.setType(cmbDbType.getSelectionModel().getSelectedItem());
            connectionProperties.setHost(tbHost.getText());
            connectionProperties.setPort(Integer.parseInt(tbPort.getText()));
            connectionProperties.setDatabase(tbDb.getText());
            connectionProperties.setUser(tbUser.getText());
            connectionProperties.setPassword(tbPwd.getText());

            openSharedDatabase();
        } catch (JabRefException exception) {
            dialogService.showErrorDialogAndWait(Localization.lang("Warning"), exception);

        }
    }

    @FXML
    private void initialize() {

        viewModel = new SharedDatabaseLoginDialogViewModel();

        cmbDbType.itemsProperty().bind(viewModel.dbmstypeProperty());
        cmbDbType.getSelectionModel().select(0);
        viewModel.selectedDbmstypeProperty().bind(cmbDbType.getSelectionModel().selectedItemProperty());

        viewModel.selectedDbmstypeProperty().addListener((observable, oldValue, newValue) -> {
            tbPort.setText(Integer.toString(newValue.getDefaultPort()));
        });

        btnBrowse.disableProperty().bind(chkAutosave.selectedProperty().not());
        tbFolder.disableProperty().bind(chkAutosave.selectedProperty().not());

        applyPreferences();
    }

    @FXML
    void openFileDialog(ActionEvent event) {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                                               .addExtensionFilter(FileType.BIBTEX_DB)
                                                                                               .withDefaultExtension(FileType.BIBTEX_DB)
                                                                                               .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
                                                                                               .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);
        exportPath.ifPresent(path -> {
            tbFolder.setText(path.toString());
        });
    }

    private void openSharedDatabase() {

        if (isSharedDatabaseAlreadyPresent()) {

            dialogService.showWarningDialogAndWait(Localization.lang("Shared database connection"),
                                                   Localization.lang("You are already connected to a database using entered connection details."));

            return;
        }

        if (chkAutosave.isSelected()) {

            Path localFilePath = Paths.get(tbFolder.getText());

            if (Files.exists(localFilePath) && !Files.isDirectory(localFilePath)) {

                boolean overwriteFilePressed = dialogService.showConfirmationDialogAndWait(Localization.lang("Existing file"),
                                                                                           Localization.lang("'%0' exists. Overwrite file?", localFilePath.getFileName().toString()),
                                                                                           Localization.lang("Overwrite file"),
                                                                                           Localization.lang("Cancel"));
                if (!overwriteFilePressed) {
                    tbFolder.requestFocus();
                    return;
                }
            }
        }

        setLoadingConnectButtonText(true);

        try {
            SharedDatabaseUIManager manager = new SharedDatabaseUIManager(frame);
            BasePanel panel = manager.openNewSharedDatabaseTab(connectionProperties);
            setPreferences();

            if (!tbFolder.getText().isEmpty()) {
                try {
                    new SaveDatabaseAction(panel, Paths.get(tbFolder.getText())).runCommand();
                } catch (Throwable e) {
                    LOGGER.error("Error while saving the database", e);
                }
            }

            return; // setLoadingConnectButtonText(false) should not be reached regularly.
        } catch (SQLException | InvalidDBMSConnectionPropertiesException exception) {

            frame.getDialogService().showErrorDialogAndWait(Localization.lang("Connection error"), exception);

        } catch (DatabaseNotSupportedException exception) {
            ButtonType openHelp = new ButtonType("Open Help", ButtonData.OTHER);

            Optional<ButtonType> result = dialogService.showCustomButtonDialogAndWait(AlertType.INFORMATION,
                                                                                      Localization.lang("Migration help information"),
                                                                                      Localization.lang("Entered database has obsolete structure and is no longer supported.")
                                                                                                                                       + "\n" +
                                                                                                                                       Localization.lang("Click help to learn about the migration of pre-3.6 databases.")
                                                                                                                                       + "\n" +
                                                                                                                                       Localization.lang("However, a new database was created alongside the pre-3.6 one."),
                                                                                      ButtonType.OK, openHelp);

            result.filter(btn -> btn.equals(openHelp)).ifPresent(btn -> HelpAction.openHelpPage(HelpFile.SQL_DATABASE_MIGRATION));
            result.filter(btn -> btn == ButtonType.OK).ifPresent(btn -> openSharedDatabase());

        }

        setLoadingConnectButtonText(false);
    }

    private void setLoadingConnectButtonText(boolean isLoading) {

        if (isLoading) {
            //    connectButton.setText(Localization.lang("Connecting..."));
        } else {
            //  connectButton.setText(Localization.lang("Connect"));
        }
    }

    private boolean isSharedDatabaseAlreadyPresent() {
        List<BasePanel> panels = frame.getBasePanelList();
        return panels.parallelStream().anyMatch(panel -> {
            BibDatabaseContext context = panel.getBibDatabaseContext();

            return ((context.getLocation() == DatabaseLocation.SHARED) &&
                    this.connectionProperties.equals(context.getDBMSSynchronizer().getConnectionProperties()));
        });
    }

    private void setPreferences() {
        prefs.setType(cmbDbType.getSelectionModel().getSelectedItem().toString());
        prefs.setHost(tbHost.getText());
        prefs.setPort(tbPort.getText());
        prefs.setName(tbDb.getText());
        prefs.setUser(tbUser.getText());

        if (chkRememberPassword.isSelected()) {
            try {
                prefs.setPassword(new Password(tbPwd.getText(), tbPwd.getText()).encrypt());
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                LOGGER.error("Could not store the password due to encryption problems.", e);
            }
        } else {
            prefs.clearPassword(); // for the case that the password is already set
        }

        prefs.setRememberPassword(chkRememberPassword.isSelected());
    }

    private void checkFields() throws JabRefException {
        if (isEmptyField(tbHost)) {
            tbHost.requestFocus();
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Host")));
        }
        if (isEmptyField(tbPort)) {
            tbPort.requestFocus();
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Port")));
        }
        if (isEmptyField(tbDb)) {
            tbDb.requestFocus();
            throw new JabRefException(
                                      Localization.lang("Required field \"%0\" is empty.", Localization.lang("Library")));
        }
        if (isEmptyField(tbUser)) {
            tbUser.requestFocus();
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("User")));
        }
        if (chkAutosave.isSelected() && isEmptyField(tbFolder)) {
            tbFolder.requestFocus();
            throw new JabRefException(Localization.lang("Please enter a valid file path."));
        }
    }

    private boolean isEmptyField(TextField field) {
        return field.getText().trim().length() == 0;
    }

    /**
     * Fetches possibly saved data and configures the control elements respectively.
     */
    private void applyPreferences() {
        Optional<String> sharedDatabaseType = prefs.getType();
        Optional<String> sharedDatabaseHost = prefs.getHost();
        Optional<String> sharedDatabasePort = prefs.getPort();
        Optional<String> sharedDatabaseName = prefs.getName();
        Optional<String> sharedDatabaseUser = prefs.getUser();
        Optional<String> sharedDatabasePassword = prefs.getPassword();
        boolean sharedDatabaseRememberPassword = prefs.getRememberPassword();

        if (sharedDatabaseType.isPresent()) {
            Optional<DBMSType> dbmsType = DBMSType.fromString(sharedDatabaseType.get());
            dbmsType.ifPresent(dbtype -> cmbDbType.getSelectionModel().select(dbtype));
        }

        if (sharedDatabaseHost.isPresent()) {
            tbHost.setText(sharedDatabaseHost.get());
        }

        if (sharedDatabasePort.isPresent()) {
            tbPort.setText(sharedDatabasePort.get());
        }

        if (sharedDatabaseName.isPresent()) {
            tbDb.setText(sharedDatabaseName.get());
        }

        if (sharedDatabaseUser.isPresent()) {
            tbUser.setText(sharedDatabaseUser.get());
        }

        if (sharedDatabasePassword.isPresent() && sharedDatabaseUser.isPresent()) {
            try {
                tbPwd.setText(new Password(sharedDatabasePassword.get().toCharArray(), sharedDatabaseUser.get()).decrypt());
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                LOGGER.error("Could not read the password due to decryption problems.", e);
            }
        }

        chkRememberPassword.setSelected(sharedDatabaseRememberPassword);
    }

}
