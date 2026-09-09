package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class LiveDocumentViewerViewModelTest {

    private StateManager stateManager;
    private CliPreferences preferences;
    private DialogService dialogService;
    private ObservableList<BibEntry> selectedEntries;

    @BeforeEach
    void setUp() {
        stateManager = mock(StateManager.class);
        preferences = mock(CliPreferences.class);
        dialogService = mock(DialogService.class);
        selectedEntries = FXCollections.observableArrayList();
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
    }

    @Test
    void initialSelectionWithPdfPopulatesFiles() {
        LinkedFile pdfFile = new LinkedFile("Paper", "paper.pdf", "PDF");
        BibEntry entry = new BibEntry().withFiles(List.of(pdfFile));
        selectedEntries.setAll(entry);

        LiveDocumentViewerViewModel viewModel = new LiveDocumentViewerViewModel(stateManager, preferences, dialogService);

        assertEquals(List.of(pdfFile), viewModel.filesProperty().get());
        assertTrue(viewModel.isLiveMode());
    }

    @Test
    void selectionChangeUpdatesFilesWhenInLiveMode() {
        LiveDocumentViewerViewModel viewModel = new LiveDocumentViewerViewModel(stateManager, preferences, dialogService);
        assertEquals(List.of(), viewModel.filesProperty().get());

        LinkedFile pdfFile = new LinkedFile("Paper", "paper.pdf", "PDF");
        BibEntry entry = new BibEntry().withFiles(List.of(pdfFile));
        selectedEntries.setAll(entry);

        assertEquals(List.of(pdfFile), viewModel.filesProperty().get());
    }

    @Test
    void selectionChangeDoesNotUpdateFilesWhenInLockedMode() {
        LinkedFile pdfFile1 = new LinkedFile("Paper 1", "paper1.pdf", "PDF");
        BibEntry entry1 = new BibEntry().withFiles(List.of(pdfFile1));
        selectedEntries.setAll(entry1);

        LiveDocumentViewerViewModel viewModel = new LiveDocumentViewerViewModel(stateManager, preferences, dialogService);
        assertEquals(List.of(pdfFile1), viewModel.filesProperty().get());

        viewModel.setLiveMode(false);
        assertFalse(viewModel.isLiveMode());

        LinkedFile pdfFile2 = new LinkedFile("Paper 2", "paper2.pdf", "PDF");
        BibEntry entry2 = new BibEntry().withFiles(List.of(pdfFile2));
        selectedEntries.setAll(entry2);

        assertEquals(List.of(pdfFile1), viewModel.filesProperty().get());
    }

    @Test
    void togglingFromLockedToLiveUpdatesFilesWithCurrentSelection() {
        LinkedFile pdfFile1 = new LinkedFile("Paper 1", "paper1.pdf", "PDF");
        BibEntry entry1 = new BibEntry().withFiles(List.of(pdfFile1));
        selectedEntries.setAll(entry1);

        LiveDocumentViewerViewModel viewModel = new LiveDocumentViewerViewModel(stateManager, preferences, dialogService);
        viewModel.setLiveMode(false);

        LinkedFile pdfFile2 = new LinkedFile("Paper 2", "paper2.pdf", "PDF");
        BibEntry entry2 = new BibEntry().withFiles(List.of(pdfFile2));
        selectedEntries.setAll(entry2);

        assertEquals(List.of(pdfFile1), viewModel.filesProperty().get());

        viewModel.setLiveMode(true);
        assertEquals(List.of(pdfFile2), viewModel.filesProperty().get());
    }

    @Test
    void selectionWithNoPdfClearsFilesAndNotifies() {
        LiveDocumentViewerViewModel viewModel = new LiveDocumentViewerViewModel(stateManager, preferences, dialogService);

        BibEntry entry = new BibEntry().withCitationKey("Key1");
        selectedEntries.setAll(entry);

        assertEquals(List.of(), viewModel.filesProperty().get());
        verify(dialogService).notify(Localization.lang("No PDF files available"));
    }

    @Test
    void switchToFileResolvesFileAndSetsDocument(@TempDir Path tempDir) throws IOException {
        Path pdfFile = tempDir.resolve("sample.pdf");
        Files.createFile(pdfFile);

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(preferences.getFilePreferences()).thenReturn(filePreferences);

        LiveDocumentViewerViewModel viewModel = new LiveDocumentViewerViewModel(stateManager, preferences, dialogService);

        LinkedFile linkedFile = new LinkedFile("", pdfFile.toAbsolutePath().toString(), "PDF");
        viewModel.switchToFile(linkedFile);

        assertEquals(Optional.of(pdfFile), viewModel.getCurrentDocument());
    }
}
