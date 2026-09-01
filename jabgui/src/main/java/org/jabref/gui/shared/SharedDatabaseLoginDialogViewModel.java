package org.jabref.gui.shared;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.GuiUndoManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSConnectionPropertiesBuilder;
import org.jabref.logic.shared.DBMSConnectionUrl;
import org.jabref.logic.shared.DBMSType;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

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
    private final boolean keyringAvailable = OS.isKeyringAvailable();
    private final BooleanProperty loading = new SimpleBooleanProperty();
    private final BooleanProperty useSSL = new SimpleBooleanProperty();
    private final StringProperty serverTimezone = new SimpleStringProperty("");
    private final BooleanProperty expertMode = new SimpleBooleanProperty();
    private final StringProperty jdbcUrl = new SimpleStringProperty("");
    private final StringProperty connectionUrl = new SimpleStringProperty("");

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final AiService aiService;
    private final SharedDatabasePreferences sharedDatabasePreferences = new SharedDatabasePreferences();
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final GuiUndoManager undoManager;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    private final Validator databaseValidator;
    private final Validator hostValidator;
    private final Validator portValidator;
    private final Validator userValidator;
    private final Validator folderValidator;
    private final Validator connectionUrlValidator;
    private final CompositeValidator formValidator;

    public SharedDatabaseLoginDialogViewModel(LibraryTabContainer tabContainer,
                                              DialogService dialogService,
                                              GuiPreferences preferences,
                                              AiService aiService,
                                              StateManager stateManager,
                                              BibEntryTypesManager entryTypesManager,
                                              FileUpdateMonitor fileUpdateMonitor,
                                              GuiUndoManager undoManager,
                                              ClipBoardManager clipBoardManager,
                                              TaskExecutor taskExecutor,
                                              JournalAbbreviationRepository journalAbbreviationRepository) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.undoManager = undoManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.journalAbbreviationRepository = journalAbbreviationRepository;

        EasyBind.subscribe(selectedDBMSType, selected -> port.setValue(Integer.toString(selected.getDefaultPort())));
        EasyBind.subscribe(autosave, selected -> {
            String current = folder.getValue();
            folder.setValue(null);
            folder.setValue(current);
        });

        Predicate<String> notEmpty = input -> (input != null) && !input.isBlank();
        Predicate<String> folderRule = input -> {
            if (!autosave.get()) {
                return true;
            } else if (input != null) {
                try {
                    Path p = Path.of(input.trim());
                    p = p.getParent();
                    return (p != null) && Files.isDirectory(p);
                } catch (InvalidPathException e) {
                    return false;
                }
            }
            return false;
        };

        databaseValidator = new FunctionBasedValidator<>(database, notEmpty, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Library"))));
        hostValidator = new FunctionBasedValidator<>(host, notEmpty, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Host"))));
        portValidator = new FunctionBasedValidator<>(port, notEmpty, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Port"))));
        userValidator = new FunctionBasedValidator<>(user, notEmpty, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("User"))));
        folderValidator = new FunctionBasedValidator<>(folder, folderRule, ValidationMessage.error(Localization.lang("Please enter a valid file path.")));
        connectionUrlValidator = new FunctionBasedValidator<>(connectionUrl, input -> !notEmpty.test(input) || DBMSConnectionUrl.parse(input).isPresent(), ValidationMessage.error(Localization.lang("Not a PostgreSQL connection URL.")));

        formValidator = new CompositeValidator();
        formValidator.addValidators(databaseValidator, hostValidator, portValidator, userValidator, folderValidator);

        applyPreferences();

        EasyBind.subscribe(connectionUrl, text -> DBMSConnectionUrl.parse(text).ifPresent(this::applyConnectionUrl));
    }

    private void applyConnectionUrl(DBMSConnectionUrl url) {
        selectedDBMSType.set(url.type());
        host.set(url.host());
        port.set(Integer.toString(url.port()));
        database.set(url.database());
        url.user().ifPresent(user::set);
        url.password().ifPresent(password::set);
        useSSL.set(url.useSSL());
        // Parameters without a dedicated field (e.g. sslmode=require) survive only in the custom JDBC URL
        expertMode.set(!url.query().isEmpty());
        jdbcUrl.set(url.toJdbcUrl());
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
                .setServerTimezone(serverTimezone.getValue())
                .setExpertMode(expertMode.getValue())
                .setJdbcUrl(jdbcUrl.getValue())
                .createDBMSConnectionProperties();

        return openSharedDatabase(connectionProperties);
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
            SharedDatabaseUIManager manager = new SharedDatabaseUIManager(
                    tabContainer,
                    dialogService,
                    preferences,
                    aiService,
                    stateManager,
                    entryTypesManager,
                    fileUpdateMonitor,
                    undoManager,
                    clipBoardManager,
                    taskExecutor);
            LibraryTab libraryTab = manager.openNewSharedDatabaseTab(connectionProperties);
            setPreferences();

            if (!folder.getValue().isEmpty() && autosave.get()) {
                try {
                    new SaveDatabaseAction(
                            libraryTab,
                            dialogService,
                            preferences,
                            entryTypesManager,
                            stateManager,
                            journalAbbreviationRepository
                    ).saveAs(Path.of(folder.getValue()));
                } catch (Throwable e) {
                    LOGGER.error("Error while saving the database", e);
                }
            }

            return true;
        } catch (SQLException | InvalidDBMSConnectionPropertiesException exception) {
            dialogService.showErrorDialogAndWait(Localization.lang("Connection error"), exception);
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

            result.filter(btn -> btn.equals(openHelp)).ifPresent(btn -> new HelpAction(HelpFile.SQL_DATABASE_MIGRATION, dialogService, preferences.getExternalApplicationsPreferences()).execute());
            result.filter(ButtonType.OK::equals).ifPresent(btn -> openSharedDatabase(connectionProperties));
        }
        loading.set(false);
        return false;
    }

    private void setPreferences() {
        sharedDatabasePreferences.setType(selectedDBMSType.getValue().toString());
        sharedDatabasePreferences.setHost(host.getValue());
        sharedDatabasePreferences.setPort(port.getValue());
        sharedDatabasePreferences.setName(database.getValue());
        sharedDatabasePreferences.setUser(user.getValue());
        sharedDatabasePreferences.setUseSSL(useSSL.getValue());
        sharedDatabasePreferences.setServerTimezone(serverTimezone.getValue());
        sharedDatabasePreferences.setExpertMode(expertMode.get());
        sharedDatabasePreferences.setJdbcUrl(jdbcUrl.getValue());

        if (rememberPassword.get()) {
            sharedDatabasePreferences.setPassword(password.getValue());
        } else {
            sharedDatabasePreferences.clearPassword();
        }

        sharedDatabasePreferences.setRememberPassword(rememberPassword.get());

        sharedDatabasePreferences.setFolder(folder.getValue());
        sharedDatabasePreferences.setAutosave(autosave.get());
    }

    /// Fetches possibly saved data and configures the control elements respectively.
    private void applyPreferences() {
        Optional<String> sharedDatabaseType = sharedDatabasePreferences.getType();
        Optional<String> sharedDatabaseHost = sharedDatabasePreferences.getHost();
        Optional<String> sharedDatabasePort = sharedDatabasePreferences.getPort();
        Optional<String> sharedDatabaseName = sharedDatabasePreferences.getName();
        Optional<String> sharedDatabaseUser = sharedDatabasePreferences.getUser();
        boolean sharedDatabaseRememberPassword = sharedDatabasePreferences.getRememberPassword();
        Optional<String> sharedDatabaseFolder = sharedDatabasePreferences.getFolder();
        boolean sharedDatabaseAutosave = sharedDatabasePreferences.getAutosave();

        if (sharedDatabaseType.isPresent()) {
            Optional<DBMSType> dbmsType = DBMSType.fromString(sharedDatabaseType.get());
            dbmsType.ifPresent(selectedDBMSType::set);
        }

        sharedDatabaseHost.ifPresent(host::set);
        sharedDatabasePort.ifPresent(port::set);
        sharedDatabaseName.ifPresent(database::set);
        sharedDatabaseUser.ifPresent(user::set);
        useSSL.setValue(sharedDatabasePreferences.isUseSSL());
        sharedDatabasePreferences.getServerTimezone().ifPresent(serverTimezone::set);
        expertMode.set(sharedDatabasePreferences.isUseExpertMode());
        sharedDatabasePreferences.getJdbcUrl().ifPresent(jdbcUrl::set);

        rememberPassword.set(sharedDatabaseRememberPassword && keyringAvailable);
        if (rememberPassword.get()) {
            sharedDatabasePreferences.getPassword().ifPresent(password::set);
        }

        sharedDatabaseFolder.ifPresent(folder::set);
        autosave.set(sharedDatabaseAutosave);
    }

    private boolean isSharedDatabaseAlreadyPresent(DBMSConnectionProperties connectionProperties) {
        List<LibraryTab> libraryTabs = tabContainer.getLibraryTabs();
        return libraryTabs.parallelStream().anyMatch(panel -> {
            BibDatabaseContext context = panel.getBibDatabaseContext();

            return (context.getLocation() == DatabaseLocation.SHARED) &&
                    connectionProperties.equals(context.getDBMSSynchronizer().getConnectionProperties());
        });
    }

    public void showSaveDbToFileDialog() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.BIBTEX_DB)
                .withDefaultExtension(StandardFileType.BIBTEX_DB)
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);
        exportPath.ifPresent(path -> folder.setValue(path.toString()));
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

    public boolean isKeyringAvailable() {
        return keyringAvailable;
    }

    public StringProperty folderProperty() {
        return folder;
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

    public ValidationStatus formValidation() {
        return formValidator.getValidationStatus();
    }

    public StringProperty serverTimezoneProperty() {
        return serverTimezone;
    }

    public BooleanProperty expertModeProperty() {
        return expertMode;
    }

    public StringProperty jdbcUrlProperty() {
        return jdbcUrl;
    }

    public StringProperty connectionUrlProperty() {
        return connectionUrl;
    }

    public ValidationStatus connectionUrlValidation() {
        return connectionUrlValidator.getValidationStatus();
    }
}
