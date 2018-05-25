package org.jabref.gui.shared;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

import org.jabref.Globals;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.help.HelpAction;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedDatabaseLoginDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedDatabaseLoginDialogViewModel.class);

    private final ListProperty<DBMSType> allDBMSTypes = new SimpleListProperty<>(FXCollections.observableArrayList(DBMSType.values()));
    private final ObjectProperty<DBMSType> selectedDBMSType = new SimpleObjectProperty<>();

    private final StringProperty db = new SimpleStringProperty("");
    private final StringProperty host = new SimpleStringProperty("");
    private final StringProperty port = new SimpleStringProperty("");
    private final StringProperty user = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty folder = new SimpleStringProperty("");
    private final BooleanProperty autosave = new SimpleBooleanProperty();
    private final BooleanProperty rememberPassword = new SimpleBooleanProperty();

    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final SharedDatabasePreferences prefs = new SharedDatabasePreferences();

    private DBMSConnectionProperties connectionProperties;

    public SharedDatabaseLoginDialogViewModel(JabRefFrame frame, DialogService dialogService) {
        this.frame = frame;
        this.dialogService = dialogService;

        selectedDbmstypeProperty().addListener((observable, oldValue, newValue) -> {
            port.setValue(Integer.toString(newValue.getDefaultPort()));
        });
    }

    public void openDatabase() {

        connectionProperties = new DBMSConnectionProperties();
        connectionProperties.setType(selectedDBMSType.getValue());
        connectionProperties.setHost(host.getValue());
        connectionProperties.setPort(Integer.parseInt(port.getValue()));
        connectionProperties.setDatabase(db.getValue());
        connectionProperties.setUser(user.getValue());
        connectionProperties.setPassword(password.getValue());

        openSharedDatabase();
    }

    private void openSharedDatabase() {
        if (isSharedDatabaseAlreadyPresent()) {

            dialogService.showWarningDialogAndWait(Localization.lang("Shared database connection"),
                                                   Localization.lang("You are already connected to a database using entered connection details."));
            return;
        }

        if (autosave.get()) {
            Path localFilePath = Paths.get(folder.getValue());

            if (Files.exists(localFilePath) && !Files.isDirectory(localFilePath)) {

                boolean overwriteFilePressed = dialogService.showConfirmationDialogAndWait(Localization.lang("Existing file"),
                                                                                           Localization.lang("'%0' exists. Overwrite file?", localFilePath.getFileName().toString()),
                                                                                           Localization.lang("Overwrite file"),
                                                                                           Localization.lang("Cancel"));
                if (!overwriteFilePressed) {
                    // tbFolder.requestFocus();
                    return;
                }
            }
        }

        //  setLoadingConnectButtonText(true);

        try {
            SharedDatabaseUIManager manager = new SharedDatabaseUIManager(frame);
            BasePanel panel = manager.openNewSharedDatabaseTab(connectionProperties);
            setPreferences();

            if (!folder.getValue().isEmpty()) {
                try {
                    new SaveDatabaseAction(panel, Paths.get(folder.getValue())).runCommand();
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

    }

    private void setPreferences() {
        prefs.setType(selectedDBMSType.getValue().toString());
        prefs.setHost(host.getValue());
        prefs.setPort(port.getValue());
        prefs.setName(db.getValue());
        prefs.setUser(user.getValue());

        if (rememberPassword.get()) {
            try {
                prefs.setPassword(new Password(password.getValue(), password.getValue()).encrypt());
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                LOGGER.error("Could not store the password due to encryption problems.", e);
            }
        } else {
            prefs.clearPassword(); // for the case that the password is already set
        }

        prefs.setRememberPassword(rememberPassword.get());
    }

    /**
     * Fetches possibly saved data and configures the control elements respectively.
     */
    public void applyPreferences() {
        Optional<String> sharedDatabaseType = prefs.getType();
        Optional<String> sharedDatabaseHost = prefs.getHost();
        Optional<String> sharedDatabasePort = prefs.getPort();
        Optional<String> sharedDatabaseName = prefs.getName();
        Optional<String> sharedDatabaseUser = prefs.getUser();
        Optional<String> sharedDatabasePassword = prefs.getPassword();
        boolean sharedDatabaseRememberPassword = prefs.getRememberPassword();

        if (sharedDatabaseType.isPresent()) {
            Optional<DBMSType> dbmsType = DBMSType.fromString(sharedDatabaseType.get());
            dbmsType.ifPresent(selectedDBMSType::set);
        }

        sharedDatabaseHost.ifPresent(host::set);
        sharedDatabasePort.ifPresent(port::set);
        sharedDatabaseName.ifPresent(db::set);
        sharedDatabaseUser.ifPresent(user::set);

        if (sharedDatabasePassword.isPresent() && sharedDatabaseUser.isPresent()) {
            try {
                password.setValue(new Password(sharedDatabasePassword.get().toCharArray(), sharedDatabaseUser.get()).decrypt());
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                LOGGER.error("Could not read the password due to decryption problems.", e);
            }
        }

        rememberPassword.set(sharedDatabaseRememberPassword);
    }

    private boolean isSharedDatabaseAlreadyPresent() {
        List<BasePanel> panels = frame.getBasePanelList();
        return panels.parallelStream().anyMatch(panel -> {
            BibDatabaseContext context = panel.getBibDatabaseContext();

            return ((context.getLocation() == DatabaseLocation.SHARED) &&
                    this.connectionProperties.equals(context.getDBMSSynchronizer().getConnectionProperties()));
        });
    }

    public void openFileDialog() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                                               .addExtensionFilter(FileType.BIBTEX_DB)
                                                                                               .withDefaultExtension(FileType.BIBTEX_DB)
                                                                                               .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
                                                                                               .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);
        exportPath.ifPresent(path -> {
            folder.setValue(path.toString());
        });
    }

    public StringProperty dbProperty() {
        return db;
    }

    public StringProperty hostProperty() {
        return host;
    }

    public StringProperty portProperty() {
        return port;
    }

    public StringProperty userProperty() {
        return user;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public BooleanProperty autosaveProperty() {
        return autosave;
    }

    public BooleanProperty rememberPasswordProperty() {
        return rememberPassword;
    }

    public StringProperty folderProperty() {
        return folder;
    }

    public ListProperty<DBMSType> dbmstypeProperty() {
        return allDBMSTypes;
    }

    public ObjectProperty<DBMSType> selectedDbmstypeProperty() {
        return selectedDBMSType;
    }
}
