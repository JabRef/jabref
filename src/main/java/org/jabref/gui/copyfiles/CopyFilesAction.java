package org.jabref.gui.copyfiles;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class CopyFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;

    public CopyFilesAction(DialogService dialogService, PreferencesService preferencesService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(stateManager).and(needsEntriesSelected(stateManager)));
    }

    private void showDialog(List<CopyFilesResultItemViewModel> data) {
        if (data.isEmpty()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Copy linked files to folder..."), Localization.lang("No linked files found for export."));
            return;
        }
        dialogService.showCustomDialogAndWait(new CopyFilesDialogView(new CopyFilesResultListDependency(data)));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        List<BibEntry> entries = stateManager.getSelectedEntries();

        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferencesService.getImportExportPreferences().getExportWorkingDirectory())
                .build();
        Optional<Path> exportPath = dialogService.showDirectorySelectionDialog(dirDialogConfiguration);
        exportPath.ifPresent(path -> {
            Task<List<CopyFilesResultItemViewModel>> exportTask = new CopyFilesTask(database, entries, path, preferencesService);
            dialogService.showProgressDialog(
                    Localization.lang("Copy linked files to folder..."),
                    Localization.lang("Copy linked files to folder..."),
                    exportTask);
            Globals.TASK_EXECUTOR.execute(exportTask);
            exportTask.setOnSucceeded((e) -> showDialog(exportTask.getValue()));
        });
    }
}
