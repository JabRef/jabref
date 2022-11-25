package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.dialogs.BackupUIManager;
import org.jabref.gui.menus.FileHistoryMenu;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The action concerned with opening an existing database.
public class OpenDatabaseAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDatabaseAction.class);

    // List of actions that may need to be called after opening the file. Such as
    // upgrade actions etc. that may depend on the JabRef version that wrote the file:
    private static final List<GUIPostOpenAction> POST_OPEN_ACTIONS = List.of(
            // Migrations:
            // Warning for migrating the Review into the Comment field
            new MergeReviewIntoCommentAction(),
            // Check for new custom entry types loaded from the BIB file:
            new CheckForNewEntryTypesAction());

    private final JabRefFrame frame;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;
    private final DialogService dialogService;

    public OpenDatabaseAction(JabRefFrame frame,
                              PreferencesService preferencesService,
                              DialogService dialogService,
                              StateManager stateManager,
                              ThemeManager themeManager) {
        this.frame = frame;
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.themeManager = themeManager;
    }

    /**
     * Go through the list of post open actions, and perform those that need to be performed.
     *
     * @param libraryTab The BasePanel where the database is shown.
     * @param result     The result of the BIB file parse operation.
     */
    public static void performPostOpenActions(LibraryTab libraryTab, ParserResult result) {
        for (GUIPostOpenAction action : OpenDatabaseAction.POST_OPEN_ACTIONS) {
            if (action.isActionNecessary(result)) {
                action.performAction(libraryTab, result);
                libraryTab.frame().showLibraryTab(libraryTab);
            }
        }
    }

    @Override
    public void execute() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.BIBTEX_DB)
                .withDefaultExtension(StandardFileType.BIBTEX_DB)
                .withInitialDirectory(getInitialDirectory())
                .build();

        List<Path> filesToOpen = dialogService.showFileOpenDialogAndGetMultipleFiles(fileDialogConfiguration);
        openFiles(filesToOpen);
    }

    /**
     * @return Path of current panel database directory or the working directory
     */
    private Path getInitialDirectory() {
        if (frame.getBasePanelCount() == 0) {
            return preferencesService.getFilePreferences().getWorkingDirectory();
        } else {
            Optional<Path> databasePath = frame.getCurrentLibraryTab().getBibDatabaseContext().getDatabasePath();
            return databasePath.map(Path::getParent).orElse(preferencesService.getFilePreferences().getWorkingDirectory());
        }
    }

    /**
     * Opens the given file. If null or 404, nothing happens.
     * In case the file is already opened, that panel is raised.
     *
     * @param file the file, may be null or not existing
     */
    public void openFile(Path file) {
        openFiles(new ArrayList<>(List.of(file)));
    }

    /**
     * Opens the given files. If one of it is null or 404, nothing happens.
     * In case the file is already opened, that panel is raised.
     *
     * @param filesToOpen the filesToOpen, may be null or not existing
     */
    public void openFiles(List<Path> filesToOpen) {
        LibraryTab toRaise = null;
        int initialCount = filesToOpen.size();
        int removed = 0;

        // Check if any of the files are already open:
        for (Iterator<Path> iterator = filesToOpen.iterator(); iterator.hasNext(); ) {
            Path file = iterator.next();
            for (int i = 0; i < frame.getTabbedPane().getTabs().size(); i++) {
                LibraryTab libraryTab = frame.getLibraryTabAt(i);
                if ((libraryTab.getBibDatabaseContext().getDatabasePath().isPresent())
                        && libraryTab.getBibDatabaseContext().getDatabasePath().get().equals(file)) {
                    iterator.remove();
                    removed++;
                    // See if we removed the final one. If so, we must perhaps
                    // raise the LibraryTab in question:
                    if (removed == initialCount) {
                        toRaise = libraryTab;
                    }
                    // no more LibraryTabs to check, we found a matching one
                    break;
                }
            }
        }

        // Run the actual open in a thread to prevent the program
        // locking until the file is loaded.
        if (!filesToOpen.isEmpty()) {
            FileHistoryMenu fileHistory = frame.getFileHistory();
            filesToOpen.forEach(theFile -> {
                // This method will execute the concrete file opening and loading in a background thread
                openTheFile(theFile);
                fileHistory.newFile(theFile);
            });
        } else if (toRaise != null) {
            // If no files are remaining to open, this could mean that a file was
            // already open. If so, we may have to raise the correct tab:
            frame.showLibraryTab(toRaise);
        }
    }

    /**
     * This is the real file opening. Should be called via {@link #openFile(Path)}
     *
     * @param file the file, may be NOT null, but may not be existing
     */
    private void openTheFile(Path file) {
        Objects.requireNonNull(file);
        if (!Files.exists(file)) {
            return;
        }

        BackgroundTask<ParserResult> backgroundTask = BackgroundTask.wrap(() -> loadDatabase(file));
        LibraryTab.Factory libraryTabFactory = new LibraryTab.Factory();
        LibraryTab newTab = libraryTabFactory.createLibraryTab(frame, preferencesService, stateManager, themeManager, file, backgroundTask, Globals.IMPORT_FORMAT_READER);

        backgroundTask.onFinished(() -> trackOpenNewDatabase(newTab));
    }

    /**
     * This method is similar to {@link org.jabref.gui.JabRefGUI#openLastEditedDatabases()}.
     * This method also has the capability to open remote shared databases
     */
    private ParserResult loadDatabase(Path file) throws Exception {
        Path fileToLoad = file.toAbsolutePath();

        dialogService.notify(Localization.lang("Opening") + ": '" + file + "'");

        preferencesService.getFilePreferences().setWorkingDirectory(fileToLoad.getParent());

        if (BackupManager.backupFileDiffers(fileToLoad)) {
            BackupUIManager.showRestoreBackupDialog(dialogService, fileToLoad);
        }

        ParserResult result;
        try {
            result = OpenDatabase.loadDatabase(fileToLoad,
                    preferencesService.getImportFormatPreferences(),
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

    private void trackOpenNewDatabase(LibraryTab libraryTab) {
        Map<String, String> properties = new HashMap<>();
        Map<String, Double> measurements = new HashMap<>();
        measurements.put("NumberOfEntries", (double) libraryTab.getBibDatabaseContext().getDatabase().getEntryCount());

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("OpenNewDatabase", properties, measurements));
    }
}
