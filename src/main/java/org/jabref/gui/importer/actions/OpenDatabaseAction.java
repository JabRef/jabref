package org.jabref.gui.importer.actions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.BasePanelPreferences;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.dialogs.BackupUIManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.importer.ParserResultWarningDialog;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The action concerned with opening an existing database.
public class OpenDatabaseAction extends SimpleCommand {

    public static final Logger LOGGER = LoggerFactory.getLogger(OpenDatabaseAction.class);
    // List of actions that may need to be called after opening the file. Such as
    // upgrade actions etc. that may depend on the JabRef version that wrote the file:
    private static final List<GUIPostOpenAction> POST_OPEN_ACTIONS = Arrays.asList(
                                                                                   // Migrations:
                                                                                   // Warning for migrating the Review into the Comment field
                                                                                   new MergeReviewIntoCommentAction(),
                                                                                   // Check for new custom entry types loaded from the BIB file:
                                                                                   new CheckForNewEntryTypesAction());

    private final JabRefFrame frame;
    private final DialogService dialogService;

    public OpenDatabaseAction(JabRefFrame frame) {
        this.frame = frame;
        this.dialogService = frame.getDialogService();
    }

    /**
     * Go through the list of post open actions, and perform those that need to be performed.
     *
     * @param panel  The BasePanel where the database is shown.
     * @param result The result of the BIB file parse operation.
     */
    public static void performPostOpenActions(BasePanel panel, ParserResult result) {
        for (GUIPostOpenAction action : OpenDatabaseAction.POST_OPEN_ACTIONS) {
            if (action.isActionNecessary(result)) {
                action.performAction(panel, result);
                panel.frame().showBasePanel(panel);
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
        openFiles(filesToOpen, true);
    }

    /**
     *
     * @return Path of current panel database directory or the working directory
     */
    private Path getInitialDirectory() {
        if (frame.getBasePanelCount() == 0) {
            return Globals.prefs.getWorkingDir();
        } else {
            Optional<Path> databasePath = frame.getCurrentBasePanel().getBibDatabaseContext().getDatabasePath();
            return databasePath.map(Path::getParent).orElse(Globals.prefs.getWorkingDir());
        }
    }

    /**
     * Opens the given file. If null or 404, nothing happens
     *
     * @param file the file, may be null or not existing
     */
    public void openFile(Path file, boolean raisePanel) {
        openFiles(Arrays.asList(file), raisePanel);
    }

    /**
     * Opens the given files. If one of it is null or 404, nothing happens
     *
     * @param filesToOpen the filesToOpen, may be null or not existing
     */
    public void openFiles(List<Path> filesToOpen, boolean raisePanel) {
        BasePanel toRaise = null;
        int initialCount = filesToOpen.size();
        int removed = 0;

        // Check if any of the files are already open:
        for (Iterator<Path> iterator = filesToOpen.iterator(); iterator.hasNext();) {
            Path file = iterator.next();
            for (int i = 0; i < frame.getTabbedPane().getTabs().size(); i++) {
                BasePanel basePanel = frame.getBasePanelAt(i);
                if ((basePanel.getBibDatabaseContext().getDatabasePath().isPresent())
                    && basePanel.getBibDatabaseContext().getDatabasePath().get().equals(file)) {
                    iterator.remove();
                    removed++;
                    // See if we removed the final one. If so, we must perhaps
                    // raise the BasePanel in question:
                    if (removed == initialCount) {
                        toRaise = basePanel;
                    }
                    // no more bps to check, we found a matching one
                    break;
                }
            }
        }

        // Run the actual open in a thread to prevent the program
        // locking until the file is loaded.
        if (!filesToOpen.isEmpty()) {
            final List<Path> theFiles = Collections.unmodifiableList(filesToOpen);

            for (Path theFile : theFiles) {
                //This method will execute the concrete file opening and loading in a background thread
                openTheFile(theFile, raisePanel);
            }

            for (Path theFile : theFiles) {
                frame.getFileHistory().newFile(theFile);
            }
        }
        // If no files are remaining to open, this could mean that a file was
        // already open. If so, we may have to raise the correct tab:
        else if (toRaise != null) {
            frame.showBasePanel(toRaise);
        }
    }

    /**
     * @param file the file, may be null or not existing
     */
    private void openTheFile(Path file, boolean raisePanel) {
        Objects.requireNonNull(file);
        if (Files.exists(file)) {

            BackgroundTask.wrap(() -> loadDatabase(file))
                          .onSuccess(result -> {
                              BasePanel panel = addNewDatabase(result, file, raisePanel);
                              OpenDatabaseAction.performPostOpenActions(panel, result);
                          })
                          .onFailure(ex -> dialogService.showErrorDialogAndWait(Localization.lang("Connection error"),
                                                                                           ex.getMessage() + "\n\n" + Localization.lang("A local copy will be opened.")))
                          .executeWith(Globals.TASK_EXECUTOR);
        }

    }

    private ParserResult loadDatabase(Path file) throws Exception {
        Path fileToLoad = file.toAbsolutePath();

        dialogService.notify(Localization.lang("Opening") + ": '" + file + "'");

        Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, fileToLoad.getParent().toString());

        if (BackupManager.checkForBackupFile(fileToLoad)) {
            BackupUIManager.showRestoreBackupDialog(dialogService, fileToLoad);
        }

        ParserResult result = OpenDatabase.loadDatabase(fileToLoad.toString(),
                                                        Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());

        if (result.getDatabase().isShared()) {
            try {
                new SharedDatabaseUIManager(frame).openSharedDatabaseFromParserResult(result);
            } catch (SQLException | DatabaseNotSupportedException | InvalidDBMSConnectionPropertiesException |
                     NotASharedDatabaseException e) {
                result.getDatabaseContext().clearDatabaseFile(); // do not open the original file
                result.getDatabase().clearSharedDatabaseID();
                LOGGER.error("Connection error", e);

                throw e;

            }
        }
        return result;

    }

    private BasePanel addNewDatabase(ParserResult result, final Path file, boolean raisePanel) {
        if (result.hasWarnings()) {
            ParserResultWarningDialog.showParserResultWarningDialog(result, frame);
        }

        BasePanel basePanel = new BasePanel(frame, BasePanelPreferences.from(Globals.prefs), result.getDatabaseContext(), ExternalFileTypes.getInstance());
        frame.addTab(basePanel, raisePanel);
        return basePanel;

    }
}
