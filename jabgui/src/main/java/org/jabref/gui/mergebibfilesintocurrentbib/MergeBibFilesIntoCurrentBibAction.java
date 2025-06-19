package org.jabref.gui.mergebibfilesintocurrentbib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class MergeBibFilesIntoCurrentBibAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeBibFilesIntoCurrentBibAction.class);

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;

    private boolean shouldMergeSameKeyEntries;
    private boolean shouldMergeDuplicateEntries;

    private final List<BibEntry> entriesToMerge = new ArrayList<>();
    private final List<List<BibEntry>> duplicatePairsToMerge = new ArrayList<>();
    private final List<List<BibEntry>> sameKeyPairsToMerge = new ArrayList<>();

    public MergeBibFilesIntoCurrentBibAction(DialogService dialogService,
                                             GuiPreferences preferences,
                                             StateManager stateManager,
                                             UndoManager undoManager,
                                             FileUpdateMonitor fileUpdateMonitor,
                                             BibEntryTypesManager entryTypesManager) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;

        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        Optional<Path> selectedDirectory = getDirectoryToMerge();
        Optional<BibDatabaseContext> context = stateManager.getActiveDatabase();

        MergeBibFilesIntoCurrentBibPreferences mergeBibFilesIntoCurrentBibPreferences = preferences.getMergeBibFilesIntoCurrentBibPreferences();

        shouldMergeSameKeyEntries = mergeBibFilesIntoCurrentBibPreferences.shouldMergeSameKeyEntries();
        shouldMergeDuplicateEntries = mergeBibFilesIntoCurrentBibPreferences.shouldMergeDuplicateEntries();

        if (selectedDirectory.isPresent() && context.isPresent()) {
            mergeBibFilesIntoCurrentBib(selectedDirectory.get(), context.get());
        }
    }

    private Optional<Path> getDirectoryToMerge() {
        DirectoryDialogConfiguration config = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();

        return dialogService.showDirectorySelectionDialog(config);
    }

    public void mergeBibFilesIntoCurrentBib(Path directory, BibDatabaseContext context) {
        BibDatabase database = context.getDatabase();
        Optional<Path> databasePath = context.getDatabasePath();
        DuplicateCheck duplicateCheck = new DuplicateCheck(entryTypesManager);

        entriesToMerge.clear();
        sameKeyPairsToMerge.clear();
        duplicatePairsToMerge.clear();

        for (Path path : getAllBibFiles(directory, databasePath.orElseGet(() -> Path.of("")))) {
            ParserResult result;
            try {
                result = OpenDatabase.loadDatabase(path, preferences.getImportFormatPreferences(), fileUpdateMonitor);
            } catch (IOException e) {
                LOGGER.error("Could not load file '{}': {}", path, e.getMessage());
                continue;
            }
            for (BibEntry toMergeEntry : result.getDatabase().getEntries()) {
                processEntry(toMergeEntry, database, duplicateCheck);
            }
        }

        database.insertEntries(entriesToMerge);
        performMerges();

        NamedCompound compound = new NamedCompound(Localization.lang("Merge BibTeX files into current library"));
        compound.addEdit(new UndoableInsertEntries(database, entriesToMerge));
        compound.end();
        undoManager.addEdit(compound);
    }

    private void processEntry(BibEntry entry, BibDatabase database, DuplicateCheck duplicateCheck) {
        for (BibEntry existingEntry : database.getEntries()) {
            if (entry.equals(existingEntry)) {
                return;
            } else if (entry.getCitationKey().equals(existingEntry.getCitationKey())) {
                if (shouldMergeSameKeyEntries) {
                    sameKeyPairsToMerge.add(List.of(entry, existingEntry));
                }
                return;
            } else if (duplicateCheck.isDuplicate(entry, existingEntry, BibDatabaseMode.BIBTEX)) {
                if (shouldMergeDuplicateEntries) {
                    duplicatePairsToMerge.add(List.of(entry, existingEntry));
                }
                return;
            }
        }
        entriesToMerge.add(entry);
    }

    private void performMerges() {
        for (List<BibEntry> pair : sameKeyPairsToMerge) {
            mergeEntries(pair);
        }
        for (List<BibEntry> pair : duplicatePairsToMerge) {
            mergeEntries(pair);
        }
    }

    private void mergeEntries(List<BibEntry> entries) {
        stateManager.setSelectedEntries(entries);
        new MergeEntriesAction(dialogService, stateManager, undoManager, preferences).execute();
    }

    private List<Path> getAllBibFiles(Path directory, Path databasePath) {
        if (!checkPathValidity(directory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.find(
                directory,
                Integer.MAX_VALUE,
                (path, _) -> path.getFileName().toString().endsWith(".bib") &&
                        !path.equals(databasePath)
        )) {
            return stream.collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error finding .bib files in '{}': {}", directory.getFileName(), e.getMessage());
        }
        return List.of();
    }

    private boolean checkPathValidity(Path directory) {
        if (!Files.exists(directory)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Chosen folder does not exist:") + " " + directory);
            return false;
        }
        if (!Files.isDirectory(directory)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Chosen path is not a folder:") + " " + directory);
            return false;
        }
        if (!Files.isReadable(directory)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Chosen folder is not readable:") + " " + directory);
            return false;
        }
        return true;
    }
}
