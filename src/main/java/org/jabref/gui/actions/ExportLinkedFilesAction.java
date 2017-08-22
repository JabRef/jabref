package org.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.swing.AbstractAction;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.JabRefPreferences;

import org.controlsfx.dialog.ProgressDialog;

public class ExportLinkedFilesAction extends AbstractAction {

    private final BiFunction<Path, Path, Path> resolvePathFilename = (p, f) -> {
        return p.resolve(f.getFileName());
    };
    private final DialogService ds = new FXDialogService();

    public ExportLinkedFilesAction() {
        super(Localization.lang("Export linked files"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(
                        Paths.get(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY)))
                .build();
        List<BibEntry> entries = JabRefGUI.getMainFrame().getCurrentBasePanel().getSelectedEntries();

        Optional<Path> path = DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showDirectorySelectionDialog(dirDialogConfiguration));

        BibDatabaseContext databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getDatabaseContext();

        long totalFilesCount = entries.stream().flatMap(entry -> entry.getFiles().stream()).count();

        Service<Void> service = new Service<Void>() {

            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {

                    int totalFilesCounter;

                    @Override
                    protected Void call()
                            throws InterruptedException {
                        updateMessage(Localization.lang("Exporting files..."));
                        updateProgress(0, totalFilesCount);

                        for (int i = 0; i < entries.size(); i++) {

                            List<LinkedFile> files = entries.get(i).getFiles();

                            for (int j = 0; j < files.size(); j++) {
                                updateMessage(Localization.lang("Exporting file %0 of BibEntry %1", Integer.toString(j), Integer.toString(i)));
                                Thread.sleep(500); //TODO: Adjust/leave/any other idea?

                                String fileName = files.get(j).getLink();
                                Optional<Path> fileToExport = FileHelper.expandFilenameAsPath(fileName,
                                        databaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences()));

                                Optional<Path> newPath = OptionalUtil.combine(path, fileToExport, resolvePathFilename);

                                newPath.ifPresent(newFile -> {
                                    FileUtil.copyFile(fileToExport.get(), newFile, false);
                                    updateProgress(totalFilesCounter++, totalFilesCount);
                                    updateMessage(Localization.lang("Exported file successfully"));

                                });
                            }
                        }
                        updateMessage(Localization.lang("Finished exporting"));
                        return null;
                    }
                };
            }
        };

        service.start();

        DefaultTaskExecutor.runInJavaFXThread(() -> {
            ProgressDialog dlg = new ProgressDialog(service);
            dlg.setOnCloseRequest(evt -> service.cancel());
            DialogPane dialogPane = dlg.getDialogPane();
            dialogPane.getButtonTypes().add(ButtonType.CANCEL);
            Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            cancelButton.setOnAction(evt -> {
                service.cancel();
                dlg.close();
            });
            dlg.showAndWait();
        });
    }
}
