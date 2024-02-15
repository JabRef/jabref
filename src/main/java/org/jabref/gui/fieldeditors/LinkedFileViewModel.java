package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.linkedfile.DownloadLinkedFileAction;
import org.jabref.gui.linkedfile.LinkedFileEditDialogView;
import org.jabref.gui.mergeentries.MultiMergeEntriesView;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfContentImporter;
import org.jabref.logic.importer.fileformat.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.PdfVerbatimBibTextImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedFileViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileViewModel.class);

    private final LinkedFile linkedFile;
    private final BibDatabaseContext databaseContext;
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(-1);
    private final BooleanProperty downloadOngoing = new SimpleBooleanProperty(false);
    private final BooleanProperty isAutomaticallyFound = new SimpleBooleanProperty(false);
    private final BooleanProperty isOfflinePdf = new SimpleBooleanProperty(false);
    private final DialogService dialogService;
    private final BibEntry entry;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;
    private final LinkedFileHandler linkedFileHandler;

    private ObjectBinding<Node> linkedFileIconBinding;

    private final Validator fileExistsValidator;

    public LinkedFileViewModel(LinkedFile linkedFile,
                               BibEntry entry,
                               BibDatabaseContext databaseContext,
                               TaskExecutor taskExecutor,
                               DialogService dialogService,
                               PreferencesService preferencesService) {

        this.linkedFile = linkedFile;
        this.preferencesService = preferencesService;
        this.linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, preferencesService.getFilePreferences());
        this.databaseContext = databaseContext;
        this.entry = entry;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        fileExistsValidator = new FunctionBasedValidator<>(
                linkedFile.linkProperty(),
                link -> {
                    if (linkedFile.isOnlineLink()) {
                        return true;
                    } else {
                        Optional<Path> path = FileUtil.find(databaseContext, link, preferencesService.getFilePreferences());
                        return path.isPresent() && Files.exists(path.get());
                    }
                },
                ValidationMessage.warning(Localization.lang("Could not find file '%0'.", linkedFile.getLink())));

        downloadOngoing.bind(downloadProgress.greaterThanOrEqualTo(0).and(downloadProgress.lessThan(1)));
        isOfflinePdf.setValue(!linkedFile.isOnlineLink() && "pdf".equalsIgnoreCase(linkedFile.getFileType()));
    }

    public BooleanProperty isOfflinePdfProperty() {
        return isOfflinePdf;
    }

    public boolean isAutomaticallyFound() {
        return isAutomaticallyFound.get();
    }

    public BooleanProperty isAutomaticallyFoundProperty() {
        return isAutomaticallyFound;
    }

    public BooleanProperty downloadOngoingProperty() {
        return downloadOngoing;
    }

    public DoubleProperty downloadProgressProperty() {
        return downloadProgress;
    }

    public StringProperty linkProperty() {
        return linkedFile.linkProperty();
    }

    public StringProperty descriptionProperty() {
        return linkedFile.descriptionProperty();
    }

    public String getDescription() {
        return linkedFile.getDescription();
    }

    public String getDescriptionAndLink() {
        if (StringUtil.isBlank(linkedFile.getDescription())) {
            return linkedFile.getLink();
        } else {
            return linkedFile.getDescription() + " (" + linkedFile.getLink() + ")";
        }
    }

    public String getTruncatedDescriptionAndLink() {
        if (StringUtil.isBlank(linkedFile.getDescription())) {
            return ControlHelper.truncateString(linkedFile.getLink(), -1, "...",
                    ControlHelper.EllipsisPosition.CENTER);
        } else {
            return ControlHelper.truncateString(linkedFile.getDescription(), -1, "...",
                    ControlHelper.EllipsisPosition.CENTER) + " (" +
                    ControlHelper.truncateString(linkedFile.getLink(), -1, "...",
                    ControlHelper.EllipsisPosition.CENTER) + ")";
        }
    }

    public Optional<Path> findIn(List<Path> directories) {
        return linkedFile.findIn(directories);
    }

    public JabRefIcon getTypeIcon() {
        return ExternalFileTypes.getExternalFileTypeByLinkedFile(linkedFile, false, preferencesService.getFilePreferences())
                                .map(ExternalFileType::getIcon)
                                .orElse(IconTheme.JabRefIcons.FILE);
    }

    public ObjectBinding<Node> typeIconProperty() {
        if (linkedFileIconBinding == null) {
            linkedFileIconBinding = Bindings.createObjectBinding(() -> this.getTypeIcon().getGraphicNode(), linkedFile.fileTypeProperty());
        }

        return linkedFileIconBinding;
    }

    public void markAsAutomaticallyFound() {
        isAutomaticallyFound.setValue(true);
    }

    public void acceptAsLinked() {
        isAutomaticallyFound.setValue(false);
    }

    public Observable[] getObservables() {
        List<Observable> observables = new ArrayList<>(Arrays.asList(linkedFile.getObservables()));
        observables.add(downloadOngoing);
        observables.add(downloadProgress);
        observables.add(isAutomaticallyFound);
        return observables.toArray(new Observable[0]);
    }

    public void open() {
        try {
            Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByLinkedFile(linkedFile, true, preferencesService.getFilePreferences());
            boolean successful = JabRefDesktop.openExternalFileAnyFormat(databaseContext, preferencesService.getFilePreferences(), linkedFile.getLink(), type);
            if (!successful) {
                dialogService.showErrorDialogAndWait(Localization.lang("File not found"), Localization.lang("Could not find file '%0'.", linkedFile.getLink()));
            }
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file '%0'", linkedFile.getLink()), e);
        }
    }

    public void openFolder() {
        try {
            if (!linkedFile.isOnlineLink()) {
                Optional<Path> resolvedPath = FileUtil.find(
                        databaseContext,
                        linkedFile.getLink(),
                        preferencesService.getFilePreferences());
                if (resolvedPath.isPresent()) {
                    JabRefDesktop.openFolderAndSelectFile(resolvedPath.get(), preferencesService.getExternalApplicationsPreferences(), dialogService);
                } else {
                    dialogService.showErrorDialogAndWait(Localization.lang("File not found"));
                }
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Cannot open folder as the file is an online link."));
            }
        } catch (IOException ex) {
            LOGGER.debug("Cannot open folder", ex);
        }
    }

    public void renameToSuggestion() {
        renameFileToName(linkedFileHandler.getSuggestedFileName());
    }

    public void askForNameAndRename() {
        String oldFile = this.linkedFile.getLink();
        Path oldFilePath = Path.of(oldFile);
        Optional<String> askedFileName = dialogService.showInputDialogWithDefaultAndWait(
                Localization.lang("Rename file"),
                Localization.lang("New Filename"),
                oldFilePath.getFileName().toString());
        askedFileName.ifPresent(this::renameFileToName);
    }

    public void renameFileToName(String targetFileName) {
        if (linkedFile.isOnlineLink()) {
            // Cannot rename remote links
            return;
        }

        Optional<Path> file = linkedFile.findIn(databaseContext, preferencesService.getFilePreferences());
        if (file.isPresent()) {
            performRenameWithConflictCheck(targetFileName);
        } else {
            dialogService.showErrorDialogAndWait(Localization.lang("File not found"), Localization.lang("Could not find file '%0'.", linkedFile.getLink()));
        }
    }

    private void performRenameWithConflictCheck(String targetFileName) {
        Optional<Path> existingFile = linkedFileHandler.findExistingFile(linkedFile, entry, targetFileName);
        boolean overwriteFile = false;

        if (existingFile.isPresent()) {
            overwriteFile = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("File exists"),
                    Localization.lang("'%0' exists. Overwrite file?", targetFileName),
                    Localization.lang("Overwrite"));

            if (!overwriteFile) {
                return;
            }
        }

        try {
            linkedFileHandler.renameToName(targetFileName, overwriteFile);
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Rename failed"), Localization.lang("JabRef cannot access the file because it is being used by another process."));
        }
    }

    public void moveToDefaultDirectory() {
        if (linkedFile.isOnlineLink()) {
            // Cannot move remote links
            return;
        }

        // Get target folder
        Optional<Path> fileDir = databaseContext.getFirstExistingFileDir(preferencesService.getFilePreferences());
        if (fileDir.isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Move file"), Localization.lang("File directory is not set or does not exist!"));
            return;
        }

        Optional<Path> file = linkedFile.findIn(databaseContext, preferencesService.getFilePreferences());
        if (file.isPresent()) {
            // Found the linked file, so move it
            try {
                linkedFileHandler.moveToDefaultDirectory();
            } catch (IOException exception) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Move file"),
                        Localization.lang("Could not move file '%0'.", file.get().toString()),
                        exception);
            }
        } else {
            // File doesn't exist, so we can't move it.
            dialogService.showErrorDialogAndWait(Localization.lang("File not found"), Localization.lang("Could not find file '%0'.", linkedFile.getLink()));
        }
    }

    /**
     * Gets the filename for the current linked file and compares it to the new suggested filename.
     *
     * @return true if the suggested filename is same as current filename.
     */
    public boolean isGeneratedNameSameAsOriginal() {
        Path file = Path.of(this.linkedFile.getLink());
        String currentFileName = file.getFileName().toString();
        String suggestedFileName = this.linkedFileHandler.getSuggestedFileName();

        return currentFileName.equals(suggestedFileName);
    }

    /**
     * Compares suggested directory of current linkedFile with existing filepath directory.
     *
     * @return true if suggested filepath is same as existing filepath.
     */
    public boolean isGeneratedPathSameAsOriginal() {
        FilePreferences filePreferences = preferencesService.getFilePreferences();
        Optional<Path> baseDir = databaseContext.getFirstExistingFileDir(filePreferences);
        if (baseDir.isEmpty()) {
            // could not find default path
            return false;
        }

        // append File directory pattern if exits
        String targetDirectoryName = FileUtil.createDirNameFromPattern(
                databaseContext.getDatabase(),
                entry,
                filePreferences.getFileDirectoryPattern());

        Optional<Path> targetDir = baseDir.map(dir -> dir.resolve(targetDirectoryName));

        Optional<Path> currentDir = linkedFile.findIn(databaseContext, preferencesService.getFilePreferences()).map(Path::getParent);
        if (currentDir.isEmpty()) {
            // Could not find file
            return false;
        }

        BiPredicate<Path, Path> equality = (fileA, fileB) -> {
            try {
                return Files.isSameFile(fileA, fileB);
            } catch (IOException e) {
                return false;
            }
        };
        return OptionalUtil.equals(targetDir, currentDir, equality);
    }

    public void moveToDefaultDirectoryAndRename() {
        moveToDefaultDirectory();
        renameToSuggestion();
    }

    /**
     * Asks the user for confirmation that he really wants to the delete the file from disk (or just remove the link).
     *
     * @return true if the linked file should be removed afterwards from the entry (i.e because it was deleted
     * successfully, does not exist in the first place or the user choose to remove it)
     */
    public boolean delete() {
        Optional<Path> file = linkedFile.findIn(databaseContext, preferencesService.getFilePreferences());

        if (file.isEmpty()) {
            LOGGER.warn("Could not find file {}", linkedFile.getLink());
            return true;
        }

        ButtonType removeFromEntry = new ButtonType(Localization.lang("Remove from entry"), ButtonData.YES);
        ButtonType deleteFromEntry = new ButtonType(Localization.lang("Delete from disk"));
        Optional<ButtonType> buttonType = dialogService.showCustomButtonDialogAndWait(AlertType.INFORMATION,
                Localization.lang("Delete '%0'", file.get().getFileName().toString()),
                Localization.lang("Delete '%0' permanently from disk, or just remove the file from the entry? Pressing Delete will delete the file permanently from disk.", file.get().toString()),
                removeFromEntry, deleteFromEntry, ButtonType.CANCEL);

        if (buttonType.isPresent()) {
            if (buttonType.get().equals(removeFromEntry)) {
                return true;
            }

            if (buttonType.get().equals(deleteFromEntry)) {
                try {
                    Files.delete(file.get());
                    return true;
                } catch (IOException ex) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Cannot delete file"), Localization.lang("File permission error"));
                    LOGGER.warn("File permission error while deleting: {}", linkedFile, ex);
                }
            }
        }
        return false;
    }

    public void edit() {
        Optional<LinkedFile> editedFile = dialogService.showCustomDialogAndWait(new LinkedFileEditDialogView(this.linkedFile));
        editedFile.ifPresent(file -> {
            this.linkedFile.setLink(file.getLink());
            this.linkedFile.setDescription(file.getDescription());
            this.linkedFile.setFileType(file.getFileType());
            this.linkedFile.setSourceURL(file.getSourceUrl());
        });
    }

    public void redownload() {
        LOGGER.info("Redownloading file from " + linkedFile.getSourceUrl());
        if (linkedFile.getSourceUrl().isEmpty() || !LinkedFile.isOnlineLink(linkedFile.getSourceUrl())) {
            throw new UnsupportedOperationException("In order to download the file, the source url has to be an online link");
        }

        String fileName = Path.of(linkedFile.getLink()).getFileName().toString();

        DownloadLinkedFileAction downloadLinkedFileAction = new DownloadLinkedFileAction(
                databaseContext,
                entry,
                linkedFile,
                linkedFile.getSourceUrl(),
                dialogService,
                preferencesService.getFilePreferences(),
                taskExecutor,
                fileName);
        downloadProgress.bind(downloadLinkedFileAction.downloadProgress());
        downloadLinkedFileAction.execute();
    }

    public void download() {
        LOGGER.info("Downloading file from " + linkedFile.getSourceUrl());
        if (!linkedFile.isOnlineLink()) {
            throw new UnsupportedOperationException("In order to download the file it has to be an online link");
        }

        DownloadLinkedFileAction downloadLinkedFileAction = new DownloadLinkedFileAction(
                databaseContext,
                entry,
                linkedFile,
                linkedFile.getLink(),
                dialogService,
                preferencesService.getFilePreferences(),
                taskExecutor);
        downloadProgress.bind(downloadLinkedFileAction.downloadProgress());
        downloadLinkedFileAction.execute();
    }

    public LinkedFile getFile() {
        return linkedFile;
    }

    public ValidationStatus fileExistsValidationStatus() {
        return fileExistsValidator.getValidationStatus();
    }

    public void parsePdfMetadataAndShowMergeDialog() {
        linkedFile.findIn(databaseContext, preferencesService.getFilePreferences()).ifPresent(filePath -> {
            MultiMergeEntriesView dialog = new MultiMergeEntriesView(preferencesService, taskExecutor);
            dialog.setTitle(Localization.lang("Merge PDF metadata"));
            dialog.addSource(Localization.lang("Entry"), entry);
            dialog.addSource(Localization.lang("Verbatim"), wrapImporterToSupplier(new PdfVerbatimBibTextImporter(preferencesService.getImportFormatPreferences()), filePath));
            dialog.addSource(Localization.lang("Embedded"), wrapImporterToSupplier(new PdfEmbeddedBibFileImporter(preferencesService.getImportFormatPreferences()), filePath));
            if (preferencesService.getGrobidPreferences().isGrobidEnabled()) {
                dialog.addSource("Grobid", wrapImporterToSupplier(new PdfGrobidImporter(preferencesService.getImportFormatPreferences()), filePath));
            }
            dialog.addSource(Localization.lang("XMP metadata"), wrapImporterToSupplier(new PdfXmpImporter(preferencesService.getXmpPreferences()), filePath));
            dialog.addSource(Localization.lang("Content"), wrapImporterToSupplier(new PdfContentImporter(), filePath));
            dialogService.showCustomDialogAndWait(dialog).ifPresent(newEntry -> {
                databaseContext.getDatabase().removeEntry(entry);
                databaseContext.getDatabase().insertEntry(newEntry);
            });
        });
    }

    private Supplier<BibEntry> wrapImporterToSupplier(Importer importer, Path filePath) {
        return () -> {
            try {
                ParserResult parserResult = importer.importDatabase(filePath);
                if (parserResult.isInvalid() || parserResult.isEmpty() || !parserResult.getDatabase().hasEntries()) {
                    return null;
                }
                return parserResult.getDatabase().getEntries().get(0);
            } catch (IOException e) {
                return null;
            }
        };
    }
}
