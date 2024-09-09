package org.jabref.gui.fieldeditors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.EmptySuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest("Downloads a PDF file")
class LinkedFilesEditorViewModelTest {
    private LinkedFilesEditorViewModel viewModel;
    private final GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
    private final UndoManager undoManager = mock(UndoManager.class);

    @Test
    void urlFieldShouldDownloadFile(@TempDir Path tempDir) {
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(bibDatabaseContext.getFirstExistingFileDir(any())).thenReturn(Optional.of(tempDir));

        viewModel = new LinkedFilesEditorViewModel(StandardField.FILE, new EmptySuggestionProvider(), mock(DialogService.class), bibDatabaseContext,
                           new CurrentThreadTaskExecutor(), mock(FieldCheckers.class), preferences, undoManager);

        BibEntry entry = new BibEntry().withCitationKey("test")
            .withField(StandardField.URL, "https://ceur-ws.org/Vol-847/paper6.pdf");
                viewModel.entry = entry;
        viewModel.fetchFulltext();

        assertTrue(Files.exists(tempDir.resolve("test.pdf")));
    }
}
