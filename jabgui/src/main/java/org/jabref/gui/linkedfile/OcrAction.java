package org.jabref.gui.linkedfile;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.OcrService;
import org.jabref.logic.ocr.OcrResult;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
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
    private final BibEntry entry;

    public OcrAction(LinkedFile linkedFile,
                     BibEntry entry,
                     BibDatabaseContext databaseContext,
                     DialogService dialogService,
                     FilePreferences filePreferences,
                     TaskExecutor taskExecutor,
                     OcrService ocrService) {
        this.linkedFile = linkedFile;
        this.entry = entry;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
        this.ocrService = ocrService;

        // Only executable for existing PDF files
        this.executable.set(
                linkedFile.getFileType().equalsIgnoreCase("pdf") &&
                        linkedFile.findIn(databaseContext, filePreferences).isPresent()
        );
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

        // Generate output filename
        Path inputPath = filePath.get();
        String baseName = FileUtil.getBaseName(inputPath);
        Path outputPath = inputPath.resolveSibling(baseName + "_ocr.pdf");

        dialogService.notify(Localization.lang("Performing OCR..."));

        BackgroundTask<OcrResult> task = BackgroundTask.wrap(() ->
                ocrService.createSearchablePdf(inputPath, outputPath)
        );

        task.onSuccess(result -> {
                    // Use pattern matching with the sealed class
                    switch (result) {
                        case OcrResult.Success success -> {
                            String extractedText = success.text();
                            Optional<Path> createdFile = success.outputFile();

                            if (createdFile.isPresent()) {
                                // Ask user if they want to use the new searchable PDF
                                boolean useNewFile = dialogService.showConfirmationDialogAndWait(
                                        Localization.lang("OCR Complete"),
                                        Localization.lang("Searchable PDF created successfully. Do you want to link the new searchable PDF to this entry?")
                                );

                                if (useNewFile) {
                                    // Create new LinkedFile for the searchable PDF
                                    LinkedFile newLinkedFile = new LinkedFile(
                                            linkedFile.getDescription().isEmpty() ? "OCR Version" : linkedFile.getDescription() + " (OCR)",
                                            createdFile.get().toString(),
                                            linkedFile.getFileType()
                                    );

                                    // Add the new file to the entry
                                    entry.addFile(newLinkedFile);

                                    dialogService.notify(Localization.lang("Searchable PDF linked to entry"));
                                }
                            }

                            // Show preview of extracted text
                            if (!extractedText.isEmpty()) {
                                String preview = extractedText.length() > 500
                                        ? extractedText.substring(0, 500) + "..."
                                        : extractedText;

                                dialogService.showInformationDialogAndWait(
                                        Localization.lang("OCR Text Preview"),
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
