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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.ProgressDialog;

public class ExportLinkedFilesAction extends AbstractAction {

    private static final Log LOGGER = LogFactory.getLog(ExportLinkedFilesAction.class);
    private final BiFunction<Path, Path, Path> resolvePathFilename = (path, file) -> {
        return path.resolve(file.getFileName());
    };
    private final DialogService ds = new FXDialogService();
    private long totalFilesCount;
    private BibDatabaseContext databaseContext;
    private Optional<Path> exportPath;
    private List<BibEntry> entries;

    public ExportLinkedFilesAction() {
        super(Localization.lang("Export linked files"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Paths.get(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY)))
                .build();
        entries = JabRefGUI.getMainFrame().getCurrentBasePanel().getSelectedEntries();
        exportPath = DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showDirectorySelectionDialog(dirDialogConfiguration));
        databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getDatabaseContext();
        totalFilesCount = entries.stream().flatMap(entry -> entry.getFiles().stream()).count();

        Service<Void> exportService = new ExportService();
        startServiceAndshowProgessDialog(exportService);
    }

    private <V> void startServiceAndshowProgessDialog(Service<V> service) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            service.start();

            ProgressDialog progressDialog = new ProgressDialog(service);
            progressDialog.setOnCloseRequest(evt -> service.cancel());
            DialogPane dialogPane = progressDialog.getDialogPane();
            dialogPane.getButtonTypes().add(ButtonType.CANCEL);
            Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            cancelButton.setOnAction(evt -> {
                service.cancel();
                progressDialog.close();
            });
            progressDialog.showAndWait();
        });
    }

    private class ExportService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {

                int totalFilesCounter;
                Optional<Path> newPath;

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

                            newPath = OptionalUtil.combine(exportPath, fileToExport, resolvePathFilename);

                            newPath.ifPresent(newFile -> {
                                boolean success = FileUtil.copyFile(fileToExport.get(), newFile, false);
                                updateProgress(totalFilesCounter++, totalFilesCount);
                                if (success) {
                                    updateMessage(Localization.lang("Exported file successfully"));
                                } else {
                                    updateMessage(Localization.lang("Could not export file") + " " + Localization.lang("File exists"));
                                }
                            });
                        }
                    }
                    updateMessage(Localization.lang("Finished exporting"));
                    updateMessage(Localization.lang("Exported %0 files sucessfully to %1", Integer.toString(totalFilesCounter), newPath.map(Path::toString).orElse("")));
                    return null;
                }
            };
        }
    }
}
