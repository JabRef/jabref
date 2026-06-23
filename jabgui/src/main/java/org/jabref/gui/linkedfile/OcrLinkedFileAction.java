package org.jabref.gui.linkedfile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.OcrEngine;
import org.jabref.logic.ocr.OcrMyPdfEngine;
import org.jabref.logic.ocr.OcrResult;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcrLinkedFileAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrLinkedFileAction.class);

    private final LinkedFile linkedFile;
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final OcrEngine ocrEngine;
    private final List<BibEntry> linkedEntries;
    private final ImportHandler importHandler;

    public OcrLinkedFileAction(LinkedFile linkedFile,
                               List<BibEntry> bibEntries,
                               BibDatabaseContext databaseContext,
                               DialogService dialogService,
                               GuiPreferences preferences,
                               TaskExecutor taskExecutor,
                               FileUpdateMonitor fileUpdateMonitor,
                               UndoManager undoManager,
                               StateManager stateManager) {
        this.linkedFile = linkedFile;
        this.linkedEntries = bibEntries;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.ocrEngine = new OcrMyPdfEngine(preferences.getOcrPreferences());
        this.importHandler = new ImportHandler(
                databaseContext,
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor
        );
    }

    @Override
    public void execute() {
        Optional<Path> pdfPath = linkedFile.findIn(databaseContext, preferences.getFilePreferences());
        if (pdfPath.isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not find a file to OCR"));
            return;
        }
        BackgroundTask<OcrResult> ocrTask = BackgroundTask.wrap(() -> ocrEngine.performOcrAndEmbedText(pdfPath.get()));

        ocrTask.titleProperty().set(Localization.lang("Performing OCR"));
        ocrTask.showToUser(true);
        ocrTask.onSuccess(result -> {
            switch (result) {
                case OcrResult.Success success -> {
                    dialogService.notify(Localization.lang("OCR succeeded"));
                    Path ocredPdf = success.outputFile();
                    for (BibEntry entry : linkedEntries) {
                        importHandler.getFileLinker().linkFilesToEntry(entry, List.of(ocredPdf));
                    }
                }
                case OcrResult.Failure failure -> {
                    String failureReason = getFailureResult(failure);
                    dialogService.showErrorDialogAndWait(Localization.lang("OCR failed"), failureReason);
                }
            }
        });
        ocrTask.onFailure(exception -> {
            LOGGER.error("Unexpected error during OCR", exception);
            dialogService.notify(Localization.lang("OCR failed. See the logs for the details"));
        });
        taskExecutor.execute(ocrTask);
    }

    String getFailureResult(OcrResult.Failure failure) {
        return switch (failure.reason()) {
            case NOT_AVAILABLE ->
                    Localization.lang("OCRmyPDF is not available at: %0", preferences.getOcrPreferences().getOcrEnginePath());
            case TIMEOUT ->
                    Localization.lang("OCR timed out");
            case NON_ZERO_EXIT ->
                    Localization.lang("OCR process failed");
            case IO_ERROR ->
                    Localization.lang("Could not start OCR process");
            case INTERRUPTED ->
                    Localization.lang("OCR was cancelled");
        };
    }
}
