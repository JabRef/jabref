package org.jabref.gui.filewizard;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiles.DownloadFullTextAction;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.Globals;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.sun.star.sdb.DatabaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class downloads a PDF-File in a specified directory. It will also connect it to the BibEntry.
 * There will be more methods implemented, for example one which searches for the DOI of a BibEntry based on the Title.
 * Also there will be a method which searches for the URL of a file based on the DOI.
 */
public class FileWizardDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileWizardDownloader.class);

    /**
     * Manages the downloader (which is only called if no linked file for the concerning pdf exists)
     * @param dialogService     is needed to call the method who downloads the file
     * @param preferences       is needed to call the method who downloads the file and who searches for teh url
     * @param databaseContext   is needed to call the method who downloads the file
     * @param targetDirectory   where the file is safed after download
     * @param entry             the BibEntry which the pdf is needed
     * @return                  boolean if a pdf was downloaded or not
     */
    public static boolean startDownloader(DialogService dialogService, PreferencesService preferences, BibDatabaseContext databaseContext, Path targetDirectory, BibEntry entry) {

        LOGGER.debug("FW:startDownloader, started");
        Optional<URL> url;
        url = fileWizardSearchURL(entry, preferences.getImportFormatPreferences());

        if (url.isPresent()) {
            LOGGER.debug("url was found, call fw-downloadFromURL");

            // call the method which downloads
            fileWizardDownloaderFromURL(dialogService, preferences, databaseContext, url.get(), entry, targetDirectory);

            LOGGER.debug("(startDownloader): downloader should end now");
            return true;
        } else {
            LOGGER.info("startDownloader: was not possible to find url");
            return false;
            // ToDo: let Report know that it was not possible to download it
            // add debug logger
        }
    }

    /**
     * attaches a linked file from a url (which is not linked yet) to an entry
     * and downloads the file into the target directory (also links it afterwards)
     * @param dialogService     is needed to crate a LinkedFileViewModel, which is needed for the download
     * @param preferences       is needed to crate a LinkedFileViewModel, which is needed for the download
     * @param databaseContext   the active database
     * @param url               the url "key"
     * @param entry             the entry "value"
     * @param targetDirectory   the target directory for the downloaded file
     */
    public static void fileWizardDownloaderFromURL(DialogService dialogService, PreferencesService preferences, BibDatabaseContext databaseContext, URL url, BibEntry entry, Path targetDirectory) {
        LOGGER.debug("downloadFromURL started");

        // linked file represents the link to an external file (e.g. associated pdf file)
        LinkedFile newLinkedFile = new LinkedFile(url, "");

        // new linkedFileViewModel
        LinkedFileViewModel onlineFile = new LinkedFileViewModel(
                newLinkedFile,
                entry,
                databaseContext,
                Globals.TASK_EXECUTOR,
                dialogService,
                preferences.getXmpPreferences(),
                preferences.getFilePreferences(),
                ExternalFileTypes.getInstance());

        try {
            URLDownload urldownload = new URLDownload(newLinkedFile.getLink());

            // this calls to the download
            BackgroundTask<Path> downloadTask = onlineFile.prepareDownloadTask(targetDirectory, urldownload);
            LOGGER.info("dwlFromURL (try), startet downloading");

            // the downloaded file is "linked" to the bibEntry
            downloadTask.onSuccess(destination -> {
                LinkedFile downloadedFile = LinkedFilesEditorViewModel.fromFile(
                        destination,
                        databaseContext.getFileDirectories(preferences.getFilePreferences()),
                        ExternalFileTypes.getInstance());
                entry.addFile(downloadedFile);
            });
            LOGGER.info("downloadFromURL(try), file should now be linked");

            downloadTask.titleProperty().set(Localization.lang("Downloading"));
            downloadTask.messageProperty().set(
                    Localization.lang("Fulltext for") + ": " + entry.getCitationKey().orElse(Localization.lang("New entry")));
            downloadTask.showToUser(true);

            // executes the download (was prepared before, without this, the download never starts)
            // Globals.TASK_EXECUTOR.execute(downloadTask);
            Platform.runLater(() -> Globals.TASK_EXECUTOR.execute(downloadTask));

        } catch (MalformedURLException exception) {
            LOGGER.info("DwldFromURL, malformedURLException");
            dialogService.showErrorDialogAndWait(Localization.lang("Invalid URL"), exception);
            // ToDo: let report know that this has not been found
        }
    }

    /**
     * calls a method which searches for the url of a bibEntry
     * @param entry the entry you want to know the url for
     * @param preferences needed to create FulltextFetchers (which searches for url
     * @return  the optional url of the entry
     */
    public static Optional<URL> fileWizardSearchURL(BibEntry entry, ImportFormatPreferences preferences) {
        LOGGER.debug("searchURL was startet");
        FulltextFetchers fetchers = new FulltextFetchers(preferences);

        // searches for url and also doi (but doi is not set as a field, since only a copy of the entry is passed, within the FUlltextFetchers class)
        Optional<URL> optULR = fetchers.findFullTextPDF(entry);
        LOGGER.debug("finished looking for ulr");

        // this part is only in during development to see what url is found (not always the same url for the same enry
        String test2 = optULR.toString();
        System.out.println("url found in fulltextfetchers: " + test2);

        return optULR;
    }
}
