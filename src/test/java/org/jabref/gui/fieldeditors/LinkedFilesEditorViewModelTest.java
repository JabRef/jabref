package org.jabref.gui.fieldeditors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.EmptySuggestionProvider;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;
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
    private final PreferencesService preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
    private final FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);

    @Test
    void urlFieldShouldDownloadFile(@TempDir Path tempDir) {
        when(preferencesService.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(StandardExternalFileType.values()));
        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(bibDatabaseContext.getFirstExistingFileDir(any())).thenReturn(Optional.of(tempDir));

        viewModel = new LinkedFilesEditorViewModel(StandardField.FILE, new EmptySuggestionProvider(), mock(DialogService.class), bibDatabaseContext,
                           new CurrentThreadTaskExecutor(), mock(FieldCheckers.class), preferencesService);

        BibEntry entry = new BibEntry().withCitationKey("test")
            .withField(StandardField.URL, "https://ceur-ws.org/Vol-847/paper6.pdf");
                viewModel.entry = entry;
        viewModel.fetchFulltext();

        assertTrue(Files.exists(tempDir.resolve("test.pdf")));
    }
}
