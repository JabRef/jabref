package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.EmptySuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

    // [utest->req~logic.pdf.extract-paper-page-range~1]
    @Test
    void attachingAPdfLinksACopyOfTheEntryPages(@TempDir Path tempDir) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < 5; page++) {
                document.addPage(new PDPage());
            }
            document.save(tempDir.resolve("proceedings.pdf").toFile());
        }
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.PAGES, "2--3");
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(tempDir));
        when(bibDatabaseContext.getDatabase()).thenReturn(new BibDatabase(List.of(paper)));
        DeferredTaskExecutor taskExecutor = new DeferredTaskExecutor();
        DialogService dialogService = mock(DialogService.class);
        when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        LinkedFilesEditorViewModel paperEditor = editor(dialogService, taskExecutor);
        paperEditor.bindToEntry(paper);
        paperEditor.addNewLinkedFile(new LinkedFile("", Path.of("proceedings.pdf"), "PDF"));

        taskExecutor.runPending();

        assertTrue(paper.getFiles().stream().anyMatch(file -> "proceedings-p2-3.pdf".equals(file.getLink())));
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void canSplitThePdfOfAProceedingsEntryOnly(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("volume.pdf"));
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(tempDir));
        LinkedFileViewModel pdf = mock(LinkedFileViewModel.class);
        when(pdf.getFile()).thenReturn(new LinkedFile("", Path.of("volume.pdf"), "PDF"));
        LinkedFilesEditorViewModel proceedingsEditor = editor(mock(DialogService.class), new DeferredTaskExecutor());
        proceedingsEditor.bindToEntry(new BibEntry(StandardEntryType.Proceedings));
        LinkedFilesEditorViewModel articleEditor = editor(mock(DialogService.class), new DeferredTaskExecutor());
        articleEditor.bindToEntry(new BibEntry(StandardEntryType.Article));

        assertEquals(List.of(true, false), List.of(proceedingsEditor.canSplitPdf(pdf), articleEditor.canSplitPdf(pdf)));
    }

    private LinkedFilesEditorViewModel editor(DialogService dialogService, DeferredTaskExecutor taskExecutor) {
        return new LinkedFilesEditorViewModel(
                StandardField.FILE,
                new EmptySuggestionProvider(),
                dialogService,
                bibDatabaseContext,
                taskExecutor,
                mock(FieldCheckers.class),
                preferences,
                new JabRefUndoManager()
        );
    }

    private static class DeferredTaskExecutor extends CurrentThreadTaskExecutor {

        private final List<Runnable> pending = new ArrayList<>();

        @Override
        public <V> Future<V> execute(BackgroundTask<V> task) {
            pending.add(() -> super.execute(task));
            return new CompletableFuture<>();
        }

        void runPending() {
            while (!pending.isEmpty()) {
                pending.removeFirst().run();
            }
        }
    }
}
