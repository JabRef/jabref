package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.StackPane;

import com.dlsc.pdfviewfx.PDFView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A document viewer that wraps the PDFView control for displaying PDF documents.
 */
public class PdfDocumentViewer extends StackPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfDocumentViewer.class);

    private final PDFView pdfView;
    private final IntegerProperty currentPage = new SimpleIntegerProperty(1);

    public PdfDocumentViewer() {
        pdfView = new PDFView();
        pdfView.pageProperty().bindBidirectional(currentPage);
        getChildren().add(pdfView);
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public void show(Path document) {
        if (document != null) {
            try {
                pdfView.load(Files.newInputStream(document));
                currentPage.set(1);
            } catch (IOException e) {
                LOGGER.error("Could not load PDF document {}", document, e);
            }
        } else {
            LOGGER.error("Could not load PDF document: no document found");
        }
    }
}
