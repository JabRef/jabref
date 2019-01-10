package org.jabref.gui.actions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javafx.concurrent.Task;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.copyfiles.CopyFilesDialogView;
import org.jabref.gui.copyfiles.CopyFilesResultItemViewModel;
import org.jabref.gui.copyfiles.CopyFilesResultListDependency;
import org.jabref.gui.copyfiles.CopyFilesTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class CopyFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private BibDatabaseContext databaseContext;
    private List<BibEntry> entries;
    private final JabRefFrame frame;

    public CopyFilesAction(JabRefFrame frame) {
        this.frame = frame;
        this.dialogService = frame.getDialogService();
    }

    private void showDialog(List<CopyFilesResultItemViewModel> data) {
        if (data.isEmpty()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Copy linked files to folder..."), Localization.lang("No linked files found for export."));
            return;
        }
        CopyFilesDialogView dialog = new CopyFilesDialogView(databaseContext, new CopyFilesResultListDependency(data));
        dialog.showAndWait();
    }

    @Override
    public void execute() {
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Paths.get(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY)))
                .build();
        entries = frame.getCurrentBasePanel().getSelectedEntries();

        Optional<Path> exportPath = dialogService.showDirectorySelectionDialog(dirDialogConfiguration);

        exportPath.ifPresent(path -> {
            databaseContext = frame.getCurrentBasePanel().getBibDatabaseContext();

            Task<List<CopyFilesResultItemViewModel>> exportTask = new CopyFilesTask(databaseContext, entries, path);
            dialogService.showProgressDialogAndWait(
                    Localization.lang("Copy linked files to folder..."),
                    Localization.lang("Copy linked files to folder..."),
                    exportTask);
            Globals.TASK_EXECUTOR.execute(exportTask);
            exportTask.setOnSucceeded((e) -> showDialog(exportTask.getValue()));
        });

    }
}
