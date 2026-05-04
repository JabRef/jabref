package org.jabref.gui.preferences.general;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import org.jabref.gui.StateManager;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.remote.CLIMessageHandler;
import org.jabref.gui.theme.StyleSheet;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemePreset;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.http.manager.HttpServerManager;
import org.jabref.languageserver.controller.LanguageServerController;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.RemoteUtil;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseMode;

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

    private final ReadOnlyListProperty<ThemePreset> themesListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(ThemePreset.values()));
    private final ObjectProperty<ThemePreset> selectedThemeProperty = new SimpleObjectProperty<>();

    private final ReadOnlyListProperty<ThemeColorScheme> colorSchemeListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(ThemeColorScheme.values()));
    private final ObjectProperty<ThemeColorScheme> selectedThemeColorSchemeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty customThemeEnabled = new SimpleBooleanProperty(false);
    private final StringProperty customPathToThemeProperty = new SimpleStringProperty("");

    private final BooleanProperty fontOverrideProperty = new SimpleBooleanProperty();
    private final StringProperty fontSizeProperty = new SimpleStringProperty();

    private final BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private final BooleanProperty showAdvancedHintsProperty = new SimpleBooleanProperty();
    private final BooleanProperty confirmDeleteProperty = new SimpleBooleanProperty();
    private final BooleanProperty shouldAskForIncludingCrossReferencesProperty = new SimpleBooleanProperty();
    private final BooleanProperty hideTabBarProperty = new SimpleBooleanProperty();
    private final BooleanProperty donationNeverShowProperty = new SimpleBooleanProperty();

    private final ListProperty<BibDatabaseMode> bibliographyModeListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();
    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final BooleanProperty createBackupProperty = new SimpleBooleanProperty();
    private final StringProperty backupDirectoryProperty = new SimpleStringProperty("");

    private final BooleanProperty usePostgresSearchProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final WorkspacePreferences workspacePreferences;
    private final LibraryPreferences libraryPreferences;
    private final FilePreferences filePreferences;
    private final SearchPreferences searchPreferences;
    private final RemotePreferences remotePreferences;
    private final HttpServerManager httpServerManager;
    private final LanguageServerController languageServerController;
    private final UiMessageHandler uiMessageHandler;
    private final RemoteListenerServerManager remoteListenerServerManager;
    private final StateManager stateManager;

    private final Validator fontSizeValidator;
    private final Validator themeValidator;
    private final Validator themeColorSchemeValidator;

    private final List<String> restartWarning = new ArrayList<>();
    private final BooleanProperty remoteServerProperty = new SimpleBooleanProperty();
    private final StringProperty remotePortProperty = new SimpleStringProperty("");
    private final Validator remotePortValidator;
    private final BooleanProperty enableHttpServerProperty = new SimpleBooleanProperty();
    private final StringProperty httpPortProperty = new SimpleStringProperty("");
    private final Validator httpPortValidator;
    private final Validator languageServerPortValidator;
    private final BooleanProperty enableLanguageServerProperty = new SimpleBooleanProperty();
    private final StringProperty languageServerPortProperty = new SimpleStringProperty("");
    private final TrustStoreManager trustStoreManager;

    public GeneralTabViewModel(DialogService dialogService,
                               GuiPreferences preferences,
                               HttpServerManager httpServerManager,
                               LanguageServerController languageServerController,
                               UiMessageHandler uiMessageHandler,
                               RemoteListenerServerManager remoteListenerServerManager,
                               StateManager stateManager) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.workspacePreferences = preferences.getWorkspacePreferences();
        this.libraryPreferences = preferences.getLibraryPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.searchPreferences = preferences.getSearchPreferences();
        this.remotePreferences = preferences.getRemotePreferences();
        this.httpServerManager = httpServerManager;
        this.languageServerController = languageServerController;
        this.uiMessageHandler = uiMessageHandler;
        this.remoteListenerServerManager = remoteListenerServerManager;
        this.stateManager = stateManager;

        fontSizeValidator = new FunctionBasedValidator<>(
                fontSizeProperty,
                _ -> {
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

        themeValidator = new FunctionBasedValidator<>(
                selectedThemeProperty,
                Objects::nonNull,
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("General"),
                        Localization.lang("Appearance"),
                        Localization.lang("Please set a theme."))));

        themeColorSchemeValidator = new FunctionBasedValidator<>(
                selectedThemeColorSchemeProperty,
                Objects::nonNull,
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("General"),
                        Localization.lang("Appearance"),
                        Localization.lang("Please set a color scheme."))));

        remotePortValidator = new FunctionBasedValidator<>(
                remotePortProperty,
                RemoteUtil::isStringUserPort,
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("Network"),
                        Localization.lang("Remote operation"),
                        Localization.lang("You must enter an integer value in the interval 1025-65535"))));

        httpPortValidator = new FunctionBasedValidator<>(
                httpPortProperty,
                RemoteUtil::isStringUserPort,
                ValidationMessage.error("%s".formatted(Localization.lang("You must enter an integer value in the interval 1025-65535"))));

        languageServerPortValidator = new FunctionBasedValidator<>(
                languageServerPortProperty,
                RemoteUtil::isStringUserPort,
                ValidationMessage.error(Localization.lang("You must enter an integer value in the interval 1025-65535")));

        this.trustStoreManager = new TrustStoreManager(Path.of(preferences.getSSLPreferences().getTruststorePath()));
    }

    public ValidationStatus remotePortValidationStatus() {
        return remotePortValidator.getValidationStatus();
    }

    public ValidationStatus httpPortValidationStatus() {
        return httpPortValidator.getValidationStatus();
    }

    public ValidationStatus languageServerPortValidationStatus() {
        return languageServerPortValidator.getValidationStatus();
    }

    @Override
    public void setValues() {
        selectedLanguageProperty.setValue(workspacePreferences.getLanguage());

        selectedThemeProperty.setValue(workspacePreferences.getTheme());
        selectedThemeColorSchemeProperty.setValue(workspacePreferences.getColorScheme());
        customThemeEnabled.setValue(workspacePreferences.getCustomTheme().isPresent());
        customPathToThemeProperty.setValue(workspacePreferences.getCustomTheme().map(StyleSheet::getName).orElse(""));

        fontOverrideProperty.setValue(workspacePreferences.shouldOverrideDefaultFontSize());
        fontSizeProperty.setValue(String.valueOf(workspacePreferences.getMainFontSize()));

        openLastStartupProperty.setValue(workspacePreferences.shouldOpenLastEdited());
        showAdvancedHintsProperty.setValue(workspacePreferences.shouldShowAdvancedHints());

        confirmDeleteProperty.setValue(workspacePreferences.shouldConfirmDelete());
        shouldAskForIncludingCrossReferencesProperty.setValue(preferences.getCopyToPreferences().getShouldAskForIncludingCrossReferences());
        hideTabBarProperty.setValue(workspacePreferences.shouldHideTabBar());
        donationNeverShowProperty.setValue(preferences.getDonationPreferences().isNeverShowAgain());

        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(libraryPreferences.getDefaultBibDatabaseMode());

        alwaysReformatBibProperty.setValue(libraryPreferences.shouldAlwaysReformatOnSave());
        autosaveLocalLibraries.setValue(libraryPreferences.shouldAutoSave());

        createBackupProperty.setValue(filePreferences.shouldCreateBackup());
        backupDirectoryProperty.setValue(filePreferences.getBackupDirectory().toString());

        usePostgresSearchProperty.setValue(searchPreferences.shouldUsePostgresSearch());

        remoteServerProperty.setValue(remotePreferences.shouldEnableRemoteServer());
        remotePortProperty.setValue(String.valueOf(remotePreferences.getRemoteServerPort()));

        enableHttpServerProperty.setValue(remotePreferences.shouldEnableHttpServer());
        httpPortProperty.setValue(String.valueOf(remotePreferences.getHttpServerPort()));

        enableLanguageServerProperty.setValue(remotePreferences.shouldEnableLanguageServer());
        languageServerPortProperty.setValue(String.valueOf(remotePreferences.getLanguageServerPort()));
    }

    @Override
    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != workspacePreferences.getLanguage()) {
            workspacePreferences.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language to %0", newLanguage.getDisplayName()));
        }

        workspacePreferences.setShouldOverrideDefaultFontSize(fontOverrideProperty.getValue());
        workspacePreferences.setMainFontSize(Integer.parseInt(fontSizeProperty.getValue()));

        workspacePreferences.setTheme(selectedThemeProperty.get());
        workspacePreferences.setColorScheme(selectedThemeColorSchemeProperty.get());

        String customTheme = customPathToThemeProperty.getValue();
        if (customThemeEnabled.get() && !StringUtil.isBlank(customTheme)) {
            workspacePreferences.setCustomTheme(StyleSheet.create(customTheme));
        } else {
            workspacePreferences.setCustomTheme(Optional.empty());
        }

        workspacePreferences.setOpenLastEdited(openLastStartupProperty.getValue());
        workspacePreferences.setShowAdvancedHints(showAdvancedHintsProperty.getValue());

        workspacePreferences.setConfirmDelete(confirmDeleteProperty.getValue());
        preferences.getCopyToPreferences().setShouldAskForIncludingCrossReferences(shouldAskForIncludingCrossReferencesProperty.getValue());
        workspacePreferences.setHideTabBar(confirmHideTabBarProperty().getValue());
        preferences.getDonationPreferences().setNeverShowAgain(donationNeverShowProperty.getValue());

        libraryPreferences.setDefaultBibDatabaseMode(selectedBiblatexModeProperty.getValue());

        libraryPreferences.setAlwaysReformatOnSave(alwaysReformatBibProperty.getValue());
        libraryPreferences.setAutoSave(autosaveLocalLibraries.getValue());

        filePreferences.createBackupProperty().setValue(createBackupProperty.getValue());
        filePreferences.backupDirectoryProperty().setValue(Path.of(backupDirectoryProperty.getValue()));

        searchPreferences.setUsePostgresSearch(usePostgresSearchProperty.getValue());

        getPortAsInt(remotePortProperty.getValue()).ifPresent(newPort -> {
            if (remotePreferences.isDifferentRemoteServerPort(newPort)) {
                remotePreferences.setRemoteServerPort(newPort);
            }
        });

        CLIMessageHandler messageHandler = new CLIMessageHandler(uiMessageHandler, preferences);
        // stop in all cases, because the port might have changed
        remoteListenerServerManager.stop();
        if (remoteServerProperty.getValue()) {
            remotePreferences.setEnableRemoteServer(true);
            remoteListenerServerManager.openAndStart(messageHandler, remotePreferences.getRemoteServerPort());
        } else {
            remotePreferences.setEnableRemoteServer(false);
            remoteListenerServerManager.stop();
        }

        getPortAsInt(httpPortProperty.getValue()).ifPresent(newPort -> {
            if (remotePreferences.isDifferentHttpServerPort(newPort)) {
                remotePreferences.setHttpServerPort(newPort);
            }
        });

        getPortAsInt(languageServerPortProperty.getValue()).ifPresent(newPort -> {
            if (remotePreferences.isDifferentLanguageServerPort(newPort)) {
                remotePreferences.setLanguageServerPort(newPort);
            }
        });

        // stop in all cases, because the port might have changed
        httpServerManager.stop();
        if (enableHttpServerProperty.getValue()) {
            remotePreferences.setEnableHttpServer(true);
            URI uri = remotePreferences.getHttpServerUri();
            httpServerManager.start(preferences, stateManager, uiMessageHandler, uri);
        } else {
            remotePreferences.setEnableHttpServer(false);
            httpServerManager.stop();
        }

        // stop in all cases, because the port might have changed (or other settings that can't be easily tracked https://github.com/JabRef/jabref/pull/13697#discussion_r2285997003)
        languageServerController.stop();
        if (enableLanguageServerProperty.getValue()) {
            remotePreferences.setEnableLanguageServer(true);
            languageServerController.start(messageHandler, remotePreferences.getLanguageServerPort());
        } else {
            remotePreferences.setEnableLanguageServer(false);
            languageServerController.stop();
        }

        trustStoreManager.flush();
    }

    public ValidationStatus fontSizeValidationStatus() {
        return fontSizeValidator.getValidationStatus();
    }

    public ValidationStatus themeValidationStatus() {
        return themeValidator.getValidationStatus();
    }

    public ValidationStatus themeColorSchemeValidationStatus() {
        return themeColorSchemeValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        CompositeValidator validator = new CompositeValidator();

        if (remoteServerProperty.getValue()) {
            validator.addValidators(remotePortValidator);
        }

        if (enableHttpServerProperty.getValue()) {
            validator.addValidators(httpPortValidator);
        }

        if (enableLanguageServerProperty.getValue()) {
            validator.addValidators(languageServerPortValidator);
        }

        if (fontOverrideProperty.getValue()) {
            validator.addValidators(fontSizeValidator);
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

    public ReadOnlyListProperty<ThemePreset> themesListProperty() {
        return this.themesListProperty;
    }

    public ObjectProperty<ThemePreset> selectedThemeProperty() {
        return selectedThemeProperty;
    }

    public ReadOnlyListProperty<ThemeColorScheme> colorSchemeListProperty() {
        return colorSchemeListProperty;
    }

    public ObjectProperty<ThemeColorScheme> selectedThemeColorSchemeProperty() {
        return selectedThemeColorSchemeProperty;
    }

    public StringProperty customPathToThemeProperty() {
        return customPathToThemeProperty;
    }

    public BooleanProperty customThemeEnabledProperty() {
        return customThemeEnabled;
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

    public BooleanProperty confirmDeleteProperty() {
        return this.confirmDeleteProperty;
    }

    public BooleanProperty shouldAskForIncludingCrossReferences() {
        return this.shouldAskForIncludingCrossReferencesProperty;
    }

    public BooleanProperty confirmHideTabBarProperty() {
        return this.hideTabBarProperty;
    }

    public BooleanProperty donationNeverShowProperty() {
        return this.donationNeverShowProperty;
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

    public BooleanProperty usePostgresSearchProperty() {
        return this.usePostgresSearchProperty;
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

    public BooleanProperty enableHttpServerProperty() {
        return enableHttpServerProperty;
    }

    public StringProperty httpPortProperty() {
        return httpPortProperty;
    }

    public BooleanProperty directHttpImportProperty() {
        return remotePreferences.directHttpImportProperty();
    }

    public BooleanProperty enableLanguageServerProperty() {
        return enableLanguageServerProperty;
    }

    public StringProperty languageServerPortProperty() {
        return languageServerPortProperty;
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
