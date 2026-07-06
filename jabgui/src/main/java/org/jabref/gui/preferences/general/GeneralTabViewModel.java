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
import org.jabref.gui.theme.Theme;
import org.jabref.gui.theme.ThemeTypes;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
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

import org.jfxcore.validation.property.ConstrainedObjectProperty;
import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.ReadOnlyConstrainedProperty;
import org.jfxcore.validation.property.SimpleConstrainedObjectProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;

public class GeneralTabViewModel implements PreferenceTabViewModel {

    protected static SpinnerValueFactory<Integer> fontSizeValueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(9, Integer.MAX_VALUE);

    private final ReadOnlyListProperty<Language> languagesListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(Language.getSorted()));
    private final ObjectProperty<Language> selectedLanguageProperty = new SimpleObjectProperty<>();

    private final ReadOnlyListProperty<ThemeTypes> themesListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(ThemeTypes.values()));
    private final ConstrainedObjectProperty<ThemeTypes, ValidationMessage> selectedThemeProperty = new SimpleConstrainedObjectProperty<>(
            (ThemeTypes) null,
            ValidationConstraints.predicate(
                    Objects::nonNull,
                    ValidationMessage.error("%s > %s %n %n %s".formatted(
                            Localization.lang("General"),
                            Localization.lang("Visual theme"),
                            Localization.lang("Please set a theme.")))));

    private final BooleanProperty themeSyncOsProperty = new SimpleBooleanProperty() {
        @Override
        protected void invalidated() {
            if (themeSyncOsProperty.get()) {
                selectedThemeProperty.set(null);
            }
        }
    };

    // init with empty string to avoid npe in accessing
    private final ConstrainedStringProperty<ValidationMessage> customPathToThemeProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.predicate(
                    input -> !StringUtil.isNullOrEmpty(input),
                    ValidationMessage.error("%s > %s %n %n %s".formatted(
                            Localization.lang("General"),
                            Localization.lang("Visual theme"),
                            Localization.lang("Please specify a css theme file.")))));

    private final BooleanProperty fontOverrideProperty = new SimpleBooleanProperty();
    private final ConstrainedStringProperty<ValidationMessage> fontSizeProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.predicate(
                    value -> {
                        try {
                            return Integer.parseInt(value) > 8;
                        } catch (NumberFormatException ex) {
                            return false;
                        }
                    },
                    ValidationMessage.error("%s > %s %n %n %s".formatted(
                            Localization.lang("General"),
                            Localization.lang("Font settings"),
                            Localization.lang("You must enter an integer value higher than 8.")))));

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

    private final List<String> restartWarning = new ArrayList<>();
    private final BooleanProperty remoteServerProperty = new SimpleBooleanProperty();
    private final ConstrainedStringProperty<ValidationMessage> remotePortProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.predicate(
                    RemoteUtil::isStringUserPort,
                    ValidationMessage.error("%s > %s %n %n %s".formatted(
                            Localization.lang("Network"),
                            Localization.lang("Remote operation"),
                            Localization.lang("You must enter an integer value in the interval 1025-65535")))));
    private final BooleanProperty enableHttpServerProperty = new SimpleBooleanProperty();
    private final ConstrainedStringProperty<ValidationMessage> httpPortProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.predicate(
                    RemoteUtil::isStringUserPort,
                    ValidationMessage.error("%s".formatted(Localization.lang("You must enter an integer value in the interval 1025-65535")))));
    private final BooleanProperty enableLanguageServerProperty = new SimpleBooleanProperty();
    private final ConstrainedStringProperty<ValidationMessage> languageServerPortProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.predicate(
                    RemoteUtil::isStringUserPort,
                    ValidationMessage.error(Localization.lang("You must enter an integer value in the interval 1025-65535"))));
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

        this.trustStoreManager = new TrustStoreManager(preferences.getSSLPreferences().getTruststorePath());
    }

    @Override
    public void setValues() {
        selectedLanguageProperty.setValue(workspacePreferences.getLanguage());

        switch (workspacePreferences.getTheme().getType()) {
            case LIGHT ->
                    selectedThemeProperty.setValue(ThemeTypes.LIGHT);
            case DARK ->
                    selectedThemeProperty.setValue(ThemeTypes.DARK);
            case CUSTOM -> {
                selectedThemeProperty.setValue(ThemeTypes.CUSTOM);
                customPathToThemeProperty.setValue(workspacePreferences.getTheme().getName());
            }
            case SYSTEM ->
                    selectedThemeProperty.setValue(null);
        }
        themeSyncOsProperty.setValue(workspacePreferences.shouldThemeSyncOs());

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

        boolean themeSyncOs = themeSyncOsProperty.getValue();
        workspacePreferences.setThemeSyncOs(themeSyncOs);

        if (themeSyncOs) {
            workspacePreferences.setTheme(Theme.system());
        } else {
            switch (selectedThemeProperty.get()) {
                case LIGHT ->
                        workspacePreferences.setTheme(Theme.light());
                case DARK ->
                        workspacePreferences.setTheme(Theme.dark());
                case CUSTOM ->
                        workspacePreferences.setTheme(Theme.custom(customPathToThemeProperty.getValue()));
            }
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

        filePreferences.setCreateBackup(createBackupProperty.getValue());
        filePreferences.setBackupDirectory(Path.of(backupDirectoryProperty.getValue()));

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

    @Override
    public boolean validateSettings() {
        List<ReadOnlyConstrainedProperty<?, ValidationMessage>> fieldsToCheck = new ArrayList<>();
        if (remoteServerProperty.getValue()) {
            fieldsToCheck.add(remotePortProperty);
        }
        if (enableHttpServerProperty.getValue()) {
            fieldsToCheck.add(httpPortProperty);
        }
        if (enableLanguageServerProperty.getValue()) {
            fieldsToCheck.add(languageServerPortProperty);
        }
        if (fontOverrideProperty.getValue()) {
            fieldsToCheck.add(fontSizeProperty);
        }
        if (selectedThemeProperty.getValue() == ThemeTypes.CUSTOM) {
            fieldsToCheck.add(customPathToThemeProperty);
        }
        if (!themeSyncOsProperty.get() && selectedThemeProperty.getValue() == null) {
            fieldsToCheck.add(selectedThemeProperty);
        }

        for (ReadOnlyConstrainedProperty<?, ValidationMessage> field : fieldsToCheck) {
            if (field.isInvalid()) {
                dialogService.showErrorDialogAndWait(field.getDiagnostics().invalidSubList().getFirst().message());
                return false;
            }
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

    public ConstrainedObjectProperty<ThemeTypes, ValidationMessage> selectedThemeProperty() {
        return this.selectedThemeProperty;
    }

    public BooleanProperty themeSyncOsProperty() {
        return this.themeSyncOsProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> customPathToThemeProperty() {
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

    public ConstrainedStringProperty<ValidationMessage> fontSizeProperty() {
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

    public ConstrainedStringProperty<ValidationMessage> remotePortProperty() {
        return remotePortProperty;
    }

    public BooleanProperty enableHttpServerProperty() {
        return enableHttpServerProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> httpPortProperty() {
        return httpPortProperty;
    }

    public BooleanProperty directHttpImportProperty() {
        return remotePreferences.directHttpImportProperty();
    }

    public BooleanProperty enableLanguageServerProperty() {
        return enableLanguageServerProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> languageServerPortProperty() {
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
