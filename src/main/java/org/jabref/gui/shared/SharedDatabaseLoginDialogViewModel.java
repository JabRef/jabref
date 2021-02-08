package org.jabref.gui.shared;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSConnectionPropertiesBuilder;
import org.jabref.logic.shared.DBMSType;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;

import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedDatabaseLoginDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedDatabaseLoginDialogViewModel.class);

    private final ObjectProperty<DBMSType> selectedDBMSType = new SimpleObjectProperty<>(DBMSType.values()[0]);

    private final StringProperty database = new SimpleStringProperty("");
    private final StringProperty host = new SimpleStringProperty("");
    private final StringProperty port = new SimpleStringProperty("");
    private final StringProperty user = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty folder = new SimpleStringProperty("");
    private final BooleanProperty autosave = new SimpleBooleanProperty();
    private final BooleanProperty rememberPassword = new SimpleBooleanProperty();
    private final BooleanProperty loading = new SimpleBooleanProperty();
    private final StringProperty keystore = new SimpleStringProperty("");
    private final BooleanProperty useSSL = new SimpleBooleanProperty();
    private final StringProperty keyStorePasswordProperty = new SimpleStringProperty("");
    private final StringProperty serverTimezone = new SimpleStringProperty("");

    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final SharedDatabasePreferences prefs = new SharedDatabasePreferences();

    private final Validator databaseValidator;
    private final Validator hostValidator;
    private final Validator portValidator;
    private final Validator userValidator;
    private final Validator folderValidator;
    private final Validator keystoreValidator;
    private final CompositeValidator formValidator;

    public SharedDatabaseLoginDialogViewModel(JabRefFrame frame, DialogService dialogService) {
        this.frame = frame;
        this.dialogService = dialogService;

        EasyBind.subscribe(selectedDBMSType, selected -> port.setValue(Integer.toString(selected.getDefaultPort())));

        Predicate<String> notEmpty = input -> (input != null) && !input.trim().isEmpty();
        Predicate<String> fileExists = input -> Files.exists(Path.of(input));
        Predicate<String> notEmptyAndfilesExist = notEmpty.and(fileExists);

        databaseValidator = new FunctionBasedValidator<>(database, notEmpty, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Library"))));
        hostValidator = new FunctionBasedValidator<>(host, notEmpty, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Port"))));
        portValidator = new FunctionBasedValidator<>(port, notEmpty, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Host"))));
        userValidator = new FunctionBasedValidator<>(user, notEmpty, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("User"))));
        folderValidator = new FunctionBasedValidator<>(folder, notEmptyAndfilesExist, ValidationMessage.error(Localization.lang("Please enter a valid file path.")));
        keystoreValidator = new FunctionBasedValidator<>(keystore, notEmptyAndfilesExist, ValidationMessage.error(Localization.lang("Please enter a valid file path.")));

        formValidator = new CompositeValidator();
        formValidator.addValidators(databaseValidator, hostValidator, portValidator, userValidator);

        applyPreferences();
    }

    public boolean openDatabase() {
        DBMSConnectionProperties connectionProperties = new DBMSConnectionPropertiesBuilder()
                .setType(selectedDBMSType.getValue())
                .setHost(host.getValue())
                .setPort(Integer.parseInt(port.getValue()))
                .setDatabase(database.getValue())
                .setUser(user.getValue())
                .setPassword(password.getValue())
                .setUseSSL(useSSL.getValue())
                // Authorize client to retrieve RSA server public key when serverRsaPublicKeyFile is not set (for sha256_password and caching_sha2_password authentication password)
                .setAllowPublicKeyRetrieval(true)
                .setKeyStore(keystore.getValue())
                .setServerTimezone(serverTimezone.getValue())
                .createDBMSConnectionProperties();

        setupKeyStore();
        return openSharedDatabase(connectionProperties);
    }

    private void setupKeyStore() {
        System.setProperty("javax.net.ssl.trustStore", keystore.getValue());
        System.setProperty("javax.net.ssl.trustStorePassword", keyStorePasswordProperty.getValue());
        System.setProperty("javax.net.debug", "ssl");
    }

    private boolean openSharedDatabase(DBMSConnectionProperties connectionProperties) {
        if (isSharedDatabaseAlreadyPresent(connectionProperties)) {

            dialogService.showWarningDialogAndWait(Localization.lang("Shared database connection"),
                    Localization.lang("You are already connected to a database using entered connection details."));
            return true;
        }

        if (autosave.get()) {
            Path localFilePath = Path.of(folder.getValue());

            if (Files.exists(localFilePath) && !Files.isDirectory(localFilePath)) {

                boolean overwriteFilePressed = dialogService.showConfirmationDialogAndWait(Localization.lang("Existing file"),
                        Localization.lang("'%0' exists. Overwrite file?", localFilePath.getFileName().toString()),
                        Localization.lang("Overwrite file"),
                        Localization.lang("Cancel"));
                if (!overwriteFilePressed) {
                    return true;
                }
            }
        }

        loading.set(true);

        try {
            SharedDatabaseUIManager manager = new SharedDatabaseUIManager(frame);
            LibraryTab libraryTab = manager.openNewSharedDatabaseTab(connectionProperties);
            setPreferences();

            if (!folder.getValue().isEmpty()) {
                try {
                    new SaveDatabaseAction(libraryTab, Globals.prefs, Globals.entryTypesManager).saveAs(Path.of(folder.getValue()));
                } catch (Throwable e) {
                    LOGGER.error("Error while saving the database", e);
                }
            }

            return true;
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
            result.filter(btn -> btn.equals(ButtonType.OK)).ifPresent(btn -> openSharedDatabase(connectionProperties));
        }
        loading.set(false);
        return false;
    }

    private void setPreferences() {
        prefs.setType(selectedDBMSType.getValue().toString());
        prefs.setHost(host.getValue());
        prefs.setPort(port.getValue());
        prefs.setName(database.getValue());
        prefs.setUser(user.getValue());
        prefs.setUseSSL(useSSL.getValue());
        prefs.setKeystoreFile(keystore.getValue());
        prefs.setServerTimezone(serverTimezone.getValue());

        if (rememberPassword.get()) {
            try {
                prefs.setPassword(new Password(password.getValue(), user.getValue()).encrypt());
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
    private void applyPreferences() {
        Optional<String> sharedDatabaseType = prefs.getType();
        Optional<String> sharedDatabaseHost = prefs.getHost();
        Optional<String> sharedDatabasePort = prefs.getPort();
        Optional<String> sharedDatabaseName = prefs.getName();
        Optional<String> sharedDatabaseUser = prefs.getUser();
        Optional<String> sharedDatabasePassword = prefs.getPassword();
        boolean sharedDatabaseRememberPassword = prefs.getRememberPassword();
        Optional<String> sharedDatabaseKeystoreFile = prefs.getKeyStoreFile();

        if (sharedDatabaseType.isPresent()) {
            Optional<DBMSType> dbmsType = DBMSType.fromString(sharedDatabaseType.get());
            dbmsType.ifPresent(selectedDBMSType::set);
        }

        sharedDatabaseHost.ifPresent(host::set);
        sharedDatabasePort.ifPresent(port::set);
        sharedDatabaseName.ifPresent(database::set);
        sharedDatabaseUser.ifPresent(user::set);
        sharedDatabaseKeystoreFile.ifPresent(keystore::set);
        useSSL.setValue(prefs.isUseSSL());

        if (sharedDatabasePassword.isPresent() && sharedDatabaseUser.isPresent()) {
            try {
                password.setValue(new Password(sharedDatabasePassword.get().toCharArray(), sharedDatabaseUser.get()).decrypt());
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                LOGGER.error("Could not read the password due to decryption problems.", e);
            }
        }

        rememberPassword.set(sharedDatabaseRememberPassword);
    }

    private boolean isSharedDatabaseAlreadyPresent(DBMSConnectionProperties connectionProperties) {
        List<LibraryTab> panels = frame.getLibraryTabs();
        return panels.parallelStream().anyMatch(panel -> {
            BibDatabaseContext context = panel.getBibDatabaseContext();

            return ((context.getLocation() == DatabaseLocation.SHARED) &&
                    connectionProperties.equals(context.getDBMSSynchronizer().getConnectionProperties()));
        });
    }

    public void showSaveDbToFileDialog() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.BIBTEX_DB)
                .withDefaultExtension(StandardFileType.BIBTEX_DB)
                .withInitialDirectory(Globals.prefs.getWorkingDir())
                .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);
        exportPath.ifPresent(path -> folder.setValue(path.toString()));
    }

    public void showOpenKeystoreFileDialog() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.ANY_FILE)
                .addExtensionFilter(StandardFileType.JAVA_KEYSTORE)
                .withDefaultExtension(StandardFileType.JAVA_KEYSTORE)
                .withInitialDirectory(Globals.prefs.getWorkingDir())
                .build();
        Optional<Path> keystorePath = dialogService.showFileOpenDialog(fileDialogConfiguration);
        keystorePath.ifPresent(path -> keystore.setValue(path.toString()));
    }

    public StringProperty databaseproperty() {
        return database;
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

    public StringProperty keyStoreProperty() {
        return keystore;
    }

    public StringProperty keyStorePasswordProperty() {
        return keyStorePasswordProperty;
    }

    public BooleanProperty useSSLProperty() {
        return useSSL;
    }

    public ObjectProperty<DBMSType> selectedDbmstypeProperty() {
        return selectedDBMSType;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public ValidationStatus dbValidation() {
        return databaseValidator.getValidationStatus();
    }

    public ValidationStatus hostValidation() {
        return hostValidator.getValidationStatus();
    }

    public ValidationStatus portValidation() {
        return portValidator.getValidationStatus();
    }

    public ValidationStatus userValidation() {
        return userValidator.getValidationStatus();
    }

    public ValidationStatus folderValidation() {
        return folderValidator.getValidationStatus();
    }

    public ValidationStatus keystoreValidation() {
        return keystoreValidator.getValidationStatus();
    }

    public ValidationStatus formValidation() {
        return formValidator.getValidationStatus();
    }

    public StringProperty serverTimezoneProperty() {
        return serverTimezone;
    }
}
