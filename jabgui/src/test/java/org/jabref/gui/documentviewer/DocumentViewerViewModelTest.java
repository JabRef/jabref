package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class DocumentViewerViewModelTest {

    private DocumentViewerViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new DocumentViewerViewModel();
    }

    @Test
    void defaultStateHasNoDocument() {
        assertEquals(Optional.empty(), viewModel.getCurrentDocument());
        assertEquals(0, viewModel.currentPageProperty().get());
        assertEquals("", viewModel.highlightTextProperty().get());
        assertEquals(Localization.lang("Document viewer"), viewModel.titleProperty().get());
    }

    @Test
    void showDocumentWithValidPdfSetsDocumentAndTitle(@TempDir Path tempDir) throws IOException {
        Path pdfFile = tempDir.resolve("sample.pdf");
        Files.createFile(pdfFile);

        viewModel.showDocument(pdfFile);

        assertEquals(Optional.of(pdfFile), viewModel.getCurrentDocument());
        assertEquals(Localization.lang("Document viewer - %0", "sample.pdf"), viewModel.titleProperty().get());
    }

    @Test
    void showDocumentWithNonPdfClearsDocument(@TempDir Path tempDir) throws IOException {
        Path txtFile = tempDir.resolve("sample.txt");
        Files.createFile(txtFile);

        viewModel.showDocument(txtFile);

        assertEquals(Optional.empty(), viewModel.getCurrentDocument());
        assertEquals(Localization.lang("Document viewer"), viewModel.titleProperty().get());
    }

    @Test
    void showDocumentWithNullClearsDocument(@TempDir Path tempDir) throws IOException {
        Path pdfFile = tempDir.resolve("sample.pdf");
        Files.createFile(pdfFile);
        viewModel.showDocument(pdfFile);

        viewModel.showDocument(null);

        assertEquals(Optional.empty(), viewModel.getCurrentDocument());
        assertEquals(Localization.lang("Document viewer"), viewModel.titleProperty().get());
    }

    @Test
    void showPageConvertsOneIndexedToZeroIndexed() {
        viewModel.showPage(5);
        assertEquals(4, viewModel.currentPageProperty().get());

        viewModel.showPage(1);
        assertEquals(0, viewModel.currentPageProperty().get());

        viewModel.showPage(0);
        assertEquals(0, viewModel.currentPageProperty().get());

        viewModel.showPage(-3);
        assertEquals(0, viewModel.currentPageProperty().get());
    }

    @Test
    void highlightTextUpdatesProperty() {
        viewModel.highlightText("quantum computing");
        assertEquals("quantum computing", viewModel.highlightTextProperty().get());
    }

    @Test
    void resetClearsState(@TempDir Path tempDir) throws IOException {
        Path pdfFile = tempDir.resolve("sample.pdf");
        Files.createFile(pdfFile);

        viewModel.showDocument(pdfFile);
        viewModel.showPage(5);
        viewModel.highlightText("sample search");

        viewModel.reset();

        assertEquals(Optional.empty(), viewModel.getCurrentDocument());
        assertEquals(0, viewModel.currentPageProperty().get());
        assertEquals("", viewModel.highlightTextProperty().get());
        assertEquals(Localization.lang("Document viewer"), viewModel.titleProperty().get());
    }
}
