package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.EmptySuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.UndoableInsertEntries;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// @ExternalServicesTest("Downloads a PDF file")
class LinkedFilesEditorViewModelTest {
    private LinkedFilesEditorViewModel viewModel;
    private final GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
    private final UndoManager undoManager = mock(UndoManager.class);

    @Test
    @Disabled("Runs in UI AND downloads data. This causes troubles. If @ExternalServicesTest: UI framework cannot be started. If GuiTest: Too many connection errors")
    void urlFieldShouldDownloadFile(@TempDir Path tempDir) {
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(bibDatabaseContext.getFirstExistingFileDir(any())).thenReturn(Optional.of(tempDir));

        viewModel = new LinkedFilesEditorViewModel(StandardField.FILE, new EmptySuggestionProvider(), mock(DialogService.class), bibDatabaseContext,
                new CurrentThreadTaskExecutor(), mock(FieldCheckers.class), preferences, undoManager);

        viewModel.entry = new BibEntry().withCitationKey("test")
                                        .withField(StandardField.URL, "https://ceur-ws.org/Vol-847/paper6.pdf");
        viewModel.fetchFulltext();

        assertTrue(Files.exists(tempDir.resolve("test.pdf")));
    }

    @Test
    void entryFileChangeShouldSynchronizeWithViewModelFiles() {
        BibEntry entry = new BibEntry();

        viewModel = new LinkedFilesEditorViewModel(
                StandardField.FILE,
                new EmptySuggestionProvider(),
                mock(DialogService.class),
                bibDatabaseContext,
                new CurrentThreadTaskExecutor(),
                mock(FieldCheckers.class),
                preferences,
                undoManager
        );
        viewModel.bindToEntry(entry);

        assertTrue(viewModel.getFiles().isEmpty(), "Initial files list should be empty");

        LinkedFile newFile = new LinkedFile("description", Path.of("test.pdf"), "PDF");
        entry.setFiles(List.of(newFile));

        assertEquals(1, viewModel.getFiles().size(), "ViewModel files should update reactively when entry files are updated");
    }

    @Test
    void addingAProceedingsPdfAlsoLinksTheEntrysPages(@TempDir Path tempDir) throws IOException {
        Path proceedings = tempDir.resolve("proceedings.pdf");
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < 5; page++) {
                document.addPage(new PDPage());
            }
            document.save(proceedings.toFile());
        }
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(tempDir));
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.PAGES, "2--3");

        viewModel = new LinkedFilesEditorViewModel(
                StandardField.FILE,
                new EmptySuggestionProvider(),
                mock(DialogService.class),
                bibDatabaseContext,
                new CurrentThreadTaskExecutor(),
                mock(FieldCheckers.class),
                preferences,
                undoManager
        );
        viewModel.bindToEntry(entry);

        viewModel.addNewLinkedFile(new LinkedFile("", Path.of("proceedings.pdf"), "PDF"));

        assertEquals(2, viewModel.getFiles().size());
        assertEquals("proceedings-p2-3.pdf", viewModel.getFiles().get(1).getFile().getLink());
        assertEquals(Optional.of(tempDir.resolve("proceedings-p2-3.pdf")), viewModel.getFiles().get(1).getFile().findIn(List.of(tempDir)));
    }

    @Test
    void addingAPdfToProceedingsCreatesAnEntryPerPaper(@TempDir Path tempDir) throws IOException {
        proceedingsEditor(tempDir, 18);

        viewModel.addNewLinkedFile(new LinkedFile("", Path.of("proceedings.pdf"), "PDF"));

        ArgumentCaptor<UndoableInsertEntries> insert = ArgumentCaptor.forClass(UndoableInsertEntries.class);
        verify(undoManager).applyEdit(insert.capture());
        assertEquals(List.of("Alpha", "Beta"), insert.getValue().entries().stream().map(paper -> paper.getField(StandardField.TITLE).orElseThrow()).toList());
        assertEquals("Alpha.pdf", insert.getValue().entries().getFirst().getFiles().getFirst().getLink());
        assertEquals(1, viewModel.getFiles().size());
    }

    @Test
    void doesNotSplitAPdfThatHoldsOnePaper(@TempDir Path tempDir) throws IOException {
        proceedingsEditor(tempDir, 10);

        viewModel.addNewLinkedFile(new LinkedFile("", Path.of("proceedings.pdf"), "PDF"));

        verify(undoManager, never()).applyEdit(any());
    }

    /// Binds the editor to a proceedings entry whose PDF has bookmarks "Alpha" and "Beta" on pages headed in `headingSize` point text
    private void proceedingsEditor(Path tempDir, int headingSize) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < 4; page++) {
                PDPage pdfPage = new PDPage();
                document.addPage(pdfPage);
                try (PDPageContentStream content = new PDPageContentStream(document, pdfPage)) {
                    if (page % 2 == 0) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), headingSize);
                        content.newLineAtOffset(72, 720);
                        content.showText("A paper in the proceedings");
                        content.endText();
                    }
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    content.newLineAtOffset(72, 600);
                    content.showText("Body text that sets the most common font size of the page.");
                    content.endText();
                }
            }
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(bookmark("Alpha", document.getPage(0)));
            outline.addLast(bookmark("Beta", document.getPage(2)));
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(tempDir.resolve("proceedings.pdf").toFile());
        }
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(tempDir));
        when(bibDatabaseContext.getDatabase()).thenReturn(new BibDatabase());

        viewModel = new LinkedFilesEditorViewModel(
                StandardField.FILE,
                new EmptySuggestionProvider(),
                mock(DialogService.class),
                bibDatabaseContext,
                new CurrentThreadTaskExecutor(),
                mock(FieldCheckers.class),
                preferences,
                undoManager
        );
        viewModel.bindToEntry(new BibEntry(StandardEntryType.Proceedings));
    }

    private static PDOutlineItem bookmark(String title, PDPage page) {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(title);
        item.setDestination(page);
        return item;
    }
}
