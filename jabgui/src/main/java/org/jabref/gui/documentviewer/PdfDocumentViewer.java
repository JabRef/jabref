package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

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

        // Create placeholder label
        placeholderLabel = new Label("No PDF available for preview");
        placeholderLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 16px;");
        placeholderLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(pdfView, placeholderLabel);

        // Initially show placeholder
        pdfView.setVisible(false);
        placeholderLabel.setVisible(true);

        EasyBind.subscribe(currentPage, current -> pdfView.setPage(current.intValue()));
        // We can only set the search query at the moment not the results or mark them in the text
        EasyBind.subscribe(highlightText, pdfView::setSearchText);
        // Initially hide PDFView until a document is loaded
        pdfView.setVisible(false);
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public StringProperty highlightTextProperty() {
        return highlightText;
    }

    public void show(Path document) {
        if (document != null) {
            try {
                pdfView.load(Files.newInputStream(document));
                pdfView.setPage(currentPage.get());
                // Show PDF and hide placeholder
                pdfView.setVisible(true);
                placeholderLabel.setVisible(false);
                LOGGER.debug("Successfully loaded PDF document: {}", document);
            } catch (IOException e) {
                LOGGER.error("Could not load PDF document {}", document, e);
                // Show error message
                pdfView.setVisible(false);
                placeholderLabel.setText("Could not load PDF: " + document.getFileName());
                placeholderLabel.setVisible(true);
            }
        } else {
            LOGGER.debug("No document provided to viewer, showing placeholder");
            // Show placeholder and hide PDF
            pdfView.setVisible(false);
            placeholderLabel.setText("No PDF available for preview");
            placeholderLabel.setVisible(true);
        }
    }
}
