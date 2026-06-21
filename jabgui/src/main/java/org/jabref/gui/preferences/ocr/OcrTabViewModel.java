package org.jabref.gui.preferences.ocr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.ocr.OcrPreferences;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StreamGobbler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcrTabViewModel implements PreferenceTabViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrTabViewModel.class);
    private static final int CHECKING_TIMEOUT = 10;
    private final StringProperty ocrPath = new SimpleStringProperty();

    private final OcrPreferences ocrPreferences;

    public OcrTabViewModel(OcrPreferences ocrPreferences) {
        this.ocrPreferences = ocrPreferences;
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

    Optional<String> autoDetectEnginePath() {
        if (pathExists("ocrmypdf")) {
            return Optional.of("ocrmypdf");
        } else if (pathExists("python -m ocrmypdf")) {
            return Optional.of("python -m ocrmypdf");
        } else if (pathExists("py -m ocrmypdf")) {
            return Optional.of("py -m ocrmypdf");
        } else if (pathExists("python3 -m ocrmypdf")) {
            return Optional.of("python3 -m ocrmypdf");
        } else {
            return Optional.empty();
        }
    }

    private boolean pathExists(String path) {
        String[] pathParts = path.split(" ");
        ArrayList<String> command = new ArrayList<>(Arrays.asList(pathParts));
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
