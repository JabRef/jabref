package org.jabref.gui.actions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;

import org.jabref.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.OptionalUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExportLinkedFilesService extends Service<Void> {

    private static final Log LOGGER = LogFactory.getLog(ExportLinkedFilesAction.class);
    private static final String LOGFILE = "exportLog.log";
    private final String localizedSucessMessage = Localization.lang("Copied file successfully");
    private final String localizedErrorMessage = Localization.lang("Could not copy file") + ": " + Localization.lang("File exists");
    private final long totalFilesCount;
    private final BibDatabaseContext databaseContext;
    private final List<BibEntry> entries;
    private final Path exportPath;

    private final BiFunction<Path, Path, Path> resolvePathFilename = (path, file) -> {
        return path.resolve(file.getFileName());
    };

    public ExportLinkedFilesService(BibDatabaseContext databaseContext, List<BibEntry> entries, Path path) {
        this.databaseContext = databaseContext;
        this.entries = entries;
        this.exportPath = path;
        totalFilesCount = entries.stream().flatMap(entry -> entry.getFiles().stream()).count();

    }

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

                try (BufferedWriter bw = Files.newBufferedWriter(exportPath.resolve(LOGFILE), StandardCharsets.UTF_8)) {

                    for (int i = 0; i < entries.size(); i++) {

                        List<LinkedFile> files = entries.get(i).getFiles();

                        for (int j = 0; j < files.size(); j++) {
                            updateMessage(Localization.lang("Copying file %0 of entry %1", Integer.toString(j + 1), Integer.toString(i + 1)));

                            LinkedFile fileName = files.get(j);

                            Optional<Path> fileToExport = fileName.findIn(databaseContext, Globals.prefs.getFileDirectoryPreferences());

                            newPath = OptionalUtil.combine(Optional.of(exportPath), fileToExport, resolvePathFilename);

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
                    showDialog();
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

    private void showDialog() {
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Results");
        ListView<String> lv = new ListView<>();
        dlg.getDialogPane().setContent(lv);
    }
}
