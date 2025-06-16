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
import org.jabref.logic.ocr.OcrException;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.logic.FilePreferences;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Action for performing OCR (Optical Character Recognition) on linked PDF files.
 * <p>
 * This action extracts text content from PDF files that are attached to BibTeX entries.
 * It runs the OCR process in a background thread to keep the UI responsive and provides
 * user feedback through dialogs and notifications.
 * <p>
 * The action follows JabRef's command pattern and can be triggered from context menus.
 * It includes built-in validation to ensure it's only enabled for PDF files that exist on disk.
 *
 * @see OcrService
 * @see org.jabref.gui.actions.SimpleCommand
 */

// Personal Note: Add more doc in between later

public class OcrAction extends SimpleCommand {

    private final LinkedFile linkedFile;
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    public OcrAction(LinkedFile linkedFile,
                     BibDatabaseContext databaseContext,
                     DialogService dialogService,
                     FilePreferences filePreferences,
                     TaskExecutor taskExecutor) {
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

        BackgroundTask.wrap(() -> {
                          OcrService ocrService = new OcrService();
                          return ocrService.performOcr(filePath.get());
                      })
                      .onSuccess(extractedText -> {
                          if (extractedText.isEmpty()) {
                              dialogService.showInformationDialogAndWait(
                                      Localization.lang("OCR Complete"),
                                      Localization.lang("No text was found in the PDF.")
                              );
                          } else {
                              // For now, just show preview
                              String preview = extractedText.length() > 1000
                                      ? extractedText.substring(0, 1000) + "..."
                                      : extractedText;

                              dialogService.showInformationDialogAndWait(
                                      Localization.lang("OCR Result"),
                                      preview
                              );
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
