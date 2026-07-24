package org.jabref.gui.documentviewer;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import com.dlsc.pdfviewfx.PDFView;
import com.tobiasdiez.easybind.EasyBind;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A document viewer that wraps the PDFView control for displaying PDF documents.
public class PdfDocumentViewer extends StackPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfDocumentViewer.class);

    private final PDFView pdfView;
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final StringProperty highlightText = new SimpleStringProperty("");
    private final Label placeholderLabel;
    private final TaskExecutor taskExecutor;
    private @Nullable BackgroundTask<byte[]> currentTask;

    public PdfDocumentViewer() {
        this(new UiTaskExecutor());
    }

    PdfDocumentViewer(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        pdfView = new PDFView();

        placeholderLabel = new Label(Localization.lang("No PDF available for preview"));
        placeholderLabel.getStyleClass().add("message");
        placeholderLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(pdfView, placeholderLabel);

        pdfView.setVisible(false);
        placeholderLabel.setVisible(true);

        EasyBind.subscribe(currentPage, current -> pdfView.setPage(current.intValue()));
        EasyBind.subscribe(highlightText, pdfView::setSearchText);
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public StringProperty highlightTextProperty() {
        return highlightText;
    }

    public void show(@Nullable Path document) {
        cancelCurrent();

        if (document == null) {
            LOGGER.debug("No document provided to viewer, showing placeholder");
            pdfView.setVisible(false);
            placeholderLabel.setText(Localization.lang("No PDF available for preview"));
            placeholderLabel.setVisible(true);
            return;
        }

        pdfView.setVisible(false);
        placeholderLabel.setText(Localization.lang("Loading PDF..."));
        placeholderLabel.setVisible(true);

        BackgroundTask<byte[]> task = BackgroundTask.wrap(() -> Files.readAllBytes(document));

        task.onSuccess(bytes -> UiTaskExecutor.runNowOrInJavaFXThread(() -> {
            try {
                pdfView.load(new ByteArrayInputStream(bytes));
                pdfView.setPage(currentPage.get());
                pdfView.setVisible(true);
                placeholderLabel.setVisible(false);
                LOGGER.debug("Successfully loaded PDF document: {}", document);
            } catch (PDFView.Document.DocumentProcessingException e) {
                LOGGER.error("Could not load PDF document {}", document, e);
                pdfView.setVisible(false);
                placeholderLabel.setText(Localization.lang("Could not load PDF: %0", document.getFileName().toString()));
                placeholderLabel.setVisible(true);
            }
        }));

        task.onFailure(exception -> UiTaskExecutor.runNowOrInJavaFXThread(() -> {
            LOGGER.error("Could not load PDF document {}", document, exception);
            pdfView.setVisible(false);
            placeholderLabel.setText(Localization.lang("Could not load PDF: %0", document.getFileName().toString()));
            placeholderLabel.setVisible(true);
        }));

        currentTask = task;
        task.executeWith(taskExecutor);
    }

    /// Cancels any in-flight PDF loading.
    public void cancelCurrent() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }
}
