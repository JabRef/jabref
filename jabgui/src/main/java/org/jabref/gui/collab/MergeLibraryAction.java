package org.jabref.gui.collab;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeLibraryAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeLibraryAction.class);

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final CountingUndoManager undoManager;
    private final LibraryTabContainer libraryTabContainer;

    public MergeLibraryAction(DialogService dialogService,
                              StateManager stateManager,
                              GuiPreferences preferences,
                              TaskExecutor taskExecutor,
                              CountingUndoManager undoManager,
                              LibraryTabContainer libraryTabContainer) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;
        this.libraryTabContainer = libraryTabContainer;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(activeDatabase -> {
            Path initialDirectory = activeDatabase.getDatabasePath()
                                                  .map(Path::getParent)
                                                  .orElse(preferences.getImporterPreferences().getImportWorkingDirectory());

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(FileFilterConverter.toExtensionFilter(Localization.lang("BibTeX"), StandardFileType.BIBTEX_DB))
                    .withDefaultExtension(StandardFileType.BIBTEX_DB)
                    .withInitialDirectory(initialDirectory)
                    .build();

            dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(mergeFile -> {
                if (!Files.exists(mergeFile)) {
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("Merge"),
                            Localization.lang("File %0 not found.", mergeFile.getFileName().toString()));
                    return;
                }

                ChangeScanner changeScanner = new ChangeScanner(activeDatabase, dialogService, preferences, stateManager);
                BackgroundTask.wrap(() -> changeScanner.getDatabaseChanges(mergeFile))
                              .onSuccess(changes -> showMergeDialog(activeDatabase, changes))
                              .onFailure(exception -> {
                                  LOGGER.warn("Error while reading merge file {}", mergeFile, exception);
                                  dialogService.showErrorDialogAndWait(
                                          Localization.lang("Merge"),
                                          Localization.lang("Could not read merge file."));
                              })
                              .executeWith(taskExecutor);
            });
        });
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

            libraryTabContainer.getLibraryTabs().stream()
                               .filter(tab -> tab.getBibDatabaseContext().equals(activeDatabase))
                               .findFirst()
                               .ifPresent(LibraryTab::markBaseChanged);
        }
    }
}
