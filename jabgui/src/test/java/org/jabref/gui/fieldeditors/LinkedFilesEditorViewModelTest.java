package org.jabref.gui.fieldeditors;

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
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
}
