package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.transform.TransformerException;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiles.FileDownloadTask;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.linkedfile.LinkedFileEditDialogView;
import org.jabref.gui.mergeentries.MultiMergeEntriesView;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfContentImporter;
import org.jabref.logic.importer.fileformat.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.PdfVerbatimBibTextImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.io.FileNameUniqueness;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileHelper;
import org.jabref.model.util.OptionalUtil;
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
    private final PreferencesService preferences;
    private final LinkedFileHandler linkedFileHandler;
    private final ExternalFileTypes externalFileTypes;

    private final Validator fileExistsValidator;

    public LinkedFileViewModel(LinkedFile linkedFile,
                               BibEntry entry,
                               BibDatabaseContext databaseContext,
                               TaskExecutor taskExecutor,
                               DialogService dialogService,
                               PreferencesService preferences,
                               ExternalFileTypes externalFileTypes) {

        this.linkedFile = linkedFile;
        this.preferences = preferences;
        this.linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, preferences.getFilePreferences());
        this.databaseContext = databaseContext;
        this.entry = entry;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.externalFileTypes = externalFileTypes;

        fileExistsValidator = new FunctionBasedValidator<>(
                linkedFile.linkProperty(),
                link -> {
                    if (linkedFile.isOnlineLink()) {
                        return true;
                    } else {
                        Optional<Path> path = FileHelper.find(databaseContext, link, preferences.getFilePreferences());
                        return path.isPresent() && Files.exists(path.get());
                    }
                },
                ValidationMessage.warning(Localization.lang("Could not find file '%0'.", linkedFile.getLink())));

        downloadOngoing.bind(downloadProgress.greaterThanOrEqualTo(0).and(downloadProgress.lessThan(1)));
        isOfflinePdf.setValue(!linkedFile.isOnlineLink() && linkedFile.getFileType().equalsIgnoreCase("pdf"));
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
        return externalFileTypes.fromLinkedFile(linkedFile, false)
                                .map(ExternalFileType::getIcon)
                                .orElse(IconTheme.JabRefIcons.FILE);
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
            Optional<ExternalFileType> type = ExternalFileTypes.getInstance().fromLinkedFile(linkedFile, true);
            boolean successful = JabRefDesktop.openExternalFileAnyFormat(databaseContext, preferences, linkedFile.getLink(), type);
            if (!successful) {
                dialogService.showErrorDialogAndWait(Localization.lang("File not found"), Localization.lang("Could not find file '%0'.", linkedFile.getLink()));
            }
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file '%0'.", linkedFile.getLink()), e);
        }
    }

    public void openFolder() {
        try {
            if (!linkedFile.isOnlineLink()) {
                Optional<Path> resolvedPath = FileHelper.find(
                        databaseContext,
                        linkedFile.getLink(),
                        preferences.getFilePreferences());

                if (resolvedPath.isPresent()) {
                    JabRefDesktop.openFolderAndSelectFile(resolvedPath.get(), preferences);
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

        Optional<Path> file = linkedFile.findIn(databaseContext, preferences.getFilePreferences());
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
        Optional<Path> fileDir = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences());
        if (fileDir.isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Move file"), Localization.lang("File directory is not set or does not exist!"));
            return;
        }

        Optional<Path> file = linkedFile.findIn(databaseContext, preferences.getFilePreferences());
        if ((file.isPresent())) {
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
        Optional<Path> newDir = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences());

        Optional<Path> currentDir = linkedFile.findIn(databaseContext, preferences.getFilePreferences()).map(Path::getParent);

        BiPredicate<Path, Path> equality = (fileA, fileB) -> {
            try {
                return Files.isSameFile(fileA, fileB);
            } catch (IOException e) {
                return false;
            }
        };
        return OptionalUtil.equals(newDir, currentDir, equality);
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
        Optional<Path> file = linkedFile.findIn(databaseContext, preferences.getFilePreferences());

        if (file.isEmpty()) {
            LOGGER.warn("Could not find file " + linkedFile.getLink());
            return true;
        }

        ButtonType removeFromEntry = new ButtonType(Localization.lang("Remove from entry"), ButtonData.YES);
        ButtonType deleteFromEntry = new ButtonType(Localization.lang("Delete from disk"));
        Optional<ButtonType> buttonType = dialogService.showCustomButtonDialogAndWait(AlertType.INFORMATION,
                Localization.lang("Delete '%0'", file.get().toString()),
                Localization.lang("Delete the selected file permanently from disk, or just remove the file from the entry? Pressing Delete will delete the file permanently from disk."),
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
                    LOGGER.warn("File permission error while deleting: " + linkedFile, ex);
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
        });
    }

    public void writeMetadataToPdf() {
        BackgroundTask<Void> writeTask = BackgroundTask.wrap(() -> {
            Optional<Path> file = linkedFile.findIn(databaseContext, preferences.getFilePreferences());
            if (file.isEmpty()) {
                // TODO: Print error message
                // Localization.lang("PDF does not exist");
            } else {
                try {
                    XmpUtilWriter.writeXmp(file.get(), entry, databaseContext.getDatabase(), preferences.getXmpPreferences());

                    EmbeddedBibFilePdfExporter embeddedBibExporter = new EmbeddedBibFilePdfExporter(preferences.getGeneralPreferences().getDefaultBibDatabaseMode(), Globals.entryTypesManager, preferences.getFieldWriterPreferences());
                    embeddedBibExporter.exportToFileByPath(databaseContext, databaseContext.getDatabase(), preferences.getGeneralPreferences().getDefaultEncoding(), preferences.getFilePreferences(), file.get());
                } catch (IOException | TransformerException ex) {
                    // TODO: Print error message
                    // Localization.lang("Error while writing") + " '" + file.toString() + "': " + ex;
                }
            }
            return null;
        });

        // TODO: Show progress
        taskExecutor.execute(writeTask);
    }

    public void download() {
        if (!linkedFile.isOnlineLink()) {
            throw new UnsupportedOperationException("In order to download the file it has to be an online link");
        }
        try {
            Optional<Path> targetDirectory = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences());
            if (targetDirectory.isEmpty()) {
                dialogService.showErrorDialogAndWait(Localization.lang("Download file"), Localization.lang("File directory is not set or does not exist!"));
                return;
            }

            URLDownload urlDownload = new URLDownload(linkedFile.getLink());
            if (!checkSSLHandshake(urlDownload)) {
                return;
            }

            BackgroundTask<Path> downloadTask = prepareDownloadTask(targetDirectory.get(), urlDownload);
            downloadTask.onSuccess(destination -> {
                boolean isDuplicate = false;
                try {
                    isDuplicate = FileNameUniqueness.isDuplicatedFile(targetDirectory.get(), destination.getFileName(), dialogService);
                } catch (IOException e) {
                    LOGGER.error("FileNameUniqueness.isDuplicatedFile failed", e);
                    return;
                }

                if (!isDuplicate) {
                    LinkedFile newLinkedFile = LinkedFilesEditorViewModel.fromFile(destination, databaseContext.getFileDirectories(preferences.getFilePreferences()), externalFileTypes);
                    List<LinkedFile> linkedFiles = entry.getFiles();

                    entry.addLinkedFile(entry, linkedFile, newLinkedFile, linkedFiles);

                    // Notify in bar when the file type is HTML.
                    if (newLinkedFile.getFileType().equals(StandardExternalFileType.URL.getName())) {
                        dialogService.notify(Localization.lang("Downloaded website as an HTML file."));
                        LOGGER.debug("Downloaded website {} as an HTML file at {}", linkedFile.getLink(), destination);
                    }
                }
            });
            downloadProgress.bind(downloadTask.workDonePercentageProperty());
            downloadTask.titleProperty().set(Localization.lang("Downloading"));
            downloadTask.messageProperty().set(
                    Localization.lang("Fulltext for") + ": " + entry.getCitationKey().orElse(Localization.lang("New entry")));
            downloadTask.showToUser(true);
            taskExecutor.execute(downloadTask);
        } catch (MalformedURLException exception) {
            dialogService.showErrorDialogAndWait(Localization.lang("Invalid URL"), exception);
        }
    }

    public boolean checkSSLHandshake(URLDownload urlDownload) {
        try {
            urlDownload.canBeReached();
        } catch (kong.unirest.UnirestException ex) {
            if (ex.getCause() instanceof javax.net.ssl.SSLHandshakeException) {
                if (dialogService.showConfirmationDialogAndWait(Localization.lang("Download file"),
                        Localization.lang("Unable to find valid certification path to requested target(%0), download anyway?",
                                urlDownload.getSource().toString()))) {
                    return true;
                } else {
                    dialogService.notify(Localization.lang("Download operation canceled."));
                }
            }
            return false;
        }
        return true;
    }

    public BackgroundTask<Path> prepareDownloadTask(Path targetDirectory, URLDownload urlDownload) {
        SSLSocketFactory defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        BackgroundTask<Path> downloadTask = BackgroundTask
                .wrap(() -> {
                    Optional<ExternalFileType> suggestedType = inferFileType(urlDownload);
                    ExternalFileType externalFileType = suggestedType.orElse(StandardExternalFileType.PDF);
                    String suggestedTypeName = externalFileType.getName();
                    linkedFile.setFileType(suggestedTypeName);
                    String suggestedName = linkedFileHandler.getSuggestedFileName(externalFileType.getExtension());
                    String fulltextDir = FileUtil.createDirNameFromPattern(databaseContext.getDatabase(), entry, preferences.getFilePreferences().getFileDirectoryPattern());
                    suggestedName = FileNameUniqueness.getNonOverWritingFileName(targetDirectory.resolve(fulltextDir), suggestedName);
                    return targetDirectory.resolve(fulltextDir).resolve(suggestedName);
                })
                .then(destination -> new FileDownloadTask(urlDownload.getSource(), destination))
                .onFinished(() -> URLDownload.setSSLVerification(defaultSSLSocketFactory, defaultHostnameVerifier))
                .onFailure(exception -> dialogService.showErrorDialogAndWait("Download failed", exception));
        return downloadTask;
    }

    private Optional<ExternalFileType> inferFileType(URLDownload urlDownload) {
        Optional<ExternalFileType> suggestedType = inferFileTypeFromMimeType(urlDownload);

        // If we did not find a file type from the MIME type, try based on extension:
        if (suggestedType.isEmpty()) {
            suggestedType = inferFileTypeFromURL(urlDownload.getSource().toExternalForm());
        }
        return suggestedType;
    }

    private Optional<ExternalFileType> inferFileTypeFromMimeType(URLDownload urlDownload) {
        String mimeType = urlDownload.getMimeType();

        if (mimeType != null) {
            LOGGER.debug("MIME Type suggested: " + mimeType);
            return externalFileTypes.getExternalFileTypeByMimeType(mimeType);
        } else {
            return Optional.empty();
        }
    }

    private Optional<ExternalFileType> inferFileTypeFromURL(String url) {
        return URLUtil.getSuffix(url)
                      .flatMap(externalFileTypes::getExternalFileTypeByExt);
    }

    public LinkedFile getFile() {
        return linkedFile;
    }

    public ValidationStatus fileExistsValidationStatus() {
        return fileExistsValidator.getValidationStatus();
    }

    public void parsePdfMetadataAndShowMergeDialog() {
        linkedFile.findIn(databaseContext, preferences.getFilePreferences()).ifPresent(filePath -> {
            MultiMergeEntriesView dialog = new MultiMergeEntriesView(preferences, taskExecutor);
            dialog.addSource(Localization.lang("Entry"), entry);
            dialog.addSource(Localization.lang("Verbatim"), wrapImporterToSupplier(new PdfVerbatimBibTextImporter(preferences.getImportFormatPreferences()), filePath));
            dialog.addSource(Localization.lang("Embedded"), wrapImporterToSupplier(new PdfEmbeddedBibFileImporter(preferences.getImportFormatPreferences()), filePath));
            if (preferences.getImporterPreferences().isGrobidEnabled()) {
                dialog.addSource("Grobid", wrapImporterToSupplier(new PdfGrobidImporter(preferences.getImporterPreferences(), preferences.getImportFormatPreferences()), filePath));
            }
            dialog.addSource(Localization.lang("XMP metadata"), wrapImporterToSupplier(new PdfXmpImporter(preferences.getXmpPreferences()), filePath));
            dialog.addSource(Localization.lang("Content"), wrapImporterToSupplier(new PdfContentImporter(preferences.getImportFormatPreferences()), filePath));
            dialog.showAndWait().ifPresent(newEntry -> {
                databaseContext.getDatabase().removeEntry(entry);
                databaseContext.getDatabase().insertEntry(newEntry);
            });
        });
    }

    private Supplier<BibEntry> wrapImporterToSupplier(Importer importer, Path filePath) {
        return () -> {
            try {
                ParserResult parserResult = importer.importDatabase(filePath, preferences.getGeneralPreferences().getDefaultEncoding());
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
