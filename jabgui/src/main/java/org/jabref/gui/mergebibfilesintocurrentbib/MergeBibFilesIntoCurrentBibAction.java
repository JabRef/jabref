package org.jabref.gui.mergebibfilesintocurrentbib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.undo.UndoManager;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.dialogs.BackupUIManager;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.shared.SharedDatabaseUIManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/**
 * Perform a merge libraries (.bib files) in folder into current library action
 */
public class MergeBibFilesIntoCurrentBibAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeBibFilesIntoCurrentBibAction.class);

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final AiService aiService;
    private final BibEntryTypesManager entryTypesManager;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;

    private boolean shouldMergeSameKeyEntries;
    private boolean shouldMergeDuplicateEntries;

    public MergeBibFilesIntoCurrentBibAction(LibraryTabContainer tabContainer,
                                             DialogService dialogService,
                                             GuiPreferences preferences,
                                             StateManager stateManager,
                                             UndoManager undoManager,
                                             FileUpdateMonitor fileUpdateMonitor,
                                             AiService aiService,
                                             BibEntryTypesManager entryTypesManager,
                                             ClipBoardManager clipBoardManager,
                                             TaskExecutor taskExecutor) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.aiService = aiService;
        this.entryTypesManager = entryTypesManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        Optional<Path> selectedDirectory = getDirectoryToMerge();
        Optional<BibDatabaseContext> context = stateManager.getActiveDatabase();

        MergeBibFilesIntoCurrentBibPreferences mergeBibFilesIntoCurrentBibPreferences = preferences.getMergeBibFilesIntoCurrentBibPreferences();

        shouldMergeSameKeyEntries = mergeBibFilesIntoCurrentBibPreferences.getShouldMergeSameKeyEntries();
        shouldMergeDuplicateEntries = mergeBibFilesIntoCurrentBibPreferences.getShouldMergeDuplicateEntries();

        if (selectedDirectory.isPresent() && context.isPresent()) {
            mergeBibFilesIntoCurrentBib(selectedDirectory.get(), context.get());
        }
    }

    public Optional<Path> getDirectoryToMerge() {
        DirectoryDialogConfiguration config = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();

        return dialogService.showDirectorySelectionDialog(config);
    }

    public void mergeBibFilesIntoCurrentBib(Path directory, BibDatabaseContext context) {
        List<BibEntry> newEntries = new ArrayList<>();
        List<BibEntry> selectedEntries;

        BibDatabase database = context.getDatabase();
        Optional<Path> databasePath = context.getDatabasePath();

        BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();
        DuplicateCheck dupCheck = new DuplicateCheck(entryTypesManager);

        for (Path p : getAllBibFiles(directory, databasePath.orElseGet(() -> Path.of("")))) {
            ParserResult result = loadDatabase(p);

            for (BibEntry toMergeEntry : result.getDatabase().getEntries()) {
                boolean validNewEntry = true;
                for (BibEntry e : database.getEntries()) {
                    if (toMergeEntry.equals(e)) {
                        validNewEntry = false;
                        break;
                    } else if (toMergeEntry.getCitationKey().equals(e.getCitationKey())) {
                        validNewEntry = false;

                        if (shouldMergeSameKeyEntries) {
                            selectedEntries = new ArrayList<>();
                            selectedEntries.add(toMergeEntry);
                            selectedEntries.add(e);
                            stateManager.setSelectedEntries(selectedEntries);
                            new MergeEntriesAction(dialogService, stateManager, undoManager, preferences).execute();
                        }
                        break;
                    } else if (dupCheck.isDuplicate(toMergeEntry, e, BibDatabaseMode.BIBTEX)) {
                        validNewEntry = false;

                        if (shouldMergeDuplicateEntries) {
                            selectedEntries = new ArrayList<>();
                            selectedEntries.add(toMergeEntry);
                            selectedEntries.add(e);
                            stateManager.setSelectedEntries(selectedEntries);
                            new MergeEntriesAction(dialogService, stateManager, undoManager, preferences).execute();
                        }
                        break;
                    }
                }

                if (validNewEntry) {
                    newEntries.add(toMergeEntry);
                    database.insertEntry(toMergeEntry);
                }
            }
        }
        NamedCompound ce = new NamedCompound(Localization.lang("Merge bib files into current bib"));
        ce.addEdit(new UndoableInsertEntries(database, newEntries));
        ce.end();

        undoManager.addEdit(ce);
    }

    public List<Path> getAllBibFiles(Path directory, Path databasePath) {
        try (Stream<Path> stream = Files.find(
                directory,
                Integer.MAX_VALUE,
                (path, _) -> path.getFileName().toString().endsWith(".bib") &&
                        !path.equals(databasePath)
        )) {
            return stream.collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error finding .bib files in '{}'", directory.getFileName(), e);
        }
        return List.of();
    }

    public ParserResult loadDatabase(Path file) {
        Path fileToLoad = file.toAbsolutePath();

        preferences.getFilePreferences().setWorkingDirectory(fileToLoad.getParent());
        Path backupDir = preferences.getFilePreferences().getBackupDirectory();

        ParserResult parserResult = null;
        if (BackupManager.backupFileDiffers(fileToLoad, backupDir)) {
            parserResult = BackupUIManager.showRestoreBackupDialog(dialogService, fileToLoad, preferences, fileUpdateMonitor, undoManager, stateManager)
                                          .orElse(null);
        }

        try {
            if (parserResult == null) {
                parserResult = OpenDatabase.loadDatabase(fileToLoad,
                        preferences.getImportFormatPreferences(),
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
            try {
                new SharedDatabaseUIManager(tabContainer, dialogService, preferences, aiService, stateManager, entryTypesManager,
                        fileUpdateMonitor, undoManager, clipBoardManager, taskExecutor)
                        .openSharedDatabaseFromParserResult(parserResult);
            } catch (SQLException |
                     DatabaseNotSupportedException |
                     InvalidDBMSConnectionPropertiesException |
                     NotASharedDatabaseException e) {
                parserResult.getDatabaseContext().clearDatabasePath();
                parserResult.getDatabase().clearSharedDatabaseID();
                LOGGER.error("Connection error", e);
            }
        }
        return parserResult;
    }
}
