package org.jabref.gui.preferences.linkedfiles;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

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
import org.jabref.gui.commonfxcontrols.CitationKeyPatternsPanelViewModel;
import org.jabref.gui.commonfxcontrols.FilenamePatternItemModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.filenameformatpatterns.GlobalFilenamePattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;

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
//    private final ListProperty<String> defaultFileNamePatternsProperty =
//            new SimpleListProperty<>(FXCollections.observableArrayList(FilePreferences.DEFAULT_FILENAME_PATTERNS));
    private final BooleanProperty fulltextIndex = new SimpleBooleanProperty();
//    private final StringProperty fileNamePatternProperty = new SimpleStringProperty();
    private final StringProperty fileDirectoryPatternProperty = new SimpleStringProperty();
    private final BooleanProperty confirmLinkedFileDeleteProperty = new SimpleBooleanProperty();
    private final BooleanProperty moveToTrashProperty = new SimpleBooleanProperty();

    private final Validator mainFileDirValidator;

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final AutoLinkPreferences autoLinkPreferences;


    // The list and the default properties are being overwritten by the bound properties of the tableView, but to
    // prevent an NPE on storing the preferences before lazy-loading of the setValues, they need to be initialized.
    private final ListProperty<FilenamePatternItemModel> patternListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<FilenamePatternItemModel> defaultKeyPatternProperty = new SimpleObjectProperty<>(
            new FilenamePatternItemModel(new CitationKeyPatternsPanelViewModel.DefaultEntryType(), ""));

    public LinkedFilesTabViewModel(DialogService dialogService, CliPreferences preferences) {
        this.dialogService = dialogService;
        this.filePreferences = preferences.getFilePreferences();
        this.autoLinkPreferences = preferences.getAutoLinkPreferences();

        mainFileDirValidator = new FunctionBasedValidator<>(
                mainFileDirectoryProperty,
                mainDirectoryPath -> {
                    ValidationMessage error = ValidationMessage.error(
                            Localization.lang("Main file directory '%0' not found.\nCheck the tab \"Linked files\".", mainDirectoryPath)
                    );
                    try {
                        Path path = Path.of(mainDirectoryPath);
                        if (!(Files.exists(path) && Files.isDirectory(path))) {
                            return error;
                        }
                    } catch (InvalidPathException ex) {
                        return error;
                    }
                    // main directory is valid
                    return null;
                }
        );
    }

    @Override
    public void setValues() {
        // External files preferences / Attached files preferences / File preferences
        mainFileDirectoryProperty.setValue(filePreferences.getMainFileDirectory().orElse(Path.of("")).toString());
        useMainFileDirectoryProperty.setValue(!filePreferences.shouldStoreFilesRelativeToBibFile());
        useBibLocationAsPrimaryProperty.setValue(filePreferences.shouldStoreFilesRelativeToBibFile());
        fulltextIndex.setValue(filePreferences.shouldFulltextIndexLinkedFiles());

        fileDirectoryPatternProperty.setValue(filePreferences.getFileDirectoryPattern());
        confirmLinkedFileDeleteProperty.setValue(filePreferences.confirmDeleteLinkedFile());
        moveToTrashProperty.setValue(filePreferences.moveToTrash());

        // Autolink preferences
        switch (autoLinkPreferences.getCitationKeyDependency()) {
            case START -> autolinkFileStartsBibtexProperty.setValue(true);
            case EXACT -> autolinkFileExactBibtexProperty.setValue(true);
            case REGEX -> autolinkUseRegexProperty.setValue(true);
        }

        autolinkRegexKeyProperty.setValue(autoLinkPreferences.getRegularExpression());
    }

    @Override
    public void storeSettings() {
        // External files preferences / Attached files preferences / File preferences
        filePreferences.setMainFileDirectory(mainFileDirectoryProperty.getValue());
        filePreferences.setStoreFilesRelativeToBibFile(useBibLocationAsPrimaryProperty.getValue());

//        filePreferences.setFileNamePattern(fileNamePatternProperty.getValue());
        GlobalFilenamePattern newKeyPattern =
                new GlobalFilenamePattern(filePreferences.getFileNamePatternObj().getDefaultValue());
        patternListProperty.forEach(item -> {
            String patternString = item.getPattern();
            if (!"default".equals(item.getEntryType().getName())) {
                if (!patternString.trim().isEmpty()) {
                    newKeyPattern.addFilenameFormatPattern(item.getEntryType(), patternString);
                }
            }
        });
        if (!defaultKeyPatternProperty.getValue().getPattern().trim().isEmpty()) {
            // we do not trim the value at the assignment to enable users to have spaces at the beginning and
            // at the end of the pattern
            newKeyPattern.setDefaultValue(defaultKeyPatternProperty.getValue().getPattern());
        }

        filePreferences.setFileDirectoryPattern(fileDirectoryPatternProperty.getValue());
        filePreferences.setFulltextIndexLinkedFiles(fulltextIndex.getValue());

        // Autolink preferences
        if (autolinkFileStartsBibtexProperty.getValue()) {
            autoLinkPreferences.setCitationKeyDependency(AutoLinkPreferences.CitationKeyDependency.START);
        } else if (autolinkFileExactBibtexProperty.getValue()) {
            autoLinkPreferences.setCitationKeyDependency(AutoLinkPreferences.CitationKeyDependency.EXACT);
        } else if (autolinkUseRegexProperty.getValue()) {
            autoLinkPreferences.setCitationKeyDependency(AutoLinkPreferences.CitationKeyDependency.REGEX);
        }

        autoLinkPreferences.setRegularExpression(autolinkRegexKeyProperty.getValue());
        filePreferences.confirmDeleteLinkedFile(confirmLinkedFileDeleteProperty.getValue());
        filePreferences.moveToTrash(moveToTrashProperty.getValue());
        filePreferences.setFileNamePattern(newKeyPattern);
    }

    ValidationStatus mainFileDirValidationStatus() {
        return mainFileDirValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        ValidationStatus validationStatus = mainFileDirValidationStatus();
        if (!validationStatus.isValid() && useMainFileDirectoryProperty().get()) {
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

    public BooleanProperty fulltextIndexProperty() {
        return fulltextIndex;
    }

//    public ListProperty<String> defaultFileNamePatternsProperty() {
//        return defaultFileNamePatternsProperty;
//    }
//
//    public StringProperty fileNamePatternProperty() {
//        return fileNamePatternProperty;
//    }
    public ListProperty<FilenamePatternItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<FilenamePatternItemModel> defaultKeyPatternProperty() {
        return defaultKeyPatternProperty;
    }




    public StringProperty fileDirectoryPatternProperty() {
        return fileDirectoryPatternProperty;
    }

    public BooleanProperty useMainFileDirectoryProperty() {
        return useMainFileDirectoryProperty;
    }

    public BooleanProperty confirmLinkedFileDeleteProperty() {
        return this.confirmLinkedFileDeleteProperty;
    }

    public BooleanProperty moveToTrashProperty() {
        return this.moveToTrashProperty;
    }
}

