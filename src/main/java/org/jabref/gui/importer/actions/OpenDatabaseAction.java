package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.dialogs.BackupUIManager;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
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
            new CheckForNewEntryTypesAction(),
            // AI chat history links BibEntry with citation key. When citation key is changed, chat history should be transferred from old citation key to new citation key
            new ListenForCitationKeyChangeForAiAction());

    private final LibraryTabContainer tabContainer;
    private final PreferencesService preferencesService;
    private final AiService aiService;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final DialogService dialogService;
    private final BibEntryTypesManager entryTypesManager;
    private final CountingUndoManager undoManager;
    private final ClipBoardManager clipboardManager;
    private final TaskExecutor taskExecutor;

    public OpenDatabaseAction(LibraryTabContainer tabContainer,
                              PreferencesService preferencesService,
                              AiService aiService,
                              DialogService dialogService,
                              StateManager stateManager,
                              FileUpdateMonitor fileUpdateMonitor,
                              BibEntryTypesManager entryTypesManager,
                              CountingUndoManager undoManager,
                              ClipBoardManager clipBoardManager,
                              TaskExecutor taskExecutor) {
        this.tabContainer = tabContainer;
        this.preferencesService = preferencesService;
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.undoManager = undoManager;
        this.clipboardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Go through the list of post open actions, and perform those that need to be performed.
     *
     * @param result     The result of the BIB file parse operation.
     */
    public static void performPostOpenActions(ParserResult result, DialogService dialogService, PreferencesService preferencesService) {
        for (GUIPostOpenAction action : OpenDatabaseAction.POST_OPEN_ACTIONS) {
            if (action.isActionNecessary(result, preferencesService)) {
                action.performAction(result, dialogService, preferencesService);
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
        if (tabContainer.getLibraryTabs().isEmpty()) {
            return preferencesService.getFilePreferences().getWorkingDirectory();
        } else {
            Optional<Path> databasePath = tabContainer.getCurrentLibraryTab().getBibDatabaseContext().getDatabasePath();
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
            for (LibraryTab libraryTab : tabContainer.getLibraryTabs()) {
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
            FileHistory fileHistory = preferencesService.getGuiPreferences().getFileHistory();
            filesToOpen.forEach(theFile -> {
                // This method will execute the concrete file opening and loading in a background thread
                openTheFile(theFile);
                fileHistory.newFile(theFile);
            });
        } else if (toRaise != null && tabContainer.getCurrentLibraryTab() == null) {
            // If no files are remaining to open, this could mean that a file was
            // already open. If so, we may have to raise the correct tab:
            // If there is already a library focused, do not show this library
            tabContainer.showLibraryTab(toRaise);
        }
    }

    /**
     * This is the real file opening. Should be called via {@link #openFile(Path)}
     *
     * Similar method: {@link org.jabref.gui.frame.JabRefFrame#addTab(org.jabref.model.database.BibDatabaseContext, boolean)}.
     *
     * @param file the file, may be NOT null, but may not be existing
     */
    private void openTheFile(Path file) {
        Objects.requireNonNull(file);
        if (!Files.exists(file)) {
            return;
        }

        BackgroundTask<ParserResult> backgroundTask = BackgroundTask.wrap(() -> loadDatabase(file));
        // The backgroundTask is executed within the method createLibraryTab
        LibraryTab newTab = LibraryTab.createLibraryTab(
                backgroundTask,
                file,
                dialogService,
                preferencesService,
                aiService,
                stateManager,
                tabContainer,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager,
                clipboardManager,
                taskExecutor);
        tabContainer.addTab(newTab, true);
    }

    private ParserResult loadDatabase(Path file) throws Exception {
        Path fileToLoad = file.toAbsolutePath();

        dialogService.notify(Localization.lang("Opening") + ": '" + file + "'");

        preferencesService.getFilePreferences().setWorkingDirectory(fileToLoad.getParent());
        Path backupDir = preferencesService.getFilePreferences().getBackupDirectory();

        ParserResult parserResult = null;
        if (BackupManager.backupFileDiffers(fileToLoad, backupDir)) {
            // In case the backup differs, ask the user what to do.
            // In case the user opted for restoring a backup, the content of the backup is contained in parserResult.
            parserResult = BackupUIManager.showRestoreBackupDialog(dialogService, fileToLoad, preferencesService, fileUpdateMonitor, undoManager, stateManager)
                                          .orElse(null);
        }

        try {
            if (parserResult == null) {
                // No backup was restored, do the "normal" loading
                parserResult = OpenDatabase.loadDatabase(fileToLoad,
                        preferencesService.getImportFormatPreferences(),
                        fileUpdateMonitor);
            }

            if (parserResult.hasWarnings()) {
                String content = Localization.lang("Please check your library file for wrong syntax.")
                        + "\n\n" + parserResult.getErrorMessage();
                UiTaskExecutor.runInJavaFXThread(() ->
                        dialogService.showWarningDialogAndWait(Localization.lang("Open library error"), content));
            }
        } catch (IOException e) {
            parserResult = ParserResult.fromError(e);
            LOGGER.error("Error opening file '{}'", fileToLoad, e);
        }

        if (parserResult.getDatabase().isShared()) {
                         openSharedDatabase(
                                 parserResult,
                                 tabContainer,
                                 dialogService,
                                 preferencesService,
                                 aiService,
                                 stateManager,
                                 entryTypesManager,
                                 fileUpdateMonitor,
                                 undoManager,
                                 clipboardManager,
                                 taskExecutor);
        }
        return parserResult;
    }

    public static void openSharedDatabase(ParserResult parserResult,
                                          LibraryTabContainer tabContainer,
                                          DialogService dialogService,
                                          PreferencesService preferencesService,
                                          AiService aiService,
                                          StateManager stateManager,
                                          BibEntryTypesManager entryTypesManager,
                                          FileUpdateMonitor fileUpdateMonitor,
                                          UndoManager undoManager,
                                          ClipBoardManager clipBoardManager,
                                          TaskExecutor taskExecutor)
            throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException, NotASharedDatabaseException {
        try {
            new SharedDatabaseUIManager(
                    tabContainer,
                    dialogService,
                    preferencesService,
                    aiService,
                    stateManager,
                    entryTypesManager,
                    fileUpdateMonitor,
                    undoManager,
                    clipBoardManager,
                    taskExecutor)
                    .openSharedDatabaseFromParserResult(parserResult);
        } catch (SQLException | DatabaseNotSupportedException | InvalidDBMSConnectionPropertiesException |
                 NotASharedDatabaseException e) {
            parserResult.getDatabaseContext().clearDatabasePath(); // do not open the original file
            parserResult.getDatabase().clearSharedDatabaseID();

            throw e;
        }
    }
}
