package org.jabref.gui.documentviewer;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class DocumentViewerViewModelTest {

    private final DialogService dialogService = mock(DialogService.class);

    // [utest->req~jabgui.documentviewer.open-crossref-pdf-at-page~1]
    @Test
    void doesNotReportAMissingPdfForAPaperWithACrossReferencedPdf() {
        BibEntry volume = new BibEntry(StandardEntryType.Proceedings)
                .withCitationKey("volume")
                .withFiles(List.of(new LinkedFile("", "volume.pdf", "PDF")));
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.CROSSREF, "volume");

        viewerShowing(paper, volume);

        verify(dialogService, never()).notify(anyString());
    }

    @Test
    void reportsAMissingPdfForAnEntryWithoutAnyPdf() {
        BibEntry article = new BibEntry(StandardEntryType.Article);

        viewerShowing(article);

        verify(dialogService).notify("No PDF files available");
    }

    private void viewerShowing(BibEntry selected, BibEntry... others) {
        BibDatabase database = new BibDatabase(List.of(selected));
        database.insertEntries(List.of(others));
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(selected));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext(database)));
        new DocumentViewerViewModel(stateManager, mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS), dialogService);
    }
}
