package org.jabref.gui.preferences.general;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.SpinnerValueFactory;

import org.jabref.gui.DialogService;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.UiMessageHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.remote.CLIMessageHandler;
import org.jabref.gui.theme.Theme;
import org.jabref.gui.theme.ThemeTypes;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.RemoteUtil;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class GeneralTabViewModel implements PreferenceTabViewModel {

    protected static SpinnerValueFactory<Integer> fontSizeValueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(9, Integer.MAX_VALUE);

    private final ReadOnlyListProperty<Language> languagesListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(Language.getSorted()));
    private final ObjectProperty<Language> selectedLanguageProperty = new SimpleObjectProperty<>();

    private final ReadOnlyListProperty<ThemeTypes> themesListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(ThemeTypes.values()));
    private final ObjectProperty<ThemeTypes> selectedThemeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty themeSyncOsProperty = new SimpleBooleanProperty();

    // init with empty string to avoid npe in accessing
    private final StringProperty customPathToThemeProperty = new SimpleStringProperty("");

    private final BooleanProperty fontOverrideProperty = new SimpleBooleanProperty();
    private final StringProperty fontSizeProperty = new SimpleStringProperty();

    private final BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private final BooleanProperty showAdvancedHintsProperty = new SimpleBooleanProperty();
    private final BooleanProperty inspectionWarningDuplicateProperty = new SimpleBooleanProperty();
    private final BooleanProperty confirmDeleteProperty = new SimpleBooleanProperty();
    private final BooleanProperty hideTabBarProperty = new SimpleBooleanProperty();

    private final ListProperty<BibDatabaseMode> bibliographyModeListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();
    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final BooleanProperty createBackupProperty = new SimpleBooleanProperty();
    private final StringProperty backupDirectoryProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final WorkspacePreferences workspacePreferences;
    private final LibraryPreferences libraryPreferences;
    private final FilePreferences filePreferences;
    private final RemotePreferences remotePreferences;

    private final Validator fontSizeValidator;
    private final Validator customPathToThemeValidator;

    private final List<String> restartWarning = new ArrayList<>();
    private final BooleanProperty remoteServerProperty = new SimpleBooleanProperty();
    private final StringProperty remotePortProperty = new SimpleStringProperty("");
    private final Validator remotePortValidator;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final TrustStoreManager trustStoreManager;

    public GeneralTabViewModel(DialogService dialogService, GuiPreferences preferences, FileUpdateMonitor fileUpdateMonitor, BibEntryTypesManager entryTypesManager) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.workspacePreferences = preferences.getWorkspacePreferences();
        this.libraryPreferences = preferences.getLibraryPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.remotePreferences = preferences.getRemotePreferences();
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;

        fontSizeValidator = new FunctionBasedValidator<>(
                fontSizeProperty,
                input -> {
                    try {
                        return Integer.parseInt(fontSizeProperty().getValue()) > 8;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                },
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("General"),
                        Localization.lang("Font settings"),
                        Localization.lang("You must enter an integer value higher than 8."))));

        customPathToThemeValidator = new FunctionBasedValidator<>(
                customPathToThemeProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("General"),
                        Localization.lang("Visual theme"),
                        Localization.lang("Please specify a css theme file."))));

        remotePortValidator = new FunctionBasedValidator<>(
                remotePortProperty,
                input -> {
                    try {
                        int portNumber = Integer.parseInt(remotePortProperty().getValue());
                        return RemoteUtil.isUserPort(portNumber);
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                },
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("Network"),
                        Localization.lang("Remote operation"),
                        Localization.lang("You must enter an integer value in the interval 1025-65535"))));

        this.trustStoreManager = new TrustStoreManager(Path.of(preferences.getSSLPreferences().getTruststorePath()));
    }

    public ValidationStatus remotePortValidationStatus() {
        return remotePortValidator.getValidationStatus();
    }

    @Override
    public void setValues() {
        selectedLanguageProperty.setValue(workspacePreferences.getLanguage());

        // The light theme is in fact the absence of any theme modifying 'base.css'. Another embedded theme like
        // 'dark.css', stored in the classpath, can be introduced in {@link org.jabref.gui.theme.Theme}.
        switch (workspacePreferences.getTheme().getType()) {
            case DEFAULT ->
                    selectedThemeProperty.setValue(ThemeTypes.LIGHT);
            case EMBEDDED ->
                    selectedThemeProperty.setValue(ThemeTypes.DARK);
            case CUSTOM -> {
                selectedThemeProperty.setValue(ThemeTypes.CUSTOM);
                customPathToThemeProperty.setValue(workspacePreferences.getTheme().getName());
            }
        }
        themeSyncOsProperty.setValue(workspacePreferences.shouldThemeSyncOs());

        fontOverrideProperty.setValue(workspacePreferences.shouldOverrideDefaultFontSize());
        fontSizeProperty.setValue(String.valueOf(workspacePreferences.getMainFontSize()));

        openLastStartupProperty.setValue(workspacePreferences.shouldOpenLastEdited());
        showAdvancedHintsProperty.setValue(workspacePreferences.shouldShowAdvancedHints());
        inspectionWarningDuplicateProperty.setValue(workspacePreferences.shouldWarnAboutDuplicatesInInspection());

        confirmDeleteProperty.setValue(workspacePreferences.shouldConfirmDelete());
        hideTabBarProperty.setValue(workspacePreferences.shouldHideTabBar());

        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(libraryPreferences.getDefaultBibDatabaseMode());

        alwaysReformatBibProperty.setValue(libraryPreferences.shouldAlwaysReformatOnSave());
        autosaveLocalLibraries.setValue(libraryPreferences.shouldAutoSave());

        createBackupProperty.setValue(filePreferences.shouldCreateBackup());
        backupDirectoryProperty.setValue(filePreferences.getBackupDirectory().toString());

        remoteServerProperty.setValue(remotePreferences.useRemoteServer());
        remotePortProperty.setValue(String.valueOf(remotePreferences.getPort()));
    }

    @Override
    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != workspacePreferences.getLanguage()) {
            workspacePreferences.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language") + ": " + newLanguage.getDisplayName());
        }

        workspacePreferences.setShouldOverrideDefaultFontSize(fontOverrideProperty.getValue());
        workspacePreferences.setMainFontSize(Integer.parseInt(fontSizeProperty.getValue()));

        switch (selectedThemeProperty.get()) {
            case LIGHT ->
                    workspacePreferences.setTheme(Theme.light());
            case DARK ->
                    workspacePreferences.setTheme(Theme.dark());
            case CUSTOM ->
                    workspacePreferences.setTheme(Theme.custom(customPathToThemeProperty.getValue()));
        }
        workspacePreferences.setThemeSyncOs(themeSyncOsProperty.getValue());

        workspacePreferences.setOpenLastEdited(openLastStartupProperty.getValue());
        workspacePreferences.setShowAdvancedHints(showAdvancedHintsProperty.getValue());
        workspacePreferences.setWarnAboutDuplicatesInInspection(inspectionWarningDuplicateProperty.getValue());

        workspacePreferences.setConfirmDelete(confirmDeleteProperty.getValue());
        workspacePreferences.setHideTabBar(confirmHideTabBarProperty().getValue());

        libraryPreferences.setDefaultBibDatabaseMode(selectedBiblatexModeProperty.getValue());

        libraryPreferences.setAlwaysReformatOnSave(alwaysReformatBibProperty.getValue());
        libraryPreferences.setAutoSave(autosaveLocalLibraries.getValue());

        filePreferences.createBackupProperty().setValue(createBackupProperty.getValue());
        filePreferences.backupDirectoryProperty().setValue(Path.of(backupDirectoryProperty.getValue()));

        getPortAsInt(remotePortProperty.getValue()).ifPresent(newPort -> {
            if (remotePreferences.isDifferentPort(newPort)) {
                remotePreferences.setPort(newPort);
            }
        });

        getPortAsInt(remotePortProperty.getValue()).ifPresent(newPort -> {
            if (remotePreferences.isDifferentPort(newPort)) {
                remotePreferences.setPort(newPort);
            }
        });

        UiMessageHandler uiMessageHandler = Injector.instantiateModelOrService(UiMessageHandler.class);
        RemoteListenerServerManager remoteListenerServerManager = Injector.instantiateModelOrService(RemoteListenerServerManager.class);
        remoteListenerServerManager.stop(); // stop in all cases, because the port might have changed

        if (remoteServerProperty.getValue()) {
            remotePreferences.setUseRemoteServer(true);
            remoteListenerServerManager.openAndStart(
                    new CLIMessageHandler(uiMessageHandler, preferences, fileUpdateMonitor, entryTypesManager),
                    remotePreferences.getPort());
        } else {
            remotePreferences.setUseRemoteServer(false);
        }
        trustStoreManager.flush();

        if (remoteServerProperty.getValue()) {
            remotePreferences.setUseRemoteServer(true);
            remoteListenerServerManager.openAndStart(
                    new CLIMessageHandler(uiMessageHandler, preferences, fileUpdateMonitor, entryTypesManager),
                    remotePreferences.getPort());
        } else {
            remotePreferences.setUseRemoteServer(false);
            remoteListenerServerManager.stop();
        }
        trustStoreManager.flush();
    }

    public ValidationStatus fontSizeValidationStatus() {
        return fontSizeValidator.getValidationStatus();
    }

    public ValidationStatus customPathToThemeValidationStatus() {
        return customPathToThemeValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        CompositeValidator validator = new CompositeValidator();

        if (remoteServerProperty.getValue()) {
            validator.addValidators(remotePortValidator);
        }

        if (fontOverrideProperty.getValue()) {
            validator.addValidators(fontSizeValidator);
        }

        if (selectedThemeProperty.getValue() == ThemeTypes.CUSTOM) {
            validator.addValidators(customPathToThemeValidator);
        }

        ValidationStatus validationStatus = validator.getValidationStatus();
        if (!validationStatus.isValid()) {
            validationStatus.getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarning;
    }

    public ReadOnlyListProperty<Language> languagesListProperty() {
        return this.languagesListProperty;
    }

    public ObjectProperty<Language> selectedLanguageProperty() {
        return this.selectedLanguageProperty;
    }

    public ReadOnlyListProperty<ThemeTypes> themesListProperty() {
        return this.themesListProperty;
    }

    public ObjectProperty<ThemeTypes> selectedThemeProperty() {
        return this.selectedThemeProperty;
    }

    public BooleanProperty themeSyncOsProperty() {
        return this.themeSyncOsProperty;
    }

    public StringProperty customPathToThemeProperty() {
        return customPathToThemeProperty;
    }

    public void importCSSFile() {
        String fileDir = customPathToThemeProperty.getValue().isEmpty() ? preferences.getInternalPreferences().getLastPreferencesExportPath().toString()
                : customPathToThemeProperty.getValue();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(fileDir).build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file ->
                customPathToThemeProperty.setValue(file.toAbsolutePath().toString()));
    }

    public BooleanProperty fontOverrideProperty() {
        return fontOverrideProperty;
    }

    public StringProperty fontSizeProperty() {
        return fontSizeProperty;
    }

    public BooleanProperty openLastStartupProperty() {
        return openLastStartupProperty;
    }

    public BooleanProperty showAdvancedHintsProperty() {
        return this.showAdvancedHintsProperty;
    }

    public BooleanProperty inspectionWarningDuplicateProperty() {
        return this.inspectionWarningDuplicateProperty;
    }

    public BooleanProperty confirmDeleteProperty() {
        return this.confirmDeleteProperty;
    }

    public BooleanProperty confirmHideTabBarProperty() {
        return this.hideTabBarProperty;
    }

    public ListProperty<BibDatabaseMode> biblatexModeListProperty() {
        return this.bibliographyModeListProperty;
    }

    public ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty() {
        return this.selectedBiblatexModeProperty;
    }

    public BooleanProperty alwaysReformatBibProperty() {
        return alwaysReformatBibProperty;
    }

    public BooleanProperty autosaveLocalLibrariesProperty() {
        return autosaveLocalLibraries;
    }

    public BooleanProperty createBackupProperty() {
        return this.createBackupProperty;
    }

    public StringProperty backupDirectoryProperty() {
        return this.backupDirectoryProperty;
    }

    public void backupFileDirBrowse() {
        DirectoryDialogConfiguration dirDialogConfiguration =
                new DirectoryDialogConfiguration.Builder().withInitialDirectory(Path.of(backupDirectoryProperty().getValue())).build();
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration)
                     .ifPresent(dir -> backupDirectoryProperty.setValue(dir.toString()));
    }

    public BooleanProperty remoteServerProperty() {
        return remoteServerProperty;
    }

    public StringProperty remotePortProperty() {
        return remotePortProperty;
    }

    public void openBrowser() {
        String url = "https://themes.jabref.org";
        try {
            NativeDesktop.openBrowser(url, preferences.getExternalApplicationsPreferences());
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not open website."), e);
        }
    }

    private Optional<Integer> getPortAsInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
