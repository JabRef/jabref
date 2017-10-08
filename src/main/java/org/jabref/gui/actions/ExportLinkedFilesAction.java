package org.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;

import javafx.concurrent.Service;
import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class ExportLinkedFilesAction extends AbstractAction {


    private final DialogService ds = new FXDialogService();
    private long totalFilesCount;
    private BibDatabaseContext databaseContext;
    private Optional<Path> exportPath = Optional.empty();
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
        exportPath = DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showDirectorySelectionDialog(dirDialogConfiguration));

        exportPath.ifPresent(path -> {
            databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getDatabaseContext();

            Service<Void> exportService = new ExportLinkedFilesService(databaseContext, entries, path);
            startServiceAndshowProgessDialog(exportService);
        });
    }

    private <V> void startServiceAndshowProgessDialog(Service<V> service) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            service.start();

            DialogService dlgService = new FXDialogService();
            dlgService.showCanceableProgressDialogAndWait(service);

        });
    }

}
