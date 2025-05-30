package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.StackPane;

import com.dlsc.pdfviewfx.PDFView;
import com.dlsc.pdfviewfx.skins.PDFViewSkin;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A document viewer that wraps the PDFView control for displaying PDF documents.
 */
public class PdfDocumentViewer extends StackPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfDocumentViewer.class);

    private final PDFView pdfView;
    private final IntegerProperty currentPage = new SimpleIntegerProperty(1);
    private final StringProperty highlightText = new SimpleStringProperty("");

    public PdfDocumentViewer() {
        pdfView = new PDFView();
        getChildren().add(pdfView);

        // TOOD: needs to trigger always use invalidation listener or something
        EasyBind.subscribe(currentPage, (current) -> pdfView.setPage(current.intValue()));
        EasyBind.subscribe(highlightText,  text -> {
            pdfView.setSearchText(text);
            // TODO: highlighting with marker
            PDFViewSkin.PageSearchResult result = new PDFViewSkin.PageSearchResult(currentPage.getValue(), text);
            pdfView.setSearchResults(FXCollections.observableList(result.getItems())); // emtpy
            pdfView.setShowSearchResults(true);
        });

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
                //  currentPage.set(1);
            } catch (
                    IOException e) {
                LOGGER.error("Could not load PDF document {}", document, e);
            }
        } else {
            LOGGER.error("Could not load PDF document: no document found");
        }
    }
}
