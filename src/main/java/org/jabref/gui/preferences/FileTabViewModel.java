package org.jabref.gui.preferences;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.NewLineSeparator;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class FileTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private final StringProperty noWrapFilesProperty = new SimpleStringProperty("");
    private final BooleanProperty resolveStringsBibTexProperty = new SimpleBooleanProperty();
    private final BooleanProperty resolveStringsAllProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsExceptProperty = new SimpleStringProperty("");
    private final ListProperty<NewLineSeparator> newLineSeparatorListProperty = new SimpleListProperty<>();
    private final ObjectProperty<NewLineSeparator> selectedNewLineSeparatorProperty = new SimpleObjectProperty<>();
    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();

    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final StringProperty mainFileDirectoryProperty = new SimpleStringProperty("");
    private final BooleanProperty useBibLocationAsPrimaryProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkFileStartsBibtexProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkFileExactBibtexProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkUseRegexProperty = new SimpleBooleanProperty();
    private final StringProperty autolinkRegexKeyProperty = new SimpleStringProperty("");
    private final BooleanProperty searchFilesOnOpenProperty = new SimpleBooleanProperty();
    private final BooleanProperty openBrowseOnCreateProperty = new SimpleBooleanProperty();
    private final ListProperty<String> defaultFileNamePatternsProperty =
            new SimpleListProperty<>(FXCollections.observableArrayList(FilePreferences.DEFAULT_FILENAME_PATTERNS));
    private final StringProperty fileNamePatternProperty = new SimpleStringProperty();
    private final StringProperty fileDirectoryPatternProperty = new SimpleStringProperty();

    private final Validator mainFileDirValidator;

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final FilePreferences initialFilePreferences;
    private final AutoLinkPreferences initialAutoLinkPreferences;

    public FileTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialFilePreferences = preferences.getFilePreferences();
        this.initialAutoLinkPreferences = preferences.getAutoLinkPreferences();

        mainFileDirValidator = new FunctionBasedValidator<>(
                mainFileDirectoryProperty,
                input -> {
                    Path path = Path.of(mainFileDirectoryProperty.getValue());
                    return (Files.exists(path) && Files.isDirectory(path));
                },
                ValidationMessage.error(String.format("%s > %s > %s %n %n %s",
                        Localization.lang("File"),
                        Localization.lang("External file links"),
                        Localization.lang("Main file directory"),
                        Localization.lang("Directory not found")
                        )
                )
        );
    }

    @Override
    public void setValues() {
        openLastStartupProperty.setValue(preferences.getBoolean(JabRefPreferences.OPEN_LAST_EDITED));
        noWrapFilesProperty.setValue(preferences.get(JabRefPreferences.NON_WRAPPABLE_FIELDS));
        resolveStringsAllProperty.setValue(preferences.getBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS)); // Flipped around
        resolveStringsBibTexProperty.setValue(!resolveStringsAllProperty.getValue());
        resolveStringsExceptProperty.setValue(preferences.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));
        newLineSeparatorListProperty.setValue(FXCollections.observableArrayList(NewLineSeparator.values()));
        selectedNewLineSeparatorProperty.setValue(preferences.getNewLineSeparator());
        alwaysReformatBibProperty.setValue(preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT));

        autosaveLocalLibraries.setValue(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE));

        // External files preferences / Attached files preferences / File preferences
        mainFileDirectoryProperty.setValue(initialFilePreferences.getFileDirectory().orElse(Path.of("")).toString());
        useBibLocationAsPrimaryProperty.setValue(initialFilePreferences.isBibLocationAsPrimary());
        searchFilesOnOpenProperty.setValue(initialFilePreferences.shouldSearchFilesOnOpen());
        openBrowseOnCreateProperty.setValue(initialFilePreferences.shouldOpenBrowseOnCreate());
        fileNamePatternProperty.setValue(initialFilePreferences.getFileNamePattern());
        fileDirectoryPatternProperty.setValue(initialFilePreferences.getFileDirectoryPattern());

        // Autolink preferences
        switch (initialAutoLinkPreferences.getCitationKeyDependency()) {
            default:
            case START:
                autolinkFileStartsBibtexProperty.setValue(true);
                break;
            case EXACT:
                autolinkFileExactBibtexProperty.setValue(true);
                break;
            case REGEX:
                autolinkUseRegexProperty.setValue(true);
                break;
        }
        autolinkRegexKeyProperty.setValue(initialAutoLinkPreferences.getRegularExpression());
    }

    @Override
    public void storeSettings() {
        // -> Export preferences
        preferences.putBoolean(JabRefPreferences.OPEN_LAST_EDITED, openLastStartupProperty.getValue());
        if (!noWrapFilesProperty.getValue().trim().equals(preferences.get(JabRefPreferences.NON_WRAPPABLE_FIELDS))) {
            preferences.put(JabRefPreferences.NON_WRAPPABLE_FIELDS, noWrapFilesProperty.getValue());
        }
        preferences.putBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS, resolveStringsAllProperty.getValue());
        preferences.put(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR, resolveStringsExceptProperty.getValue().trim());
        resolveStringsExceptProperty.setValue(preferences.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));
        preferences.storeNewLineSeparator(selectedNewLineSeparatorProperty.getValue());
        preferences.putBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT, alwaysReformatBibProperty.getValue());

        // Autosave
        preferences.putBoolean(JabRefPreferences.LOCAL_AUTO_SAVE, autosaveLocalLibraries.getValue());

        // External files preferences / Attached files preferences / File preferences
        preferences.storeFilePreferences(new FilePreferences(
                initialFilePreferences.getUser(),
                mainFileDirectoryProperty.getValue(),
                useBibLocationAsPrimaryProperty.getValue(),
                fileNamePatternProperty.getValue(),
                fileDirectoryPatternProperty.getValue(),
                initialFilePreferences.shouldDownloadLinkedFiles(), // set in ImportEntriesViewModel
                searchFilesOnOpenProperty.getValue(),
                openBrowseOnCreateProperty.getValue()));

        // Autolink preferences
        AutoLinkPreferences.CitationKeyDependency citationKeyDependency = AutoLinkPreferences.CitationKeyDependency.START;
        if (autolinkFileExactBibtexProperty.getValue()) {
            citationKeyDependency = AutoLinkPreferences.CitationKeyDependency.EXACT;
        } else if (autolinkUseRegexProperty.getValue()) {
            citationKeyDependency = AutoLinkPreferences.CitationKeyDependency.REGEX;
        }

        preferences.storeAutoLinkPreferences(new AutoLinkPreferences(
                citationKeyDependency,
                autolinkRegexKeyProperty.getValue(),
                preferences.getKeywordDelimiter()));
    }

    ValidationStatus mainFileDirValidationStatus() {
        return mainFileDirValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        ValidationStatus validationStatus = mainFileDirValidationStatus();
        if (!validationStatus.isValid()) {
            validationStatus.getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }

    public void mainFileDirBrowse() {
        DirectoryDialogConfiguration dirDialogConfiguration =
                new DirectoryDialogConfiguration.Builder().withInitialDirectory(Path.of(mainFileDirectoryProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration)
                     .ifPresent(f -> mainFileDirectoryProperty.setValue(f.toString()));
    }

    // General

    public BooleanProperty openLastStartupProperty() {
        return openLastStartupProperty;
    }

    public StringProperty noWrapFilesProperty() {
        return noWrapFilesProperty;
    }

    public BooleanProperty resolveStringsBibTexProperty() {
        return resolveStringsBibTexProperty;
    }

    public BooleanProperty resolveStringsAllProperty() {
        return resolveStringsAllProperty;
    }

    public StringProperty resolvStringsExceptProperty() {
        return resolveStringsExceptProperty;
    }

    public ListProperty<NewLineSeparator> newLineSeparatorListProperty() {
        return newLineSeparatorListProperty;
    }

    public ObjectProperty<NewLineSeparator> selectedNewLineSeparatorProperty() {
        return selectedNewLineSeparatorProperty;
    }

    public BooleanProperty alwaysReformatBibProperty() {
        return alwaysReformatBibProperty;
    }

    // Autosave
    public BooleanProperty autosaveLocalLibrariesProperty() {
        return autosaveLocalLibraries;
    }

    // External file links

    public StringProperty mainFileDirectoryProperty() {
        return mainFileDirectoryProperty;
    }

    public BooleanProperty useBibLocationAsPrimaryProperty() {
        return useBibLocationAsPrimaryProperty;
    }

    public BooleanProperty autolinkFileStartsBibtexProperty() {
        return autolinkFileStartsBibtexProperty;
    }

    public BooleanProperty autolinkFileExactBibtexProperty() {
        return autolinkFileExactBibtexProperty;
    }

    public BooleanProperty autolinkUseRegexProperty() {
        return autolinkUseRegexProperty;
    }

    public StringProperty autolinkRegexKeyProperty() {
        return autolinkRegexKeyProperty;
    }

    public BooleanProperty searchFilesOnOpenProperty() {
        return searchFilesOnOpenProperty;
    }

    public BooleanProperty openBrowseOnCreateProperty() {
        return openBrowseOnCreateProperty;
    }

    public ListProperty<String> defaultFileNamePatternsProperty() {
        return defaultFileNamePatternsProperty;
    }

    public StringProperty fileNamePatternProperty() {
        return fileNamePatternProperty;
    }

    public StringProperty fileDirectoryPatternProperty() {
        return fileDirectoryPatternProperty;
    }
}

