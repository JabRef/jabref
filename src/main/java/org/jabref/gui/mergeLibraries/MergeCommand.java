package org.jabref.gui.mergeLibraries;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.dialogs.BackupUIManager;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;

import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jabref.logic.database.DuplicateCheck.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;


public class MergeCommand extends SimpleCommand {
    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final StateManager stateManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDatabaseAction.class);

    public MergeCommand(JabRefFrame frame,PreferencesService preferences, StateManager stateManager) {

        this.preferences = preferences;
        this.frame = frame;
        this.dialogService = frame.getDialogService();
        this.stateManager = stateManager;
    }

    @Override
    public void execute() {
        //TODO: write the execute method

        //construct a configuration with the current directory
        //(this really could have just been a constructor so think of it like that)
        DirectoryDialogConfiguration.Builder config = new DirectoryDialogConfiguration.Builder().withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory());
        //open dialog box using the dialog service which will return a path to a directory
        Optional<Path> path = dialogService.showDirectorySelectionDialog(config.build());


        Optional<BibDatabaseContext> database = stateManager.getActiveDatabase();
        if(path.isPresent() && database.isPresent())
            doMerge(path.get(), database.get().getDatabase());
    }

    public BibDatabase doMerge(Path path, BibDatabase database){
        SortedSet<Importer> importers = Globals.IMPORT_FORMAT_READER.getImportFormats();
        //find all the .lib files by crawling in doMerge and merge them
        for(File f : getAllFiles(path)){
            System.out.println(f.toString());
            ParserResult result;

            //try to convert the .bib file to a database
            try {
                result = loadDatabase(f.toPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            /*then go through each entry in the current database and check
              against the following rules for merging:
              1. if the entry is equal to any in the main database then ignore the new entry
              2. if the entry has the same key as any in the main database then ignore the new entry
              3. if the entry is a duplicate of any in the main database then ignore the new entry
            */
            for(BibEntry entry : result.getDatabase().getEntries()){
                for(BibEntry dbEntry : database.getEntries()){
//                    if(!entry.equals(dbEntry) && !(entry.getCiteKeyBinding().equals(dbEntry.getCiteKeyBinding()))
//                            && !(DuplicateCheck()))
                }
            }
        }


        //then add the database to the StateManager's activeDatabase

        return null;
    }

    /**
     *  getAllFiles: Recursively crawls through a directory to find all .bib files
     *
     * @param path the path to a directory containing the .bib files we are looking
     *             to merge
     * @return all the .bib files in a given directory and subdirectories
     */
    private List<File> getAllFiles(Path path){
        List<File> files = new ArrayList<>();

        // Get all the .bib files in this directory
        List currentFile = List.of(path.toFile().listFiles((dir, name) -> name.endsWith(".bib")));
        if(currentFile != null)
            files.addAll(currentFile);

        // Get all the directories in the directory
        File[] directories = path.toFile().listFiles(File::isDirectory);
        if(directories == null) return files;

        //crawl through all the files in the directory and add them to the files
        for(File f : directories){
            Path p = f.toPath();
            files.addAll(getAllFiles(p));
        }

        return files;
    }

    /**
     * This method is taken from the loadDatabase method in {@link OpenDatabaseAction}.
     */
    private ParserResult loadDatabase(Path file) throws Exception {
        Path fileToLoad = file.toAbsolutePath();

        preferences.getFilePreferences().setWorkingDirectory(fileToLoad.getParent());

        if (BackupManager.backupFileDiffers(fileToLoad)) {
            BackupUIManager.showRestoreBackupDialog(dialogService, fileToLoad);
        }

        ParserResult result;
        try {
            result = OpenDatabase.loadDatabase(fileToLoad,
                    preferences.getImportFormatPreferences(),
                    Globals.getFileUpdateMonitor());
            if (result.hasWarnings()) {
                String content = Localization.lang("Please check your library file for wrong syntax.")
                        + "\n\n" + result.getErrorMessage();
                DefaultTaskExecutor.runInJavaFXThread(() ->
                        dialogService.showWarningDialogAndWait(Localization.lang("Open library error"), content));
            }
        } catch (IOException e) {
            result = ParserResult.fromError(e);
            LOGGER.error("Error opening file '{}'", fileToLoad, e);
        }

        if (result.getDatabase().isShared()) {
            try {
                new SharedDatabaseUIManager(frame).openSharedDatabaseFromParserResult(result);
            } catch (SQLException | DatabaseNotSupportedException | InvalidDBMSConnectionPropertiesException |
                     NotASharedDatabaseException e) {
                result.getDatabaseContext().clearDatabasePath(); // do not open the original file
                result.getDatabase().clearSharedDatabaseID();
                LOGGER.error("Connection error", e);

                throw e;
            }
        }
        return result;
    }
}
