package org.jabref.gui.linkedfile;

import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.ocr.OcrMethod;
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
import java.util.List;
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

        // Get available OCR methods
        List<OcrMethod> availableMethods = ocrService.getAvailableMethods();

        if (availableMethods.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("OCR not available"),
                    Localization.lang("No OCR methods are available. Please check your Tesseract configuration.")
            );
            return;
        }

        // Let user choose OCR method
        OcrMethod selectedMethod;
        if (availableMethods.size() == 1) {
            // Only one method available, use it
            selectedMethod = availableMethods.get(0);
        } else {
            // Show choice dialog with custom formatting
            selectedMethod = showOcrMethodDialog(availableMethods);
            if (selectedMethod == null) {
                return;
            }
        }

        // Generate output filename
        Path inputPath = filePath.get();
        String baseName = FileUtil.getBaseName(inputPath);
        Path outputPath = inputPath.resolveSibling(baseName + "_ocr.pdf");

        dialogService.notify(Localization.lang("Performing OCR with %0...", selectedMethod.getDisplayName()));

        BackgroundTask<OcrResult> task = BackgroundTask.wrap(() ->
                ocrService.createSearchablePdf(inputPath, outputPath, selectedMethod)
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
                                        Localization.lang("Searchable PDF created successfully with %0. Do you want to link the new searchable PDF to this entry?",
                                                selectedMethod.getDisplayName())
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

                            // Show preview of extracted text (only for PDFBox method, as ocrmypdf doesn't return text)
                            if (selectedMethod == OcrMethod.PDFBOX && !extractedText.isEmpty()) {
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

    /**
     * Shows a dialog for selecting an OCR method with custom formatting.
     *
     * @param availableMethods List of available OCR methods
     * @return Selected OCR method, or null if cancelled
     */
    private OcrMethod showOcrMethodDialog(List<OcrMethod> availableMethods) {
        ChoiceDialog<OcrMethod> dialog = new ChoiceDialog<>(availableMethods.get(0), availableMethods);
        dialog.setTitle(Localization.lang("Choose OCR Method"));
        dialog.setHeaderText(Localization.lang("Select the method to create a searchable PDF:"));

        // Access the ComboBox from the dialog pane and set the converter
        ComboBox<OcrMethod> comboBox = (ComboBox<OcrMethod>) dialog.getDialogPane().lookup(".combo-box");
        if (comboBox != null) {
            comboBox.setConverter(new StringConverter<OcrMethod>() {
                @Override
                public String toString(OcrMethod method) {
                    if (method == null) {
                        return "";
                    }
                    return method.getDisplayName();
                }

                @Override
                public OcrMethod fromString(String string) {
                    return availableMethods.stream()
                            .filter(method -> method.getDisplayName().equals(string))
                            .findFirst()
                            .orElse(null);
                }
            });

            // Set a wider preferred width to accommodate the descriptions
            comboBox.setPrefWidth(300);
        }

        // Add description text below the header
        dialog.setContentText(getMethodDescriptions(availableMethods));

        Optional<OcrMethod> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Creates a formatted string with method descriptions.
     *
     * @param methods List of OCR methods
     * @return Formatted description string
     */
    private String getMethodDescriptions(List<OcrMethod> methods) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localization.lang("Available methods:")).append("\n\n");

        for (OcrMethod method : methods) {
            sb.append("• ").append(method.getDisplayName()).append(":\n");
            sb.append("  ").append(method.getDescription()).append("\n\n");
        }

        return sb.toString().trim();
    }
}
