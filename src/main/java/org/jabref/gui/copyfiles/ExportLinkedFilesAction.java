package org.jabref.gui.copyfiles;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;

import javafx.concurrent.Task;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.actions.ExportLinkedFilesTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class ExportLinkedFilesAction extends AbstractAction {

    private final DialogService dialogService = new FXDialogService();
    private BibDatabaseContext databaseContext;
    private List<BibEntry> entries;

    public ExportLinkedFilesAction() {
        super(Localization.lang("Copy attached files to folder..."));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Paths.get(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY)))
                .build();
        entries = JabRefGUI.getMainFrame().getCurrentBasePanel().getSelectedEntries();

        Optional<Path> exportPath = DefaultTaskExecutor
                .runInJavaFXThread(() -> dialogService.showDirectorySelectionDialog(dirDialogConfiguration));

        exportPath.ifPresent(path -> {
            databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getDatabaseContext();

            Task<List<CopyFilesResultItemViewModel>> exportTask = new ExportLinkedFilesTask(databaseContext, entries, path);
            startServiceAndshowProgessDialog(exportTask);
        });
    }

    private void startServiceAndshowProgessDialog(Task<List<CopyFilesResultItemViewModel>> exportService) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            exportService.setOnSucceeded(value -> {
                DefaultTaskExecutor.runInJavaFXThread(() -> showDialog(exportService.getValue()));
            });
            exportService.run();
            dialogService.showCanceableProgressDialogAndWait(exportService);

        });
    }

    private void showDialog(List<CopyFilesResultItemViewModel> data) {
        CopyFilesDialogView dlg = new CopyFilesDialogView(databaseContext, new CopyFilesResultListDependency(data));
        dlg.show();
    }
}