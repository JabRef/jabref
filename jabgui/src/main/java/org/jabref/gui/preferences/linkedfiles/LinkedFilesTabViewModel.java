package org.jabref.gui.preferences.linkedfiles;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

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
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;

import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;

public class LinkedFilesTabViewModel implements PreferenceTabViewModel {

    private final ConstrainedStringProperty<ValidationMessage> mainFileDirectoryProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.function(mainDirectoryPath -> {
                ValidationMessage error = ValidationMessage.error(
                        Localization.lang("Main file directory '%0' not found.\nCheck the tab \"Linked files\".", mainDirectoryPath)
                );
                try {
                    Path path = Path.of(mainDirectoryPath);
                    if (!(Files.exists(path) && Files.isDirectory(path))) {
                        return Optional.of(error);
                    }
                } catch (InvalidPathException ex) {
                    return Optional.of(error);
                }
                // main directory is valid
                return Optional.empty();
            }));
    private final BooleanProperty useMainFileDirectoryProperty = new SimpleBooleanProperty();
    private final BooleanProperty useBibLocationAsPrimaryProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkFileStartsBibtexProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkFileExactBibtexProperty = new SimpleBooleanProperty();
    private final BooleanProperty autolinkUseRegexProperty = new SimpleBooleanProperty();
    private final StringProperty autolinkRegexKeyProperty = new SimpleStringProperty("");
    private final ListProperty<String> defaultFileNamePatternsProperty =
            new SimpleListProperty<>(FXCollections.observableArrayList(FilePreferences.DEFAULT_FILENAME_PATTERNS));
    private final BooleanProperty fulltextIndex = new SimpleBooleanProperty();
    private final BooleanProperty autoRenameFilesOnChangeProperty = new SimpleBooleanProperty();
    private final StringProperty fileNamePatternProperty = new SimpleStringProperty();
    private final StringProperty fileDirectoryPatternProperty = new SimpleStringProperty();
    private final BooleanProperty confirmLinkedFileDeleteProperty = new SimpleBooleanProperty();
    private final BooleanProperty moveToTrashProperty = new SimpleBooleanProperty();

    private final BooleanProperty adjustLinkedFilesOnTransferProperty = new SimpleBooleanProperty();
    private final BooleanProperty copyLinkedFilesOnTransferProperty = new SimpleBooleanProperty();
    private final BooleanProperty moveFilesOnTransferProperty = new SimpleBooleanProperty();

    private final BooleanProperty openFileExplorerInFilesDirectory = new SimpleBooleanProperty();
    private final BooleanProperty openFileExplorerInLastDirectory = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final AutoLinkPreferences autoLinkPreferences;

    public LinkedFilesTabViewModel(DialogService dialogService, CliPreferences preferences) {
        this.dialogService = dialogService;
        this.filePreferences = preferences.getFilePreferences();
        this.autoLinkPreferences = preferences.getAutoLinkPreferences();
    }

    @Override
    public void setValues() {
        // External files preferences / Attached files preferences / File preferences
        mainFileDirectoryProperty.setValue(filePreferences.getMainFileDirectory().orElse(Path.of("")).toString());
        useMainFileDirectoryProperty.setValue(!filePreferences.shouldStoreFilesRelativeToBibFile());
        useBibLocationAsPrimaryProperty.setValue(filePreferences.shouldStoreFilesRelativeToBibFile());
        fulltextIndex.setValue(filePreferences.shouldFulltextIndexLinkedFiles());
        autoRenameFilesOnChangeProperty.setValue(filePreferences.shouldAutoRenameFilesOnChange());
        fileNamePatternProperty.setValue(filePreferences.getFileNamePattern());
        fileDirectoryPatternProperty.setValue(filePreferences.getFileDirectoryPattern());
        confirmLinkedFileDeleteProperty.setValue(filePreferences.confirmDeleteLinkedFile());
        moveToTrashProperty.setValue(filePreferences.moveToTrash());
        openFileExplorerInFilesDirectory.setValue(filePreferences.shouldOpenFileExplorerInFileDirectory());
        openFileExplorerInLastDirectory.setValue(filePreferences.shouldOpenFileExplorerInLastUsedDirectory());
        adjustLinkedFilesOnTransferProperty.setValue(filePreferences.shouldAdjustFileLinksOnTransfer());
        copyLinkedFilesOnTransferProperty.setValue(filePreferences.shouldCopyLinkedFilesOnTransfer());
        moveFilesOnTransferProperty.setValue(filePreferences.shouldMoveLinkedFilesOnTransfer());

        // Autolink preferences
        switch (autoLinkPreferences.getCitationKeyDependency()) {
            case START ->
                    autolinkFileStartsBibtexProperty.setValue(true);
            case EXACT ->
                    autolinkFileExactBibtexProperty.setValue(true);
            case REGEX ->
                    autolinkUseRegexProperty.setValue(true);
        }

        autolinkRegexKeyProperty.setValue(autoLinkPreferences.getRegularExpression());
    }

    @Override
    public void storeSettings() {
        // External files preferences / Attached files preferences / File preferences
        if (mainFileDirectoryProperty.getValue().isEmpty()) {
            filePreferences.setMainFileDirectory(null);
        } else {
            filePreferences.setMainFileDirectory(Path.of(mainFileDirectoryProperty.getValue()));
        }
        filePreferences.setStoreFilesRelativeToBibFile(useBibLocationAsPrimaryProperty.getValue());
        filePreferences.setAutoRenameFilesOnChange(autoRenameFilesOnChangeProperty.getValue());
        filePreferences.setFileNamePattern(fileNamePatternProperty.getValue());
        filePreferences.setFileDirectoryPattern(fileDirectoryPatternProperty.getValue());
        filePreferences.setFulltextIndexLinkedFiles(fulltextIndex.getValue());
        filePreferences.setOpenFileExplorerInFileDirectory(openFileExplorerInFilesDirectory.getValue());
        filePreferences.setOpenFileExplorerInLastUsedDirectory(openFileExplorerInLastDirectory.getValue());

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
        filePreferences.setAdjustFileLinksOnTransfer(adjustLinkedFilesOnTransferProperty.getValue());
        filePreferences.setCopyLinkedFilesOnTransfer(copyLinkedFilesOnTransferProperty.getValue());
        filePreferences.setMoveLinkedFilesOnTransfer(moveFilesOnTransferProperty.getValue());
    }

    @Override
    public boolean validateSettings() {
        if (useMainFileDirectoryProperty().get() && mainFileDirectoryProperty.isInvalid()) {
            dialogService.showErrorDialogAndWait(mainFileDirectoryProperty.getDiagnostics().invalidSubList().getFirst().message());
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
    public ConstrainedStringProperty<ValidationMessage> mainFileDirectoryProperty() {
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

    public ListProperty<String> defaultFileNamePatternsProperty() {
        return defaultFileNamePatternsProperty;
    }

    public BooleanProperty autoRenameFilesOnChangeProperty() {
        return autoRenameFilesOnChangeProperty;
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

    public BooleanProperty confirmLinkedFileDeleteProperty() {
        return this.confirmLinkedFileDeleteProperty;
    }

    public BooleanProperty moveToTrashProperty() {
        return this.moveToTrashProperty;
    }

    public BooleanProperty adjustLinkedFilesOnTransferProperty() {
        return adjustLinkedFilesOnTransferProperty;
    }

    public BooleanProperty copyLinkedFilesOnTransferProperty() {
        return copyLinkedFilesOnTransferProperty;
    }

    public BooleanProperty moveFilesOnTransferProperty() {
        return moveFilesOnTransferProperty;
    }

    public BooleanProperty openFileExplorerInFilesDirectoryProperty() {
        return openFileExplorerInFilesDirectory;
    }

    public BooleanProperty openFileExplorerInLastDirectoryProperty() {
        return openFileExplorerInLastDirectory;
    }
}

