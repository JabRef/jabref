package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.pdf.AnnotationArea;

import com.dlsc.pdfviewfx.PDFView;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A document viewer that wraps the PDFView control for displaying PDF documents.
public class PdfDocumentViewer extends StackPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfDocumentViewer.class);

    private final PDFView pdfView;
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final StringProperty highlightText = new SimpleStringProperty("");
    private final Label placeholderLabel;

    private Optional<AnnotationArea> pendingArea = Optional.empty();

    public PdfDocumentViewer() {
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

    public void centerOn(int pageIndex, AnnotationArea area) {
        pendingArea = Optional.of(area);
        currentPage.set(pageIndex);
        centerOnPendingArea();
    }

    private void centerOnPendingArea() {
        if (pdfView.getDocument() == null) {
            return;
        }
        pendingArea.ifPresent(area -> pdfView.setSelectedSearchResult(new PDFView.SearchResult(
                "", "", currentPage.get(), new Rectangle2D(area.x(), area.y(), area.width(), area.height()))));
        pendingArea = Optional.empty();
    }

    public void show(Path document) {
        // A marker belongs to the document it was placed in, and would otherwise keep floating
        // over whatever is shown next.
        pdfView.setSelectedSearchResult(null);

        if (document != null) {
            pdfView.setVisible(false);
            placeholderLabel.setText(Localization.lang("Loading PDF..."));
            placeholderLabel.setVisible(true);

            try (InputStream inputStream = Files.newInputStream(document)) {
                pdfView.load(inputStream);
                pdfView.setPage(currentPage.get());
                centerOnPendingArea();
                pdfView.setVisible(true);
                placeholderLabel.setVisible(false);
                LOGGER.debug("Successfully loaded PDF document: {}", document);
            } catch (IOException | PDFView.Document.DocumentProcessingException e) {
                LOGGER.error("Could not load PDF document {}", document, e);
                pdfView.setVisible(false);
                placeholderLabel.setText(Localization.lang("Could not load PDF: %0", document.getFileName().toString()));
                placeholderLabel.setVisible(true);
            }
        } else {
            LOGGER.debug("No document provided to viewer, showing placeholder");
            pdfView.unload();
            pdfView.setVisible(false);
            placeholderLabel.setText(Localization.lang("No PDF available for preview"));
            placeholderLabel.setVisible(true);
        }
    }
}
