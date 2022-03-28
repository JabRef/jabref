package org.jabref.gui.copyfiles;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import javafx.concurrent.Task;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyFilesTask extends Task<List<CopyFilesResultItemViewModel>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyFilesAction.class);
    private static final String LOGFILE_PREFIX = "copyFileslog_";
    private static final String LOGFILE_EXT = ".log";
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final Path exportPath;
    private final String localizedSucessMessage = Localization.lang("Copied file successfully");
    private final String localizedErrorMessage = Localization.lang("Could not copy file") + ": " + Localization.lang("File exists");
    private final long totalFilesCount;
    private final List<BibEntry> entries;
    private final List<CopyFilesResultItemViewModel> results = new ArrayList<>();
    private Optional<Path> newPath = Optional.empty();
    private int numberSuccessful;
    private int totalFilesCounter;

    private final BiFunction<Path, Path, Path> resolvePathFilename = (path, file) -> path.resolve(file.getFileName());

    public CopyFilesTask(BibDatabaseContext databaseContext, List<BibEntry> entries, Path path, PreferencesService preferencesService) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.entries = entries;
        this.exportPath = path;
        totalFilesCount = entries.stream().mapToLong(entry -> entry.getFiles().size()).sum();
    }

    @Override
    protected List<CopyFilesResultItemViewModel> call() throws InterruptedException, IOException {

        updateMessage(Localization.lang("Copying files..."));
        updateProgress(0, totalFilesCount);

        LocalDateTime currentTime = LocalDateTime.now();
        String currentDate = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

        try (BufferedWriter bw = Files.newBufferedWriter(exportPath.resolve(LOGFILE_PREFIX + currentDate + LOGFILE_EXT), StandardCharsets.UTF_8)) {

            for (int i = 0; i < entries.size(); i++) {

                if (isCancelled()) {
                    break;
                }

                List<LinkedFile> files = entries.get(i).getFiles();

                for (int j = 0; j < files.size(); j++) {

                    if (isCancelled()) {
                        break;
                    }

                    updateMessage(Localization.lang("Copying file %0 of entry %1", Integer.toString(j + 1), Integer.toString(i + 1)));

                    LinkedFile fileName = files.get(j);

                    Optional<Path> fileToExport = fileName.findIn(databaseContext, preferencesService.getFilePreferences());

                    newPath = OptionalUtil.combine(Optional.of(exportPath), fileToExport, resolvePathFilename);

                    if (newPath.isPresent()) {

                        Path newFile = newPath.get();
                        boolean success = FileUtil.copyFile(fileToExport.get(), newFile, false);
                        updateProgress(totalFilesCounter++, totalFilesCount);
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            if (isCancelled()) {
                                updateMessage("Cancelled");
                                break;
                            }
                        }
                        if (success) {
                            updateMessage(localizedSucessMessage);
                            numberSuccessful++;
                            writeLogMessage(newFile, bw, localizedSucessMessage);
                            addResultToList(newFile, success, localizedSucessMessage);
                        } else {

                            updateMessage(localizedErrorMessage);
                            writeLogMessage(newFile, bw, localizedErrorMessage);
                            addResultToList(newFile, success, localizedErrorMessage);
                        }
                    }
                }
            }
            updateMessage(Localization.lang("Finished copying"));

            String sucessMessage = Localization.lang("Copied %0 files of %1 sucessfully to %2",
                    Integer.toString(numberSuccessful),
                    Integer.toString(totalFilesCounter),
                    newPath.map(Path::getParent).map(Path::toString).orElse(""));
            updateMessage(sucessMessage);
            bw.write(sucessMessage);
            return results;
        }
    }

    private void writeLogMessage(Path newFile, BufferedWriter bw, String logMessage) {
        try {
            bw.write(logMessage + ": " + newFile);
            bw.write(OS.NEWLINE);
        } catch (IOException e) {
            LOGGER.error("error writing log file", e);
        }
    }

    private void addResultToList(Path newFile, boolean success, String logMessage) {
        CopyFilesResultItemViewModel result = new CopyFilesResultItemViewModel(newFile, success, logMessage);
        results.add(result);
    }
}
