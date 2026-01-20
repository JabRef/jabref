package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import org.jabref.logic.l10n.Localization;

import com.dlsc.pdfviewfx.PDFView;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A document viewer that wraps the PDFView control for displaying PDF documents.
 */
public class PdfDocumentViewer extends StackPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfDocumentViewer.class);

    private final PDFView pdfView;
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final StringProperty highlightText = new SimpleStringProperty("");
    private final Label placeholderLabel;

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

    public void show(Path document) {
        if (document != null) {
            pdfView.setVisible(false);
            placeholderLabel.setText(Localization.lang("Loading PDF..."));
            placeholderLabel.setVisible(true);

            try (InputStream inputStream = Files.newInputStream(document)) {
                pdfView.load(inputStream);
                pdfView.setPage(currentPage.get());
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
            pdfView.setVisible(false);
            placeholderLabel.setText(Localization.lang("No PDF available for preview"));
            placeholderLabel.setVisible(true);
        }
    }
}
