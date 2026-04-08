package org.jabref.gui.documentviewer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.pdf.PdfPageLabelResolver;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentViewerViewModelTest {

    @TempDir
    Path tempDir;

    @Test
    void setCurrentEntriesUsesPdfFromCrossrefAndChildStartPage() {
        BibDatabase database = new BibDatabase();
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        LinkedFile parentFile = new LinkedFile("", Path.of("proceedings.pdf"), "pdf");
        BibEntry parentEntry = new BibEntry()
                .withCitationKey("parent")
                .withFiles(List.of(parentFile));
        BibEntry childEntry = new BibEntry()
                .withField(StandardField.CROSSREF, "parent")
                .withField(StandardField.PAGES, "73--96");

        database.insertEntries(List.of(parentEntry, childEntry));

        ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList(childEntry);
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        DialogService dialogService = mock(DialogService.class);
        DocumentViewerViewModel viewModel = new DocumentViewerViewModel(stateManager, mock(CliPreferences.class), dialogService);

        assertEquals(1, viewModel.filesProperty().size());
        assertEquals("proceedings.pdf", viewModel.filesProperty().get(0).getLink());
        assertEquals(72, viewModel.currentPageProperty().get());
        verify(dialogService, never()).notify(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void setCurrentEntriesDefaultsToFirstPageWhenPagesFieldIsMissing() {
        BibDatabase database = new BibDatabase();
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        LinkedFile parentFile = new LinkedFile("", Path.of("proceedings.pdf"), "pdf");
        BibEntry parentEntry = new BibEntry()
                .withCitationKey("parent")
                .withFiles(List.of(parentFile));
        BibEntry childEntry = new BibEntry()
                .withField(StandardField.CROSSREF, "parent")
                .withField(StandardField.PAGES, "no-page-number");

        database.insertEntries(List.of(parentEntry, childEntry));

        ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList(childEntry);
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        DocumentViewerViewModel viewModel = new DocumentViewerViewModel(stateManager, mock(CliPreferences.class), mock(DialogService.class));

        assertEquals(0, viewModel.currentPageProperty().get());
    }

    @Test
    void switchToFileRestoresPageFromSelectedEntry() {
        LinkedFile parentFile = new LinkedFile("", Path.of("proceedings.pdf"), "pdf");
        BibEntry childEntry = new BibEntry()
                .withField(StandardField.CROSSREF, "parent")
                .withField(StandardField.PAGES, "73--96");

        ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList(childEntry);
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());

        DocumentViewerViewModel viewModel = new DocumentViewerViewModel(stateManager, mock(CliPreferences.class), mock(DialogService.class));

        viewModel.currentPageProperty().set(0);
        viewModel.switchToFile(parentFile);

        assertEquals(72, viewModel.currentPageProperty().get());
    }

    @Test
    void switchToFileUsesPdfPageLabelsForLogicalPageMapping() throws Exception {
        Path pdfPath = tempDir.resolve("proceedings.pdf");
        createPdfWithFrontMatterAndDecimalLabels(pdfPath);

        LinkedFile parentFile = spy(new LinkedFile("", pdfPath, "pdf"));
        BibEntry childEntry = new BibEntry()
                .withField(StandardField.PAGES, "3--10")
                .withFiles(List.of(parentFile));

        BibDatabase database = new BibDatabase(List.of(childEntry));
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList(childEntry);
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        CliPreferences preferences = mock(CliPreferences.class);
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        doReturn(Optional.of(pdfPath)).when(parentFile).findIn(any(BibDatabaseContext.class), any(FilePreferences.class));

        DocumentViewerViewModel viewModel = new DocumentViewerViewModel(stateManager, preferences, mock(DialogService.class));
        viewModel.switchToFile(parentFile);

        assertEquals(4, viewModel.currentPageProperty().get());
    }

    @Test
    void switchToFileSkipsPdfPageLabelResolutionForFirstLogicalPage() {
        Path pdfPath = tempDir.resolve("proceedings.pdf");

        LinkedFile parentFile = spy(new LinkedFile("", pdfPath, "pdf"));
        BibEntry childEntry = new BibEntry()
                .withField(StandardField.PAGES, "1--10")
                .withFiles(List.of(parentFile));

        BibDatabase database = new BibDatabase(List.of(childEntry));
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList(childEntry);
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        CliPreferences preferences = mock(CliPreferences.class);
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        doReturn(Optional.of(pdfPath)).when(parentFile).findIn(any(BibDatabaseContext.class), any(FilePreferences.class));

        try (MockedStatic<PdfPageLabelResolver> pdfPageLabelResolver = mockStatic(PdfPageLabelResolver.class)) {
            DocumentViewerViewModel viewModel = new DocumentViewerViewModel(stateManager, preferences, mock(DialogService.class));
            viewModel.switchToFile(parentFile);

            pdfPageLabelResolver.verifyNoInteractions();
            assertEquals(0, viewModel.currentPageProperty().get());
        }
    }

    private static void createPdfWithFrontMatterAndDecimalLabels(Path pdfFile) throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 5; i++) {
                document.addPage(new PDPage());
            }

            PDPageLabels labels = new PDPageLabels(document);

            PDPageLabelRange frontMatter = new PDPageLabelRange();
            frontMatter.setStyle(PDPageLabelRange.STYLE_ROMAN_LOWER);
            frontMatter.setStart(1);
            labels.setLabelItem(0, frontMatter);

            PDPageLabelRange mainContent = new PDPageLabelRange();
            mainContent.setStyle(PDPageLabelRange.STYLE_DECIMAL);
            mainContent.setStart(1);
            labels.setLabelItem(2, mainContent);

            document.getDocumentCatalog().setPageLabels(labels);
            document.save(pdfFile.toFile());
        }
    }
}
