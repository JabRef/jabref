package org.jabref.gui.filewizard;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.Globals;
import org.jabref.gui.externalfiles.AutoSetFileLinksUtil;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.jabref.preferences.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.jabref.gui.filewizard.FileWizardLocal.*;
import static org.jabref.gui.filewizard.FileWizardDownloader.*;

/**
 * Handles the actual execution of the File Wizard, combining the different functionalities.
 */
public class FileWizardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileWizardManager.class);

    private final File directory;
    private final List<Path> directoryAsList = new ArrayList<>();
    private final DialogService dialogService;
    private final ArrayList<BibEntry> unsuccessfullyLinked;
    private final File checkedFilesListFile;
    private List<String> checkedFilesList;
    private final FileWizardSerializer serializer;

    /**
     * @param dialogService The dialog service.
     * @param stateManager The state manager.
     * @param openDirectory True if the user checked the box to open the directory after the File Wizard has completed.
     * @param directory The selected target directory in which the File Wizard expects and downloads the fulltexts.
     * @param preferences The preferences service.
     */
    public FileWizardManager(DialogService dialogService, StateManager stateManager,
                             boolean openDirectory, File directory, PreferencesService preferences) {
        this.directory = directory;
        this.directoryAsList.add(directory.toPath());
        this.dialogService = dialogService;
        this.unsuccessfullyLinked = new ArrayList<>();
        this.serializer = new FileWizardSerializer();

        this.checkedFilesListFile = new File(directory, "jabref_info.txt");

        // if the info file for the checked files list doesn't exists (i. e. the file wizard is executed for the first time),
        // then the info file is created. Otherwise the file is deserialized.
        if (!checkedFilesListFile.exists()) {
            try {
                checkedFilesListFile.createNewFile();
                checkedFilesList = new ArrayList<>();
            } catch (IOException ioe) {
                LOGGER.error("couldn't create info file.");
            }
        } else {
            checkedFilesList = serializer.deserializeCheckedFilesList(checkedFilesListFile);
        }

        FileWizardProgressDialog progressDialog = new FileWizardProgressDialog(stateManager.getActiveDatabase()
                .get().getEntries().size() - checkedFilesList.size());

        // The task which actually coordinates the different functionalities.
        Task<Void> fileWizard = new Task<>() {
            @Override
            protected Void call() {
                List<BibEntry> entriesList = new LinkedList<>(stateManager.getActiveDatabase().get().getEntries());

                // Removes all entries which have previously been successfully linked from the list of BibEntries
                // to be handled, in order to avoid unnecessary work for the File Wizard. The citation key is used
                // as an identifier, because serialization doesn't work with BibEntry objects.
                for(BibEntry entry : entriesList) {
                    if(checkedFilesList.contains(entry.getCitationKey().get())) {
                        Platform.runLater(() -> entriesList.remove(entry));
                    }
                }

                // Iterates over all entries which haven't already been successfully linked.
                for(BibEntry entry : entriesList) {
                    if(!progressDialog.isRunning()) {
                        for(int i = entriesList.indexOf(entry); i < entriesList.size(); i++) {
                            unsuccessfullyLinked.add(entriesList.get(i));
                        }
                        return null;
                    }

                    progressDialog.updateProgress(entry, entriesList.indexOf(entry));

                    LOGGER.info("File Wizard is handling " + entry.getCitationKey());

                    // Checks if the linked file exists locally and if it does it copies it into the target directory.
                    if(!fileExistsLocally(entry, directory.toPath(), stateManager.getActiveDatabase().get().getFileDirectories(preferences.getFilePreferences()))) {
                        // Checks if the entry can be mapped to an existing file in the target directory based on the
                        // the file's name and the conventions declared in the settings.
                        if(!mapToFileInDirectory(entry, new AutoSetFileLinksUtil(directoryAsList, preferences.getAutoLinkPreferences(), ExternalFileTypes.getInstance()))) {
                            // Tries to download the file based on the entry's DOI.
                            if(!startDownloader(dialogService, preferences, stateManager.getActiveDatabase().get(), directory.toPath(), entry)) {
                                // If the entry arrives here, then the File Wizard couldn't link a fulltext file,
                                // the entry's unsuccessful linking will be displayed in the report window at the end
                                // and the File Wizard tries to link the next file in the list.
                                unsuccessfullyLinked.add(entry);
                                continue;
                            }
                        }
                    }
                    // If the entry arrives here, it could be successfully linked and is added to the list which will be
                    // serialized and the entry will never again be iterated over by the File Wizard.
                    checkedFilesList.add(entry.getCitationKey().get());
                }
                return null;
            }
        };

        fileWizard.setOnSucceeded((e) -> {
            if (openDirectory) {
                openDirectory();
            }
            serializer.serializeCheckedFiles(checkedFilesList, checkedFilesListFile);
            progressDialog.close();
            if(unsuccessfullyLinked.isEmpty()) {
                dialogService.showInformationDialogAndWait(Localization.lang("File Wizard"),
                        Localization.lang("The File Wizard could successfully link all BibEntries to their respective fulltext files."));
            } else {
                FileWizardReport fileWizardReport = new FileWizardReport(unsuccessfullyLinked);
                Platform.runLater(fileWizardReport::showAndWait);
            }
        });

        //dialogService.showProgressDialog("File Wizard", "Please wait, the file wizard is working.", fileWizard);
        Globals.TASK_EXECUTOR.execute(fileWizard);
        Platform.runLater(progressDialog::showAndWait);
    }


    private void openDirectory() {
        try {
            Desktop.getDesktop().open(new File(directory.toURI()));
        } catch (IOException ioe) {
            dialogService.showErrorDialogAndWait("An IO error occurred regarding opening the directory");
            LOGGER.error("An IO error occurred regarding the directory");
        }
    }

    public File getDirectory() {
        return directory;
    }
}
