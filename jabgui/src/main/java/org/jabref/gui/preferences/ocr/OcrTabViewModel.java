package org.jabref.gui.preferences.ocr;

import java.io.IOException;
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
        if (ocrmypdfPathExists()) {
            return Optional.of("ocrmypdf");
        } else if (pythonOcrPathExists()) {
            return Optional.of("python -m ocrmypdf");
        } else {
            return Optional.empty();
        }
    }

    private boolean ocrmypdfPathExists() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ocrmypdf", "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Get the output and the errors of the process
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);

            boolean finished = process.waitFor(CHECKING_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.debug("Auto-detecting ocrmypdf as path timed out");
                return false;
            }
            return process.exitValue() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Auto detection of ocrmpdf as engine's path was interrupted", e);
            return false;
        } catch (IOException e) {
            LOGGER.debug("ocrmypdf is not available as engine's path: IOException occurred", e);
            return false;
        }
    }

    private boolean pythonOcrPathExists() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "-m", "ocrmypdf", "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Get the output and the errors of the process
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);

            boolean finished = process.waitFor(CHECKING_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.debug("Auto-detecting python -m ocrmypdf as path timed out");
                return false;
            }
            return process.exitValue() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Auto detection of python -m ocrmpdf as engine's path was interrupted", e);
            return false;
        } catch (IOException e) {
            LOGGER.debug("python -m ocrmpdf is not available as engine's path: IOException occurred", e);
            return false;
        }
    }
}
