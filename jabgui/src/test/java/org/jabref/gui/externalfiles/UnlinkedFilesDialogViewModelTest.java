package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TreeItem;

import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class UnlinkedFilesDialogViewModelTest {
    @TempDir
    Path tempDir;
    @TempDir
    Path subDir;
    @TempDir
    Path file1;
    @TempDir
    Path file2;
    @Mock
    private TaskExecutor taskExecutor;
    @Mock
    private GuiPreferences guiPreferences;
    @Mock
    private StateManager stateManager;
    @Mock
    private BibDatabaseContext bibDatabaseContext;

    private UnlinkedFilesDialogViewModel viewModel;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock a base directory
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(guiPreferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(guiPreferences.getExternalApplicationsPreferences()).thenReturn(mock(org.jabref.gui.frame.ExternalApplicationsPreferences.class, org.mockito.Answers.RETURNS_DEEP_STUBS));
        when(guiPreferences.getImporterPreferences()).thenReturn(mock(ImporterPreferences.class, org.mockito.Answers.RETURNS_DEEP_STUBS));
        when(guiPreferences.getCitationKeyPatternPreferences()).thenReturn(mock(CitationKeyPatternPreferences.class, org.mockito.Answers.RETURNS_DEEP_STUBS));

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(guiPreferences.getFieldPreferences()).thenReturn(fieldPreferences);

        when(guiPreferences.getImporterPreferences().getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getWorkingDirectory()).thenReturn(Path.of("C:/test/base"));

        // Mock the state manager to provide an active database
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(bibDatabaseContext));

        viewModel = new UnlinkedFilesDialogViewModel(
                null,
                null,
                null,
                guiPreferences,
                stateManager,
                taskExecutor
        );
    }

    @Test
    public void startImportWithValidFilesTest() throws IOException {
        // Create temporary test files
        tempDir = Files.createTempDirectory("testDir");
        subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);

        // Create test files: one in the main directory and one in the subdirectory
        file1 = Files.createTempFile(tempDir, "file1", ".pdf");
        file2 = Files.createTempFile(subDir, "file2", ".txt");

        // Mock file nodes with the absolute paths of the temporary files
        FileNodeViewModel fileNode1 = mock(FileNodeViewModel.class);
        FileNodeViewModel fileNode2 = mock(FileNodeViewModel.class);

        when(fileNode1.getPath()).thenReturn(file1);
        when(fileNode2.getPath()).thenReturn(file2);

        // Create TreeItem for each FileNodeViewModel
        TreeItem<FileNodeViewModel> treeItem1 = new TreeItem<>(fileNode1);
        TreeItem<FileNodeViewModel> treeItem2 = new TreeItem<>(fileNode2);

        SimpleListProperty<TreeItem<FileNodeViewModel>> checkedFileListProperty =
                new SimpleListProperty<>(FXCollections.observableArrayList(treeItem1, treeItem2));

        assertEquals(2, checkedFileListProperty.get().size());
        assertEquals(file1, checkedFileListProperty.get().getFirst().getValue().getPath());
        assertEquals(file2, checkedFileListProperty.get().getLast().getValue().getPath());

        Path directory = tempDir; // Base directory for relativization

        // Create list of relative paths
        List<Path> fileList = checkedFileListProperty.stream()
                                                     .map(item -> item.getValue().getPath())
                                                     .filter(path -> path.toFile().isFile())
                                                     .map(directory::relativize)
                                                     .collect(Collectors.toList());
        assertEquals(
                List.of(directory.relativize(file1), directory.relativize(file2)),
                fileList,
                "fileList should contain exactly the relative paths of file1.pdf and file2.txt"
        );
    }

    /// Library with 100 entries whose citation keys match 100 unlinked PDFs.
    /// Resolving the related entries of every listed file must not take a directory walk per (file, entry) pair.
    @Test
    void relatedEntriesForHundredUnlinkedFiles(@TempDir Path directory) throws IOException {
        BibDatabaseContext databaseContext = spy(new BibDatabaseContext());
        databaseContext.setDatabasePath(directory.resolve("library.bib"));
        List<BibEntry> entries = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String key = "author%03d".formatted(i);
            BibEntry entry = new BibEntry(StandardEntryType.Article)
                    .withCitationKey(key)
                    .withField(StandardField.AUTHOR, key)
                    .withField(StandardField.TITLE, "Title " + i);
            entries.add(entry);
            Files.createFile(directory.resolve(key + ".pdf"));
        }
        databaseContext.getDatabase().insertEntries(entries);

        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(guiPreferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);
        when(guiPreferences.getAutoLinkPreferences())
                .thenReturn(new AutoLinkPreferences(AutoLinkPreferences.CitationKeyDependency.START, "", false, ';'));
        FilePreferences filePreferences = guiPreferences.getFilePreferences();
        when(filePreferences.getUserAndHost()).thenReturn("user-host");
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(directory));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        viewModel = new UnlinkedFilesDialogViewModel(null, null, null, guiPreferences, stateManager, new CurrentThreadTaskExecutor());
        viewModel.directoryPathProperty().set(directory.toString());
        viewModel.selectedExtensionProperty().set(new FileExtensionViewModel(StandardFileType.PDF, externalApplicationsPreferences));
        viewModel.selectedDateProperty().set(DateRange.ALL_TIME);
        viewModel.selectedSortProperty().set(ExternalFileSorter.DEFAULT);

        viewModel.startSearch();
        assertEquals(100, viewModel.treeRootProperty().get().orElseThrow().getFileCount());

        // Rendering the file tree asks for the related entries of each listed file (repeatedly, on the FX thread);
        // that must be answered from the search result without scanning the library and its file directories again
        clearInvocations(databaseContext);
        for (BibEntry entry : entries) {
            Path pdf = directory.resolve(entry.getCitationKey().orElseThrow() + ".pdf");
            assertEquals(List.of(entry), viewModel.getRelatedEntriesForFiles(pdf));
        }
        verifyNoInteractions(databaseContext);
    }
}
