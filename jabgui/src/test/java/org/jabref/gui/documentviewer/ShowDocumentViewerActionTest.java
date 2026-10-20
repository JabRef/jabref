package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.injection.Injector;
import com.dlsc.pdfviewfx.PDFView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class ShowDocumentViewerActionTest extends JavaFxTest {

    @TempDir
    Path directory;

    private final BibDatabaseContext databaseContext = new BibDatabaseContext();
    private final ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();
    private Optional<DocumentViewerView> shownViewer = Optional.empty();
    private DialogService dialogService;
    private ShowDocumentViewerAction action;

    @BeforeEach
    void setUp() {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        CliPreferences preferences = mock(CliPreferences.class);
        when(preferences.getFilePreferences()).thenReturn(FilePreferences.getDefault());
        dialogService = mock(DialogService.class);
        doAnswer(invocation -> {
            DocumentViewerView view = invocation.getArgument(0);
            shownViewer = Optional.of(view);
            view.show();
            view.getDialogPane().applyCss();
            view.getDialogPane().layout();
            return null;
        }).when(dialogService).showCustomDialog(any(DocumentViewerView.class));
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(CliPreferences.class, preferences);
        Injector.setModelOrService(DialogService.class, dialogService);
        action = new ShowDocumentViewerAction(stateManager, preferences);
    }

    @AfterEach
    void tearDown() {
        interact(() -> shownViewer.ifPresent(view -> {
            PDFView pdf = pdfControl(view);
            Optional.ofNullable(pdf.getDocument()).ifPresent(_ -> pdf.unload());
            view.close();
        }));
        Injector.forgetAll();
    }

    /// Covers page ranges, single pages, and positive and negative offsets.
    /// Invalid page values and calculated pages below one fall back to the first page.
    @ParameterizedTest
    @CsvSource(textBlock = """
            100, 54--86,  0,   54
            33,  54--86,  -53, 1
            33,  54--86,  -50, 4
            100, 54--86,  8,   62
            33,  33,      0,   33
            33,  invalid, 0,   1
            33,  54--86,  -54, 1
            """)
    void executeOpensOwnPdfAtResolvedPage(int totalPages, String pages, String offset, int expectedPage) throws IOException {
        BibEntry entry = createEntry(totalPages).withField(StandardField.PAGES, pages)
                                               .withField(PdfLocationResolver.PAGE_OFFSET, offset);

        interact(() -> {
            selectedEntries.setAll(entry);
            action.execute();

            PDFView pdf = pdfControl(openedViewer());
            assertEquals(totalPages, pdf.getDocument().getNumberOfPages());
            assertEquals(expectedPage - 1, pdf.getPage());
        });
    }

    /// An attached PDF remains viewable even when the entry has no starting-page metadata.
    @Test
    void executeDefaultsToFirstPageWithoutPages() throws IOException {
        BibEntry entry = createEntry(2);

        interact(() -> {
            selectedEntries.setAll(entry);
            action.execute();

            PDFView pdf = pdfControl(openedViewer());
            assertEquals(2, pdf.getDocument().getNumberOfPages());
            assertEquals(0, pdf.getPage());
        });
    }

    /// Multiple selected entries do not identify a unique starting page, so no entry-specific jump is applied.
    @Test
    void executeDoesNotChoosePageForMultipleEntries() throws IOException {
        BibEntry entry = createEntry(60).withField(StandardField.PAGES, "54--86");

        interact(() -> {
            selectedEntries.setAll(entry, new BibEntry());
            action.execute();

            PDFView pdf = pdfControl(openedViewer());
            assertEquals(60, pdf.getDocument().getNumberOfPages());
            assertEquals(0, pdf.getPage());
        });
    }

    /// Opening another entry linked to the same PDF resets to page 1 when that entry has no pages field.
    @Test
    void executeUpdatesPageWhenReusingViewer() throws IOException {
        BibEntry entry = createEntry(60).withField(StandardField.PAGES, "54--86");
        BibEntry entryWithoutPages = new BibEntry().withFiles(entry.getFiles());

        interact(() -> {
            selectedEntries.setAll(entry);
            action.execute();
            DocumentViewerView view = openedViewer();
            assertEquals(53, pdfControl(view).getPage());

            selectedEntries.setAll(entryWithoutPages);
            action.execute();

            assertSame(view, openedViewer());
            assertEquals(0, pdfControl(view).getPage());
        });
    }

    /// Reusing the document viewer for a different PDF must apply the new entry's starting page.
    @Test
    void executeNavigatesNewDocumentWhenReusingViewer() throws IOException {
        BibEntry shortEntry = createEntry(2).withField(StandardField.PAGES, "1--2");
        BibEntry longEntry = createEntry(60).withField(StandardField.PAGES, "54--86");

        interact(() -> {
            selectedEntries.setAll(shortEntry);
            action.execute();
            DocumentViewerView view = openedViewer();
            assertEquals(2, pdfControl(view).getDocument().getNumberOfPages());
            assertEquals(0, pdfControl(view).getPage());

            selectedEntries.setAll(longEntry);
            action.execute();

            assertSame(view, openedViewer());
            assertEquals(60, pdfControl(view).getDocument().getNumberOfPages());
            assertEquals(53, pdfControl(view).getPage());
        });
    }

    private DocumentViewerView openedViewer() {
        return shownViewer.orElseThrow();
    }

    private PDFView pdfControl(DocumentViewerView view) {
        return JavaFxExtension.lookup(view.getDialogPane(), ".pdf-view", PDFView.class);
    }

    private BibEntry createEntry(int pageCount) throws IOException {
        Path file = directory.resolve("pages-" + pageCount + ".pdf");
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < pageCount; page++) {
                document.addPage(new PDPage());
            }
            document.save(file.toFile());
        }
        return new BibEntry().withFiles(List.of(new LinkedFile("", file, "PDF")));
    }
}
