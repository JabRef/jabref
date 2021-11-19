package org.jabref.gui.preferences.linkedfiles;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class LinkedFilesTabViewModel implements PreferenceTabViewModel {

    private final StringProperty mainFileDirectoryProperty = new SimpleStringProperty("");
    private final BooleanProperty useMainFileDirectoryProperty = new SimpleBooleanProperty();
    private final BooleanProperty useBibLocationAsPrimaryProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkFileStartsBibtexProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkFileExactBibtexProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkUseRegexProperty = new SimpleBooleanProperty();
    private final StringProperty autolinkRegexKeyProperty = new SimpleStringProperty("");
    private final ListProperty<String> defaultFileNamePatternsProperty =
            new SimpleListProperty<>(FXCollections.observableArrayList(FilePreferences.DEFAULT_FILENAME_PATTERNS));
    private final StringProperty fileNamePatternProperty = new SimpleStringProperty();
    private final StringProperty fileDirectoryPatternProperty = new SimpleStringProperty();

    private final Validator mainFileDirValidator;

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final FilePreferences filePreferences;
    private final AutoLinkPreferences initialAutoLinkPreferences;

    public LinkedFilesTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.filePreferences = preferences.getFilePreferences();
        this.initialAutoLinkPreferences = preferences.getAutoLinkPreferences();

        mainFileDirValidator = new FunctionBasedValidator<>(
                mainFileDirectoryProperty,
                input -> {
                    try {
                        Path path = Path.of(mainFileDirectoryProperty.getValue());
                        return (Files.exists(path) && Files.isDirectory(path));
                    } catch (InvalidPathException ex) {
                        return false;
                    }
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
        // External files preferences / Attached files preferences / File preferences
        mainFileDirectoryProperty.setValue(filePreferences.getFileDirectory().orElse(Path.of("")).toString());
        useMainFileDirectoryProperty.setValue(!filePreferences.shouldStoreFilesRelativeToBibFile());
        useBibLocationAsPrimaryProperty.setValue(filePreferences.shouldStoreFilesRelativeToBibFile());
        fileNamePatternProperty.setValue(filePreferences.getFileNamePattern());
        fileDirectoryPatternProperty.setValue(filePreferences.getFileDirectoryPattern());

        // Autolink preferences
        switch (initialAutoLinkPreferences.getCitationKeyDependency()) {
            case START -> autolinkFileStartsBibtexProperty.setValue(true);
            case EXACT -> autolinkFileExactBibtexProperty.setValue(true);
            case REGEX -> autolinkUseRegexProperty.setValue(true);
        }

        autolinkRegexKeyProperty.setValue(initialAutoLinkPreferences.getRegularExpression());
    }

    @Override
    public void storeSettings() {
        // External files preferences / Attached files preferences / File preferences
        filePreferences.setMainFileDirectory(mainFileDirectoryProperty.getValue());
        filePreferences.setStoreFilesRelativeToBibFile(useBibLocationAsPrimaryProperty.getValue());
        filePreferences.setFileNamePattern(fileNamePatternProperty.getValue());
        filePreferences.setFileDirectoryPattern(fileDirectoryPatternProperty.getValue());
        filePreferences.setDownloadLinkedFiles(filePreferences.shouldDownloadLinkedFiles()); // set in ImportEntriesViewModel

        // Autolink preferences
        if (autolinkFileStartsBibtexProperty.getValue()) {
            preferences.getAutoLinkPreferences().setCitationKeyDependency(AutoLinkPreferences.CitationKeyDependency.START);
        } else if (autolinkFileExactBibtexProperty.getValue()) {
            preferences.getAutoLinkPreferences().setCitationKeyDependency(AutoLinkPreferences.CitationKeyDependency.EXACT);
        } else if (autolinkUseRegexProperty.getValue()) {
            preferences.getAutoLinkPreferences().setCitationKeyDependency(AutoLinkPreferences.CitationKeyDependency.REGEX);
        }

        preferences.getAutoLinkPreferences().setRegularExpression(autolinkRegexKeyProperty.getValue());
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

    public void mainFileDirBrowse() {
        DirectoryDialogConfiguration dirDialogConfiguration =
                new DirectoryDialogConfiguration.Builder().withInitialDirectory(Path.of(mainFileDirectoryProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration)
                     .ifPresent(f -> mainFileDirectoryProperty.setValue(f.toString()));
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

    public ListProperty<String> defaultFileNamePatternsProperty() {
        return defaultFileNamePatternsProperty;
    }

    public StringProperty fileNamePatternProperty() {
        return fileNamePatternProperty;
    }

    public StringProperty fileDirectoryPatternProperty() {
        return fileDirectoryPatternProperty;
    }

    public BooleanProperty useMainFileDirectoryProperty() {
        return useMainFileDirectoryProperty;
    }
}

