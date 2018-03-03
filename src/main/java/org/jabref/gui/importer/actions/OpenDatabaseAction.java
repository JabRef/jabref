package org.jabref.gui.importer.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.MnemonicAwareAction;
import org.jabref.gui.dialogs.BackupUIManager;
import org.jabref.gui.importer.ParserResultWarningDialog;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.io.FileBasedLock;
import org.jabref.migrations.FileLinksUpgradeWarning;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The action concerned with opening an existing database.
public class OpenDatabaseAction extends MnemonicAwareAction {

    public static final Logger LOGGER = LoggerFactory.getLogger(OpenDatabaseAction.class);
    // List of actions that may need to be called after opening the file. Such as
    // upgrade actions etc. that may depend on the JabRef version that wrote the file:
    private static final List<GUIPostOpenAction> POST_OPEN_ACTIONS = Arrays.asList(
            // Migrations:
            // Warning for migrating the Review into the Comment field
            new MergeReviewIntoCommentAction(),
            // External file handling system in version 2.3:
            new FileLinksUpgradeWarning(),

            // Check for new custom entry types loaded from the BIB file:
            new CheckForNewEntryTypesAction(),
            // Warning about and handling duplicate BibTeX keys:
            new HandleDuplicateWarnings());


    private final boolean showDialog;
    private final JabRefFrame frame;

