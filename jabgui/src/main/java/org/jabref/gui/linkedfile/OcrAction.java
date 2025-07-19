package org.jabref.gui.linkedfile;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.OcrService;
import org.jabref.logic.ocr.OcrResult;
import org.jabref.logic.ocr.OcrException;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.logic.FilePreferences;

import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for performing OCR (Optical Character Recognition) on linked PDF files.
 */
public class OcrAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrAction.class);

    private final LinkedFile linkedFile;
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;
    private final OcrService ocrService;

    public OcrAction(LinkedFile linkedFile,
                     BibDatabaseContext databaseContext,
                     DialogService dialogService,
                     FilePreferences filePreferences,
                     TaskExecutor taskExecutor,
                     OcrService ocrService) {
        this.linkedFile = linkedFile;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;

        // Only executable for existing PDF files
        this.executable.set(
                linkedFile.getFileType().equalsIgnoreCase("pdf") &&
                        linkedFile.findIn(databaseContext, filePreferences).isPresent()
        );
        this.ocrService = ocrService;
    }

    @Override
    public void execute() {
        Optional<Path> filePath = linkedFile.findIn(databaseContext, filePreferences);

        if (filePath.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("File not found"),
                    Localization.lang("Could not locate the PDF file on disk.")
            );
            return;
        }

        dialogService.notify(Localization.lang("Performing OCR..."));

        BackgroundTask<OcrResult> task = BackgroundTask.wrap(() -> {
                                                           return ocrService.performOcr(filePath.get());
                                                       })
                                                       .showToUser(true)  // Show in task list
                                                       .withInitialMessage(Localization.lang("Performing OCR on %0", linkedFile.getLink()));

        task.onSuccess(result -> {
                // Use pattern matching with the sealed class
                switch (result) {
                    case OcrResult.Success success -> {
                        String extractedText = success.text();
                        if (extractedText.isEmpty()) {
                            dialogService.showInformationDialogAndWait(
                                    Localization.lang("OCR Complete"),
                                    Localization.lang("No text was found in the PDF.")
                            );
                        } else {
                            // Show preview
                            String preview = extractedText.length() > 1000
                                    ? extractedText.substring(0, 1000) + "..."
                                    : extractedText;

                            dialogService.showInformationDialogAndWait(
                                    Localization.lang("OCR Result"),
                                    preview
                            );
                        }
                    }
                    case OcrResult.Failure failure -> {
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("OCR failed"),
                                failure.errorMessage()
                        );
                    }
                }
            })
            .onFailure(exception -> {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("OCR failed"),
                        exception.getMessage()
                );
            })
            .executeWith(taskExecutor);
    }
}
