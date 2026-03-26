package org.jabref.gui.collab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeLibraryAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeLibraryAction.class);

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;

    public MergeLibraryAction(DialogService dialogService,
                              StateManager stateManager,
                              GuiPreferences preferences,
                              TaskExecutor taskExecutor,
                              UndoManager undoManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        Optional<BibDatabaseContext> activeDatabaseOptional = stateManager.getActiveDatabase();
        if (activeDatabaseOptional.isEmpty()) {
            return;
        }

        BibDatabaseContext activeDatabase = activeDatabaseOptional.get();
        Path initialDirectory = activeDatabase.getDatabasePath()
                                              .map(Path::getParent)
                                              .orElse(preferences.getImporterPreferences().getImportWorkingDirectory());

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.toExtensionFilter(Localization.lang("BibTeX"), StandardFileType.BIBTEX_DB))
                .withDefaultExtension(StandardFileType.BIBTEX_DB)
                .withInitialDirectory(initialDirectory)
                .build();

        Optional<Path> selectedFile = dialogService.showFileOpenDialog(fileDialogConfiguration);
        if (selectedFile.isEmpty()) {
            return;
        }

        Path mergeFile = selectedFile.get();
        if (!Files.exists(mergeFile)) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Merge"),
                    Localization.lang("File %0 not found.", mergeFile.getFileName().toString()));
            return;
        }

        BackgroundTask.wrap(() -> scanForChanges(activeDatabase, mergeFile))
                      .onSuccess(changes -> showMergeDialog(activeDatabase, changes))
                      .onFailure(exception -> {
                          LOGGER.warn("Error while reading merge file {}", mergeFile, exception);
                          dialogService.showErrorDialogAndWait(
                                  Localization.lang("Merge"),
                                  Localization.lang("Could not read merge file."));
                      })
                      .executeWith(taskExecutor);
    }

    private List<DatabaseChange> scanForChanges(BibDatabaseContext activeDatabase, Path mergeFile) throws IOException {
        ImportFormatPreferences importFormatPreferences = preferences.getImportFormatPreferences();
        ParserResult result = OpenDatabase.loadDatabase(mergeFile, importFormatPreferences, new DummyFileUpdateMonitor());
        BibDatabaseContext databaseOnDisk = result.getDatabaseContext();

        DatabaseChangeResolverFactory databaseChangeResolverFactory = new DatabaseChangeResolverFactory(dialogService, activeDatabase, preferences, stateManager);
        return DatabaseChangeList.compareAndGetChanges(activeDatabase, databaseOnDisk, databaseChangeResolverFactory);
    }

    private void showMergeDialog(BibDatabaseContext activeDatabase, List<DatabaseChange> changes) {
        DatabaseChangesResolverDialog databaseChangesResolverDialog = new DatabaseChangesResolverDialog(
                changes,
                activeDatabase,
                Localization.lang("External Changes Resolver"));
        dialogService.showCustomDialogAndWait(databaseChangesResolverDialog);

        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Merged external changes"));
        changes.stream()
               .filter(DatabaseChange::isAccepted)
               .forEach(change -> change.applyChange(compoundEdit));
        compoundEdit.end();

        if (compoundEdit.hasEdits()) {
            undoManager.addEdit(compoundEdit);
            stateManager.activeTabProperty()
                        .get()
                        .ifPresent(LibraryTab::markBaseChanged);
        }
    }
}
