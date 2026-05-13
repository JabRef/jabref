package org.jabref.gui.linkedfile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.OcrEngine;
import org.jabref.logic.ocr.OcrResult;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

public class OcrLinkedFileAction extends SimpleCommand {
    private LinkedFile linkedFile;
    private BibDatabaseContext databaseContext;
    private DialogService dialogService;
    private FilePreferences filePreferences;
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
        this.filePreferences = preferences.getFilePreferences();
        this.taskExecutor = taskExecutor;
        this.ocrEngine = ocrEngine;
    }

    @Override
    public void execute() {
        Optional<Path> pdfPath = linkedFile.findIn(databaseContext, filePreferences);
        BackgroundTask<OcrResult> ocrTask = new BackgroundTask<>() {
            @Override
            public OcrResult call() {
                if (pdfPath.isPresent()) {
                    return ocrEngine.performOcrAndEmbedText(pdfPath.get());
                } else {
                    return null;
                }
            }
        };

        ocrTask.titleProperty().set(Localization.lang("OCRing"));
        ocrTask.showToUser(true);
        ocrTask.onSuccess(result -> {
            switch (result) {
                case OcrResult.Success s -> {
                    dialogService.notify(Localization.lang("OCR Success"));
                    LinkedFile ocredPdf = new LinkedFile(s.outputFile());
                    List<LinkedFile> ocredFiles = new ArrayList<>();
                    ocredFiles.add(ocredPdf);
                    entry.setFiles(ocredFiles);
                }
                case OcrResult.Failure f -> {
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("OCR failed"),
                            Localization.lang(switch (f.reason()) {
                                case NOT_AVAILABLE ->
                                        "OCRmyPDF is not installed.";
                                case TIMEOUT ->
                                        "OCR timed out.";
                                case NON_ZERO_EXIT ->
                                        "OCR process failed.";
                                case IO_ERROR ->
                                        "Could not start OCR process.";
                                case INTERRUPTED ->
                                        "OCR was cancelled.";
                            })
                    );
                }
            }
        });
        ocrTask.onFailure(_ -> {
            dialogService.notify(Localization.lang("OCR failed."));
        });
        taskExecutor.execute(ocrTask);
    }
}
