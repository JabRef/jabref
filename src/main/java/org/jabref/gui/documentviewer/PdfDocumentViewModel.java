package org.jabref.gui.documentviewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class PdfDocumentViewModel extends DocumentViewModel {

    private final PDDocument document;

    public PdfDocumentViewModel(PDDocument document) {
        this.document = Objects.requireNonNull(document);
        this.maxPagesProperty().set(document.getNumberOfPages());
    }

    @Override
    public ObservableList<DocumentPageViewModel> getPages() {
        @SuppressWarnings("unchecked")
        List<PDPage> pages = document.getDocumentCatalog().getAllPages();

        // There is apparently no neat way to get the page number from a PDPage...thus this old-style for loop
        List<PdfDocumentPageViewModel> pdfPages = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            pdfPages.add(new PdfDocumentPageViewModel(pages.get(i), i + 1));
        }
        return FXCollections.observableArrayList(pdfPages);
    }
}
