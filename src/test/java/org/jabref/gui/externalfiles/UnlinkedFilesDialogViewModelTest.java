package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TreeItem;

import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Mock file preferences to return a base directory
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getWorkingDirectory()).thenReturn(Paths.get("C:/test/base"));

        // Mock the state manager to provide an active database
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(bibDatabaseContext));

        viewModel = new UnlinkedFilesDialogViewModel(
                null,  // dialogService, not used in this test
                null,  // undoManager, not used in this test
                null,  // fileUpdateMonitor, not used in this test
                guiPreferences,
                stateManager,
                taskExecutor
        );
    }

    @Test
    public void testStartImportWithValidFiles() throws Exception {
        // Create temporary test files
        tempDir = Files.createTempDirectory("testDir");
        subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);

        // Create test files: one in the main directory and one in the subdirectory
        file1 = Files.createTempFile(tempDir, "file1", ".pdf"); // Main directory
        file2 = Files.createTempFile(subDir, "file2", ".txt");

        // Arrange: Mock file nodes with the absolute paths of the temporary files
        FileNodeViewModel fileNode1 = mock(FileNodeViewModel.class);
        FileNodeViewModel fileNode2 = mock(FileNodeViewModel.class);

        // Mock the getPath method to return the expected paths
        when(fileNode1.getPath()).thenReturn(file1);
        when(fileNode2.getPath()).thenReturn(file2);

        // Create TreeItem for each FileNodeViewModel
        TreeItem<FileNodeViewModel> treeItem1 = new TreeItem<>(fileNode1);
        TreeItem<FileNodeViewModel> treeItem2 = new TreeItem<>(fileNode2);

        // Initialize SimpleListProperty and assign ObservableList
        SimpleListProperty<TreeItem<FileNodeViewModel>> checkedFileListProperty =
                new SimpleListProperty<>(FXCollections.observableArrayList(treeItem1, treeItem2));

        // Assert that the list contains 2 items
        assertEquals(2, checkedFileListProperty.get().size());
        assertEquals(file1, checkedFileListProperty.get().get(0).getValue().getPath());
        assertEquals(file2, checkedFileListProperty.get().get(1).getValue().getPath());

        Path directory = tempDir; // Base directory for relativization

        // Create list of relative paths
        List<Path> fileList = checkedFileListProperty.stream()
                                                     .map(item -> item.getValue().getPath())
                                                     .filter(path -> path.toFile().isFile())
                                                     .map(path -> directory.relativize(path))
                                                     .collect(Collectors.toList());
        assertEquals(
                List.of(directory.relativize(file1), directory.relativize(file2)),
                fileList,
                "fileList should contain exactly the relative paths of file1.pdf and file2.txt"
        );
    }
}
