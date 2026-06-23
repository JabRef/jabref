package org.jabref.gui.preferences.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.OcrPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcrTabViewModel implements PreferenceTabViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrTabViewModel.class);
    private static final int CHECKING_TIMEOUT = 10;
    private static final List<String> DEFAULT_OCR_PATHS = List.of(
            "ocrmypdf",
            "python -m ocrmypdf",
            "py -m ocrmypdf",
            "python3 -m ocrmypdf"
    );
    private final StringProperty ocrPath = new SimpleStringProperty();

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final OcrPreferences ocrPreferences;
    private final TaskExecutor taskExecutor;

    public OcrTabViewModel(DialogService dialogService,
                           FilePreferences filePreferences,
                           OcrPreferences ocrPreferences,
                           TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.ocrPreferences = ocrPreferences;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void setValues() {
        ocrPath.setValue(ocrPreferences.getOcrPath());
    }

    @Override
    public void storeSettings() {
        ocrPreferences.setOcrPath(ocrPath.getValue());
    }

    public StringProperty ocrPathProperty() {
        return ocrPath;
    }

    public void browseEnginePath() {
        Optional<Path> selectedPath = dialogService.showFileOpenDialog(
                new FileDialogConfiguration.Builder()
                        .withInitialDirectory(filePreferences.getWorkingDirectory())
                        .build());
        selectedPath.ifPresent(path -> ocrPathProperty().set(path.toString()));
    }

    public Optional<String> autoDetectDefaultEnginePath() {
        return DEFAULT_OCR_PATHS.stream()
                                .filter(this::pathExists)
                                .findFirst();
    }

    public void autoDetectEnginePath() {
        BackgroundTask<Optional<String>> autoDetectTask = BackgroundTask.wrap(this::autoDetectDefaultEnginePath);

        autoDetectTask.titleProperty().set(Localization.lang("Auto detection of engine path"));
        autoDetectTask.showToUser(true);
        autoDetectTask.onSuccess(result -> {
            if (result.isPresent()) {
                String path = result.get();
                ocrPathProperty().set(path);
                dialogService.notify(Localization.lang("OCRmyPDF detected at: %0", path));
            } else {
                dialogService.notify(Localization.lang("OCRmyPDF could not be detected automatically"));
            }
        });
        autoDetectTask.onFailure(_ -> dialogService.notify(Localization.lang("Auto detect engine path failed")));
        taskExecutor.execute(autoDetectTask);
    }

    private boolean pathExists(String path) {
        ArrayList<String> command = StringUtil.splitRespectingEscapedWhitespace(ocrPreferences.getOcrPath());
        command.add("--version");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Get the output and the errors of the process
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);

            boolean finished = process.waitFor(CHECKING_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.debug("Auto-detecting {} as path timed out", path);
                return false;
            }
            return process.exitValue() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Auto detection of {} as engine's path was interrupted", path, e);
            return false;
        } catch (IOException e) {
            LOGGER.debug("{} is not available as engine's path: IOException occurred", path, e);
            return false;
        }
    }
}
