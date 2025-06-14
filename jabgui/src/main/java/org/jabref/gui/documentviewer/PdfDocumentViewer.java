package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

    public PdfDocumentViewer() {
        pdfView = new PDFView();
        getChildren().add(pdfView);
        EasyBind.subscribe(currentPage, current -> pdfView.setPage(current.intValue()));
        // We can only set the search query at the moment not the results or mark them in the text
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
            try {
                pdfView.load(Files.newInputStream(document));
                pdfView.setPage(currentPage.get());
            } catch (IOException e) {
                LOGGER.error("Could not load PDF document {}", document, e);
            }
        } else {
            LOGGER.error("Could not load PDF document: no document found");
        }
    }
}
