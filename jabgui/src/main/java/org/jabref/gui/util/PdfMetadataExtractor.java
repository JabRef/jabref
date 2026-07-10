package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.concurrent.Task;

import org.jabref.logic.l10n.Localization;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Extracts and formats PDF metadata (page count, file size, title, author, dates,
/// keywords) for preview purposes.
@NullMarked
public class PdfMetadataExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMetadataExtractor.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pdf-metadata-preview");
        thread.setDaemon(true);
        return thread;
    });

    private @Nullable Task<String> currentTask;

    /// Cancels any in-flight extraction and starts a new one.
    public void extractAsync(Path filePath, Consumer<String> onSuccess, Consumer<Throwable> onFailure) {
        cancelCurrent();

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return extract(filePath);
            }
        };

        task.setOnSucceeded(event -> UiTaskExecutor.runNowOrInJavaFXThread(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(event -> UiTaskExecutor.runNowOrInJavaFXThread(() -> onFailure.accept(task.getException())));

        currentTask = task;
        executor.execute(task);
    }

    /// Cancels the currently running extraction, if any.
    public void cancelCurrent() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    private String extract(Path filePath) {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDDocumentInformation info = document.getDocumentInformation();
            List<String> lines = new ArrayList<>();

            addLine(lines, Localization.lang("File"), filePath.getFileName().toString());
            addLine(lines, Localization.lang("Pages"), String.valueOf(document.getNumberOfPages()));
            addLine(lines, Localization.lang("Size"), formatFileSize(Files.size(filePath)));

            int baseLineCount = lines.size();

            if (info != null) {
                addLine(lines, Localization.lang("Title"), info.getTitle());
                addLine(lines, Localization.lang("Author"), info.getAuthor());
                addLine(lines, Localization.lang("Creation date"), formatDate(info.getCreationDate()));
                addLine(lines, Localization.lang("Modified date"), formatDate(info.getModificationDate()));
                addLine(lines, Localization.lang("Keywords"), info.getKeywords());
            }

            if (lines.size() == baseLineCount) {
                lines.add(Localization.lang("No extracted metadata available."));
            }

            return String.join(System.lineSeparator(), lines);
        } catch (IOException e) {
            LOGGER.warn("Could not extract PDF metadata for {}", filePath, e);
            return Localization.lang("Could not extract Metadata from: %0", filePath.getFileName().toString());
        }
    }

    private void addLine(List<String> lines, String label, @Nullable String value) {
        if ((value != null) && !value.isBlank()) {
            lines.add(Localization.lang("%0: %1", label, value));
        }
    }

    /// Formats a byte count as a human-readable size.
    private String formatFileSize(long bytes) {
        return getString(bytes);
    }

    @NonNull
    public static String getString(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        int exponent = (int) (Math.log(bytes) / Math.log(1024));
        exponent = Math.min(exponent, 4);
        char unitPrefix = "KMGT".charAt(exponent - 1);
        double value = bytes / Math.pow(1024, exponent);

        return String.format("%.1f %sB", value, unitPrefix);
    }

    /// Formats a date as a human-readable string.
    private @Nullable String formatDate(@Nullable Calendar calendar) {
        if (calendar == null) {
            return null;
        }

        return DATE_FORMATTER.format(calendar.toInstant().atZone(ZoneId.systemDefault()));
    }
}
