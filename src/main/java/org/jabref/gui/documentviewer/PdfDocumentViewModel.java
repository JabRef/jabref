package org.jabref.gui.documentviewer;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class PdfDocumentViewModel extends DocumentViewModel {

    private final PDDocument document;

    public PdfDocumentViewModel(PDDocument document) {
        this.document = Objects.requireNonNull(document);
    }

    @Override
    public ObservableList<DocumentPageViewModel> getPages() {
        @SuppressWarnings("unchecked")
        List<PDPage> pages = document.getDocumentCatalog().getAllPages();

        return FXCollections.observableArrayList(
                pages.stream().map(PdfDocumentPageViewModel::new).collect(Collectors.toList()));
    }

    public int getNumberOfPages() {
        return document.getNumberOfPages();
    }
}
