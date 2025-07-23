package org.jabref.gui.linkedfile;

import java.nio.file.Path;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.OcrResult;
import org.jabref.logic.ocr.OcrService;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background task for performing OCR on a PDF file.
 */
public class OcrBackgroundTask extends BackgroundTask<OcrResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrBackgroundTask.class);

    private final OcrService ocrService;
    private final Path filePath;
    private final LinkedFile linkedFile;

    public OcrBackgroundTask(OcrService ocrService, Path filePath, LinkedFile linkedFile) {
        this.ocrService = ocrService;
        this.filePath = filePath;
        this.linkedFile = linkedFile;

        // Configure the task
        this.showToUser(true);
        this.withInitialMessage(Localization.lang("Performing OCR on %0", linkedFile.getLink()));
    }

    @Override
    public OcrResult call() throws Exception {
        LOGGER.debug("Starting OCR task for file: {}", filePath);
        return ocrService.performOcr(filePath);
    }
}
