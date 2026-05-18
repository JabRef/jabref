package org.jabref.gui.linkedfile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.OcrEngine;
import org.jabref.logic.ocr.OcrResult;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcrLinkedFileAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrLinkedFileAction.class);

    private LinkedFile linkedFile;
    private BibDatabaseContext databaseContext;
    private DialogService dialogService;
    private GuiPreferences preferences;
    private TaskExecutor taskExecutor;
    private OcrEngine ocrEngine;
    private BibEntry entry;

    public OcrLinkedFileAction(LinkedFile linkedFile,
                               BibEntry bibEntry,
                               BibDatabaseContext databaseContext,
                               DialogService dialogService,
                               GuiPreferences preferences,
                               TaskExecutor taskExecutor,
                               OcrEngine ocrEngine) {
        this.linkedFile = linkedFile;
        this.entry = bibEntry;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.ocrEngine = ocrEngine;
    }

    @Override
    public void execute() {
        Optional<Path> pdfPath = linkedFile.findIn(databaseContext, preferences.getFilePreferences());
        if (!pdfPath.isPresent()) {
            return;
        }
        BackgroundTask<OcrResult> ocrTask = BackgroundTask.wrap(() -> ocrEngine.performOcrAndEmbedText(pdfPath.get()));

        ocrTask.titleProperty().set(Localization.lang("Performing OCR"));
        ocrTask.showToUser(true);
        ocrTask.onSuccess(result -> {
            switch (result) {
                case OcrResult.Success success -> {
                    dialogService.notify(Localization.lang("OCR succeeded"));
                    LinkedFile ocredPdf = new LinkedFile(success.outputFile());
                    List<LinkedFile> ocredFiles = List.of(ocredPdf);
                    entry.addFiles(ocredFiles);
                }
                case OcrResult.Failure failure -> {
                    String failureReason = switch (failure.reason()) {
                        case NOT_AVAILABLE ->
                                "OCRmyPDF is not available";
                        case TIMEOUT ->
                                "OCR timed out";
                        case NON_ZERO_EXIT ->
                                "OCR process failed";
                        case IO_ERROR ->
                                "Could not start OCR process";
                        case INTERRUPTED ->
                                "OCR was cancelled";
                    };
                    dialogService.showErrorDialogAndWait(Localization.lang("OCR failed"), Localization.lang(failureReason));
                }
            }
        });
        ocrTask.onFailure(exception -> {
            LOGGER.error("Unexpected error during OCR", exception);
            dialogService.notify(Localization.lang("OCR failed"));
        });
        taskExecutor.execute(ocrTask);
    }
}
