package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLHandshakeException;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.fieldeditors.URLUtil;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProgressInputStream;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileNameUniqueness;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.tobiasdiez.easybind.EasyBind;
import kong.unirest.core.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadLinkedFileAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadLinkedFileAction.class);

    private final DialogService dialogService;
    private final BibEntry entry;
    private final LinkedFile linkedFile;
    private final String suggestedName;
    private final String downloadUrl;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;
    private final boolean keepHtmlLink;

    private final BibDatabaseContext databaseContext;

    private final DoubleProperty downloadProgress = new SimpleDoubleProperty();
    private final LinkedFileHandler linkedFileHandler;

    public DownloadLinkedFileAction(BibDatabaseContext databaseContext,
                                    BibEntry entry,
                                    LinkedFile linkedFile,
                                    String downloadUrl,
                                    DialogService dialogService,
                                    ExternalApplicationsPreferences externalApplicationsPreferences,
                                    FilePreferences filePreferences,
                                    TaskExecutor taskExecutor,
                                    String suggestedName,
                                    boolean keepHtmlLink) {
        this.databaseContext = databaseContext;
        this.entry = entry;
        this.linkedFile = linkedFile;
        this.suggestedName = suggestedName;
        this.downloadUrl = downloadUrl;
        this.dialogService = dialogService;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
        this.keepHtmlLink = keepHtmlLink;

        this.linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);
    }

    /**
     * Downloads the given linked file to the first existing file directory. It keeps HTML files as URLs.
     */
    public DownloadLinkedFileAction(BibDatabaseContext databaseContext,
                                    BibEntry entry,
                                    LinkedFile linkedFile,
                                    String downloadUrl,
                                    DialogService dialogService,
                                    ExternalApplicationsPreferences externalApplicationsPreferences,
                                    FilePreferences filePreferences,
                                    TaskExecutor taskExecutor) {
        this(databaseContext,
                entry,
                linkedFile,
                downloadUrl,
                dialogService,
                externalApplicationsPreferences,
                filePreferences,
                taskExecutor,
                "",
                true);
    }

    @Override
    public void execute() {
        LOGGER.info("Downloading file from {}", downloadUrl);
        if (downloadUrl.isEmpty() || !LinkedFile.isOnlineLink(downloadUrl)) {
            throw new UnsupportedOperationException("In order to download the file, the url has to be an online link");
        }

        Optional<Path> targetDirectory = databaseContext.getFirstExistingFileDir(filePreferences);
        if (targetDirectory.isEmpty()) {
            LOGGER.warn("File directory not available while downloading {}. Storing as URL in file field.", downloadUrl);
            return;
        }

        try {
            URLDownload urlDownload = new URLDownload(downloadUrl);
            if (!checkSSLHandshake(urlDownload)) {
                return;
            }

            doDownload(targetDirectory.get(), urlDownload);
        } catch (MalformedURLException exception) {
            dialogService.showErrorDialogAndWait(Localization.lang("Invalid URL"), exception);
        }
    }

    private void doDownload(Path targetDirectory, URLDownload urlDownload) {
        BackgroundTask<Path> downloadTask = prepareDownloadTask(targetDirectory, urlDownload);
        downloadProgress.bind(downloadTask.workDonePercentageProperty());

        downloadTask.titleProperty().set(Localization.lang("Downloading"));
        entry.getCitationKey().ifPresentOrElse(
                citationkey -> downloadTask.messageProperty().set(Localization.lang("Fulltext for %0", citationkey)),
                () -> downloadTask.messageProperty().set(Localization.lang("Fulltext for a new entry")));
        downloadTask.showToUser(true);

        downloadTask.onFailure(ex -> onFailure(urlDownload, ex));
        downloadTask.onSuccess(destination -> onSuccess(targetDirectory, destination));

        taskExecutor.execute(downloadTask);
    }

    /**
     * @param targetDirectory The directory to store the file into. Is an absolute path.
     */
    private void onSuccess(Path targetDirectory, Path downloadedFile) {
        assert targetDirectory.isAbsolute();

        boolean isDuplicate;
        boolean isHtml;
        try {
            isDuplicate = FileNameUniqueness.isDuplicatedFile(targetDirectory, downloadedFile.getFileName(), dialogService::notify);
        } catch (IOException e) {
            LOGGER.error("FileNameUniqueness.isDuplicatedFile failed", e);
            return;
        }

        if (isDuplicate) {
            // We do not add duplicate files.
            // The downloaded file was deleted in {@link org.jabref.logic.util.io.FileNameUniqueness.isDuplicatedFile]}
            LOGGER.info("File {} already exists in target directory {}.", downloadedFile.getFileName(), targetDirectory);
            return;
        }

        // we need to call LinkedFileViewModel#fromFile, because we need to make the path relative to the configured directories
        LinkedFile newLinkedFile = LinkedFilesEditorViewModel.fromFile(
                downloadedFile,
                databaseContext.getFileDirectories(filePreferences),
                externalApplicationsPreferences);
        if (newLinkedFile.getDescription().isEmpty() && !linkedFile.getDescription().isEmpty()) {
            newLinkedFile.setDescription(linkedFile.getDescription());
        }
        if (linkedFile.getSourceUrl().isEmpty() && LinkedFile.isOnlineLink(linkedFile.getLink()) && filePreferences.shouldKeepDownloadUrl()) {
            newLinkedFile.setSourceURL(linkedFile.getLink());
        } else if (filePreferences.shouldKeepDownloadUrl()) {
            newLinkedFile.setSourceURL(linkedFile.getSourceUrl());
        }

        isHtml = newLinkedFile.getFileType().equals(StandardExternalFileType.URL.getName());
        if (isHtml) {
            if (this.keepHtmlLink) {
                dialogService.notify(Localization.lang("Download '%0' was a HTML file. Keeping URL.", downloadUrl));
            } else {
                dialogService.notify(Localization.lang("Download '%0' was a HTML file. Removed.", downloadUrl));
                List<LinkedFile> newFiles = new ArrayList<>(entry.getFiles());
                newFiles.remove(linkedFile);
                entry.setFiles(newFiles);
                try {
                    Files.delete(downloadedFile);
                } catch (IOException e) {
                    LOGGER.error("Could not delete downloaded file {}.", downloadedFile, e);
                }
            }
        } else {
            entry.replaceDownloadedFile(linkedFile.getLink(), newLinkedFile);
        }
    }

    private void onFailure(URLDownload urlDownload, Exception ex) {
        LOGGER.error("Error downloading from URL: {}", urlDownload, ex);
        if (ex instanceof FetcherException fetcherException) {
            dialogService.showErrorDialogAndWait(fetcherException);
        } else {
            String fetcherExceptionMessage = ex.getLocalizedMessage();
            String failedTitle = Localization.lang("Failed to download from URL");
            dialogService.showErrorDialogAndWait(failedTitle, Localization.lang("Please check the URL and try again.\nURL: %0\nDetails: %1", urlDownload.getSource(), fetcherExceptionMessage));
        }
    }

    private boolean checkSSLHandshake(URLDownload urlDownload) {
        try {
            urlDownload.canBeReached();
        } catch (UnirestException ex) {
            if (ex.getCause() instanceof SSLHandshakeException) {
                if (dialogService.showConfirmationDialogAndWait(Localization.lang("Download file"),
                        Localization.lang("Unable to find valid certification path to requested target(%0), download anyway?",
                                urlDownload.getSource().toString()))) {
                    return true;
                } else {
                    dialogService.notify(Localization.lang("Download operation canceled."));
                    return false;
                }
            } else {
                LOGGER.error("Error while checking if the file can be downloaded", ex);
                dialogService.notify(Localization.lang("Error downloading"));
                return false;
            }
        }
        return true;
    }

    private BackgroundTask<Path> prepareDownloadTask(Path targetDirectory, URLDownload urlDownload) {
        return BackgroundTask
                .wrap(() -> {
                    String suggestedName;
                    if (this.suggestedName.isEmpty()) {
                        Optional<ExternalFileType> suggestedType = inferFileType(urlDownload);
                        ExternalFileType externalFileType = suggestedType.orElse(StandardExternalFileType.PDF);
                        suggestedName = linkedFileHandler.getSuggestedFileName(externalFileType.getExtension());
                    } else {
                        suggestedName = this.suggestedName;
                    }
                    String fulltextDir = FileUtil.createDirNameFromPattern(databaseContext.getDatabase(), entry, filePreferences.getFileDirectoryPattern());
                    suggestedName = FileNameUniqueness.getNonOverWritingFileName(targetDirectory.resolve(fulltextDir), suggestedName);

                    return targetDirectory.resolve(fulltextDir).resolve(suggestedName);
                })
                .then(destination -> new FileDownloadTask(urlDownload.getSource(), destination))
                .onFailure(ex -> LOGGER.error("Error in download", ex))
                .onFinished(() -> {
                    downloadProgress.unbind();
                    downloadProgress.set(1);
                });
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
        return urlDownload.getMimeType()
                          .flatMap(mimeType -> {
                              LOGGER.debug("MIME Type suggested: {}", mimeType);
                              return ExternalFileTypes.getExternalFileTypeByMimeType(mimeType, externalApplicationsPreferences);
                          });
    }

    private Optional<ExternalFileType> inferFileTypeFromURL(String url) {
        return URLUtil.getSuffix(url, externalApplicationsPreferences)
                      .flatMap(extension -> ExternalFileTypes.getExternalFileTypeByExt(extension, externalApplicationsPreferences));
    }

    public ReadOnlyDoubleProperty downloadProgress() {
        return downloadProgress;
    }

    private static class FileDownloadTask extends BackgroundTask<Path> {
        private final URL source;
        private final Path destination;

        public FileDownloadTask(URL source, Path destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public Path call() throws FetcherException, IOException {
            URLDownload download = new URLDownload(source);
            try (ProgressInputStream inputStream = download.asInputStream()) {
                EasyBind.subscribe(
                        inputStream.totalNumBytesReadProperty(),
                        bytesRead -> updateProgress(bytesRead.longValue(), inputStream.getMaxNumBytes()));
                // Make sure directory exists since otherwise copy fails
                Files.createDirectories(destination.getParent());
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return destination;
        }
    }
}