    public OpenDatabaseAction(JabRefFrame frame, boolean showDialog) {
        super(IconTheme.JabRefIcon.OPEN.getIcon());
        this.frame = frame;
        this.showDialog = showDialog;
        putValue(Action.NAME, Localization.menuTitle("Open library"));
        putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.OPEN_DATABASE));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Open BibTeX library"));
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
                panel.frame().getTabbedPane().setSelectedComponent(panel);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<Path> filesToOpen = new ArrayList<>();

        if (showDialog) {
            DialogService ds = new FXDialogService();
            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(FileType.BIBTEX_DB)
                    .withDefaultExtension(FileType.BIBTEX_DB)
                    .withInitialDirectory(Paths.get(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)))
                    .build();

            List<Path> chosenFiles = DefaultTaskExecutor
                    .runInJavaFXThread(() -> ds.showFileOpenDialogAndGetMultipleFiles(fileDialogConfiguration));
            filesToOpen.addAll(chosenFiles);
        } else {
            LOGGER.info(Action.NAME + " " + e.getActionCommand());
            filesToOpen.add(Paths.get(StringUtil.getCorrectFileName(e.getActionCommand(), "bib")));
        }

        openFiles(filesToOpen, true);
    }

    /**
     * Opens the given file. If null or 404, nothing happens
     *
     * @param file the file, may be null or not existing
     */
    public void openFile(Path file, boolean raisePanel) {
        List<Path> filesToOpen = new ArrayList<>();
        filesToOpen.add(file);
        openFiles(filesToOpen, raisePanel);
    }

    public void openFilesAsStringList(List<String> fileNamesToOpen, boolean raisePanel) {
        List<Path> filesToOpen = fileNamesToOpen.stream().map(Paths::get).collect(Collectors.toList());

        openFiles(filesToOpen, raisePanel);
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
        for (Iterator<Path> iterator = filesToOpen.iterator(); iterator.hasNext(); ) {
            Path file = iterator.next();
            for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
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
            JabRefExecutorService.INSTANCE.execute(() -> {
                for (Path theFile : theFiles) {
                    openTheFile(theFile, raisePanel);
                }
            });
            for (Path theFile : theFiles) {
                frame.getFileHistory().newFile(theFile.toString());
            }
        }
        // If no files are remaining to open, this could mean that a file was
        // already open. If so, we may have to raise the correct tab:
        else if (toRaise != null) {
            frame.output(Localization.lang("File '%0' is already open.",
                    toRaise.getBibDatabaseContext().getDatabaseFile().get().getPath()));
            frame.getTabbedPane().setSelectedComponent(toRaise);
        }

        frame.output(Localization.lang("Files opened") + ": " + (filesToOpen.size()));
    }

    /**
     * @param file the file, may be null or not existing
     */
    private void openTheFile(Path file, boolean raisePanel) {
        Objects.requireNonNull(file);
        if (Files.exists(file)) {
            Path fileToLoad = file.toAbsolutePath();

            frame.output(Localization.lang("Opening") + ": '" + file + "'");

            String fileName = file.getFileName().toString();
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, fileToLoad.getParent().toString());

            if (FileBasedLock.hasLockFile(file)) {
                Optional<FileTime> modificationTime = FileBasedLock.getLockFileTimeStamp(file);
                if ((modificationTime.isPresent()) && ((System.currentTimeMillis()
                        - modificationTime.get().toMillis()) > FileBasedLock.LOCKFILE_CRITICAL_AGE)) {
                    // The lock file is fairly old, so we can offer to "steal" the file:
                    int answer = JOptionPane.showConfirmDialog(null,
                            "<html>" + Localization.lang("Error opening file") + " '" + fileName + "'. "
                                    + Localization.lang("File is locked by another JabRef instance.") + "<p>"
                                    + Localization.lang("Do you want to override the file lock?"),
                            Localization.lang("File locked"), JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        FileBasedLock.deleteLockFile(file);
                    } else {
                        return;
                    }
                } else if (!FileBasedLock.waitForFileLock(file)) {
                    JOptionPane.showMessageDialog(null,
                            Localization.lang("Error opening file") + " '" + fileName + "'. "
                                    + Localization.lang("File is locked by another JabRef instance."),
                            Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            if (BackupManager.checkForBackupFile(fileToLoad)) {
                BackupUIManager.showRestoreBackupDialog(frame, fileToLoad);
            }

            ParserResult result;
            result = OpenDatabase.loadDatabase(fileToLoad.toString(),
                    Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());

            if (result.getDatabase().isShared()) {
                try {
                    new SharedDatabaseUIManager(frame).openSharedDatabaseFromParserResult(result);
                } catch (SQLException | DatabaseNotSupportedException | InvalidDBMSConnectionPropertiesException |
                        NotASharedDatabaseException e) {
                    result.getDatabaseContext().clearDatabaseFile(); // do not open the original file
                    result.getDatabase().clearSharedDatabaseID();
                    LOGGER.error("Connection error", e);
                    JOptionPane.showMessageDialog(frame,
                            e.getMessage() + "\n\n" + Localization.lang("A local copy will be opened."),
                            Localization.lang("Connection error"), JOptionPane.WARNING_MESSAGE);
                }
            }

            BasePanel panel = addNewDatabase(result, file, raisePanel);

            // After adding the database, go through our list and see if
            // any post open actions need to be done. For instance, checking
            // if we found new entry types that can be imported, or checking
            // if the database contents should be modified due to new features
            // in this version of JabRef:
            final ParserResult finalReferenceToResult = result;
            SwingUtilities.invokeLater(() -> OpenDatabaseAction.performPostOpenActions(panel, finalReferenceToResult));
        }
    }

    private BasePanel addNewDatabase(ParserResult result, final Path file, boolean raisePanel) {

        BibDatabase database = result.getDatabase();

        if (result.hasWarnings()) {
            JabRefExecutorService.INSTANCE
                    .execute(() -> ParserResultWarningDialog.showParserResultWarningDialog(result, frame));
        }

        BasePanel basePanel = new BasePanel(frame, result.getDatabaseContext());

        // file is set to null inside the EventDispatcherThread
        SwingUtilities.invokeLater(() -> frame.addTab(basePanel, raisePanel));

        if (Objects.nonNull(file)) {
            frame.output(Localization.lang("Opened library") + " '" + file.toString() + "' "
                    + Localization.lang("with")
                    + " "
                    + database.getEntryCount() + " " + Localization.lang("entries") + ".");
        }

        return basePanel;
    }
}
