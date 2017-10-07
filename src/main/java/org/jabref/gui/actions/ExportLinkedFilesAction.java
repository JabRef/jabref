package org.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import org.jabref.logic.util.OS;
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
            totalFilesCount = entries.stream().flatMap(entry -> entry.getFiles().stream()).count();

            Service<Void> exportService = new ExportService();
            startServiceAndshowProgessDialog(exportService);
        });
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

        private static final String LOGFILE = "exportLog.log";
        private final String localizedSucessMessage = Localization.lang("Copied file successfully");
        private final String localizedErrorMessage = Localization.lang("Could not copy file") + ": " + Localization.lang("File exists");

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {

                int totalFilesCounter;
                int numberSucessful;
                int numberError;
                Optional<Path> newPath;

                @Override
                protected Void call()
                        throws InterruptedException, IOException {
                    updateMessage(Localization.lang("Copying files..."));
                    updateProgress(0, totalFilesCount);

                    try (BufferedWriter bw = Files.newBufferedWriter(exportPath.get().resolve(LOGFILE), StandardCharsets.UTF_8)) {

                        for (int i = 0; i < entries.size(); i++) {

                            List<LinkedFile> files = entries.get(i).getFiles();

                            for (int j = 0; j < files.size(); j++) {
                                updateMessage(Localization.lang("Copying file %0 of BibEntry %1", Integer.toString(j + 1), Integer.toString(i + 1)));
                                Thread.sleep(500); //TODO: Adjust/leave/any other idea?

                                String fileName = files.get(j).getLink();
                                Optional<Path> fileToExport = FileHelper.expandFilenameAsPath(fileName,
                                        databaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences()));

                                newPath = OptionalUtil.combine(exportPath, fileToExport, resolvePathFilename);

                                newPath.ifPresent(newFile -> {
                                    boolean success = FileUtil.copyFile(fileToExport.get(), newFile, false);
                                    updateProgress(totalFilesCounter++, totalFilesCount);
                                    if (success) {
                                        updateMessage(localizedSucessMessage);
                                        numberSucessful++;
                                        writeLogMessage(newFile, bw, localizedSucessMessage);

                                    } else {

                                        updateMessage(localizedErrorMessage);
                                        numberError++;
                                        writeLogMessage(newFile, bw, localizedErrorMessage);
                                    }
                                });
                            }
                        }
                        updateMessage(Localization.lang("Finished copying"));
                        String sucessMessage = Localization.lang("Copied %0 files of %1 sucessfully to %2", Integer.toString(numberSucessful), Integer.toString(totalFilesCounter), newPath.map(Path::getParent).map(Path::toString).orElse(""));
                        updateMessage(sucessMessage);
                        bw.write(sucessMessage);
                        return null;
                    }
                }
            };
        }

        private void writeLogMessage(Path newFile, BufferedWriter bw, String logMessage) {
            try {
                bw.write(logMessage + ": " + newFile);
                bw.write(OS.NEWLINE);
            } catch (IOException e) {
                LOGGER.error("error writing log file", e);
            }
        }

    }

}
