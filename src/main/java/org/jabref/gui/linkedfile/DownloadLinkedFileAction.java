package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

import javafx.beans.property.DoubleProperty;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiles.FileDownloadTask;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.fieldeditors.URLUtil;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.io.FileNameUniqueness;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadLinkedFileAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final DialogService dialogService;
    private final BibEntry entry;
    private final LinkedFile linkedFile;
    private final String suggestedName;
    private final String downloadUrl;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    private final BibDatabaseContext databaseContext;

    private final DoubleProperty downloadProgress;
    private final LinkedFileHandler linkedFileHandler;

    public DownloadLinkedFileAction(BibDatabaseContext databaseContext,
                                    BibEntry entry,
                                    LinkedFile linkedFile,
                                    String downloadUrl,
                                    DialogService dialogService,
                                    FilePreferences filePreferences,
                                    TaskExecutor taskExecutor,
                                    String suggestedName,
                                    DoubleProperty downloadProgress) {
        this.databaseContext = databaseContext;
        this.entry = entry;
        this.linkedFile = linkedFile;
        this.suggestedName = suggestedName;
        this.downloadUrl = downloadUrl;
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
        this.downloadProgress = downloadProgress;

        this.linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);
    }

    public DownloadLinkedFileAction(BibDatabaseContext databaseContext,
                                    BibEntry entry,
                                    LinkedFile linkedFile,
                                    String downloadUrl,
                                    DialogService dialogService,
                                    FilePreferences filePreferences,
                                    TaskExecutor taskExecutor,
                                    String suggestedName) {
        this(databaseContext, entry, linkedFile, downloadUrl, dialogService, filePreferences, taskExecutor, suggestedName, null);
    }

    public DownloadLinkedFileAction(BibDatabaseContext databaseContext,
                                    BibEntry entry,
                                    LinkedFile linkedFile,
                                    String downloadUrl,
                                    DialogService dialogService,
                                    FilePreferences filePreferences,
                                    TaskExecutor taskExecutor,
                                    DoubleProperty downloadProgress) {
        this(databaseContext, entry, linkedFile, downloadUrl, dialogService, filePreferences, taskExecutor, "", downloadProgress);
    }

    public DownloadLinkedFileAction(BibDatabaseContext databaseContext,
                                    BibEntry entry,
                                    LinkedFile linkedFile,
                                    String downloadUrl,
                                    DialogService dialogService,
                                    FilePreferences filePreferences,
                                    TaskExecutor taskExecutor) {
        this(databaseContext, entry, linkedFile, downloadUrl, dialogService, filePreferences, taskExecutor, "", null);
    }

    @Override
    public void execute() {
        LOGGER.info("Downloading file from " + downloadUrl);
        if (downloadUrl.isEmpty() || !LinkedFile.isOnlineLink(downloadUrl)) {
            throw new UnsupportedOperationException("In order to download the file, the url has to be an online link");
        }

        Optional<Path> targetDirectory = databaseContext.getFirstExistingFileDir(filePreferences);
        if (targetDirectory.isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Download file"), Localization.lang("File directory is not set or does not exist!"));
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
        if (downloadProgress != null) {
            downloadProgress.bind(downloadTask.workDonePercentageProperty());
            // If not given, we show it after the task is started
        }

        downloadTask.titleProperty().set(Localization.lang("Downloading"));
        entry.getCitationKey().ifPresentOrElse(
                citationkey -> downloadTask.messageProperty().set(Localization.lang("Fulltext for %0", citationkey)),
                () -> downloadTask.messageProperty().set(Localization.lang("Fulltext for a new entry")));
        downloadTask.showToUser(true);

        downloadTask.onFailure(ex -> onFailure(urlDownload, ex));
        downloadTask.onSuccess(destination -> onSuccess(targetDirectory, destination));

        Future<Path> taskFuture = taskExecutor.execute(downloadTask);
        if (downloadProgress == null) {
            Task<Path> task = (Task<Path>) taskFuture;
            if (task != null) {
                dialogService.showProgressDialog(
                        Localization.lang("Downloading"),
                        Localization.lang("Looking for full text document..."),
                        task);
            }
        }
    }

    private void onSuccess(Path targetDirectory, Path destination) {
        boolean isDuplicate;
        try {
            isDuplicate = FileNameUniqueness.isDuplicatedFile(targetDirectory, destination.getFileName(), dialogService);
        } catch (IOException e) {
            LOGGER.error("FileNameUniqueness.isDuplicatedFile failed", e);
            return;
        }

        if (isDuplicate) {
            destination = targetDirectory.resolve(
                    FileNameUniqueness.eraseDuplicateMarks(destination.getFileName()));
        }

        // If the file type was set to URL because it was a link, try to find a more suitable type now
        if (linkedFile.getFileType().equals(StandardExternalFileType.URL.getName())) {
            String fileExtension = FileUtil.getFileExtension(destination).orElse("");
            ExternalFileType suggestedFileType = ExternalFileTypes.getExternalFileTypeByExt(fileExtension, filePreferences)
                                                                  .orElse(new UnknownExternalFileType(fileExtension));
            linkedFile.setFileType(suggestedFileType.getName());
        }

        // Store the download url to the source URL
        if (linkedFile.getSourceUrl().isEmpty()) {
            linkedFile.setSourceURL(linkedFile.getLink());
        }

        // Set the linked file's link to the downloaded file's path
        linkedFile.setLink(FileUtil.relativize(destination,
                databaseContext.getFileDirectories(filePreferences)).toString());

        // Notify in bar when the file type is HTML.
        if (linkedFile.getFileType().equals(StandardExternalFileType.URL.getName())) {
            dialogService.notify(Localization.lang("Downloaded website as an HTML file."));
            LOGGER.debug("Downloaded website {} as an HTML file at {}", linkedFile.getLink(), destination);
        }
    }

    private void onFailure(URLDownload urlDownload, Exception ex) {
        LOGGER.error("Error downloading from URL: " + urlDownload, ex);
        String fetcherExceptionMessage = ex.getMessage();
        String failedTitle = Localization.lang("Failed to download from URL");
        int statusCode;
        if (ex instanceof FetcherClientException clientException) {
            statusCode = clientException.getStatusCode();
            if (statusCode == 401) {
                dialogService.showInformationDialogAndWait(failedTitle, Localization.lang("401 Unauthorized: Access Denied. You are not authorized to access this resource. Please check your credentials and try again. If you believe you should have access, please contact the administrator for assistance.\nURL: %0 \n %1", urlDownload.getSource(), fetcherExceptionMessage));
            } else if (statusCode == 403) {
                dialogService.showInformationDialogAndWait(failedTitle, Localization.lang("403 Forbidden: Access Denied. You do not have permission to access this resource. Please contact the administrator for assistance or try a different action.\nURL: %0 \n %1", urlDownload.getSource(), fetcherExceptionMessage));
            } else if (statusCode == 404) {
                dialogService.showInformationDialogAndWait(failedTitle, Localization.lang("404 Not Found Error: The requested resource could not be found. It seems that the file you are trying to download is not available or has been moved. Please verify the URL and try again. If you believe this is an error, please contact the administrator for further assistance.\nURL: %0 \n %1", urlDownload.getSource(), fetcherExceptionMessage));
            }
        } else if (ex instanceof FetcherServerException serverException) {
            statusCode = serverException.getStatusCode();
            dialogService.showInformationDialogAndWait(failedTitle,
                    Localization.lang("Error downloading from URL. Cause is likely the server side. HTTP Error %0 \n %1 \nURL: %2 \nPlease try again later or contact the server administrator.", statusCode, fetcherExceptionMessage, urlDownload.getSource()));
        } else {
            dialogService.showErrorDialogAndWait(failedTitle, Localization.lang("Error message: %0 \nURL: %1 \nPlease check the URL and try again.", fetcherExceptionMessage, urlDownload.getSource()));
        }
    }

    private boolean checkSSLHandshake(URLDownload urlDownload) {
        try {
            urlDownload.canBeReached();
        } catch (kong.unirest.UnirestException ex) {
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

    public BackgroundTask<Path> prepareDownloadTask(Path targetDirectory, URLDownload urlDownload) {
        SSLSocketFactory defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
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
                .onFinished(() -> URLDownload.setSSLVerification(defaultSSLSocketFactory, defaultHostnameVerifier));
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
            return ExternalFileTypes.getExternalFileTypeByMimeType(mimeType, filePreferences);
        } else {
            return Optional.empty();
        }
    }

    private Optional<ExternalFileType> inferFileTypeFromURL(String url) {
        return URLUtil.getSuffix(url, filePreferences)
                      .flatMap(extension -> ExternalFileTypes.getExternalFileTypeByExt(extension, filePreferences));
    }
}
