package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock a base directory
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);

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

    @Test
    void fixBrokenLinkForSingleExactMatch(@TempDir Path tempDir) throws IOException {
        Path directory = tempDir.resolve("files");
        Path oldPath = directory.resolve("old/minimal.pdf");
        Files.createDirectories(oldPath.getParent());
        Files.createFile(oldPath);

        BibEntry entry = new BibEntry(StandardEntryType.Misc);
        LinkedFile brokenLink = new LinkedFile("", oldPath.toString(), "PDF");
        entry.addFile(brokenLink);

        Path newPath = directory.resolve("new/minimal.pdf");
        Files.createDirectories(newPath.getParent());
        Files.move(oldPath, newPath);

        when(bibDatabaseContext.getDatabase()).thenReturn(new BibDatabase(List.of(entry)));
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(directory));

        assertTrue(entry.getFiles().stream().anyMatch(file -> file.getLink().equals(oldPath.toString())));
        viewModel.findAndFixBrokenLinks(directory);
        assertEquals(1, viewModel.resultListSize());
        assertTrue(entry.getFiles().stream().anyMatch(file -> file.getLink().equals("new/minimal.pdf")));
    }

    @Test
    void noFixForBrokenLinkOfFileAlreadyLink(@TempDir Path tempDir) throws IOException {
        Path directory = tempDir.resolve("files");
        Path oldPath = directory.resolve("old/minimal.pdf");
        Files.createDirectories(oldPath.getParent());
        Files.createFile(oldPath);

        BibEntry entry = new BibEntry(StandardEntryType.Misc);
        LinkedFile brokenLink = new LinkedFile("", oldPath.toString(), "PDF");
        entry.addFile(brokenLink);

        Path newPath = directory.resolve("new/minimal.pdf");
        Files.createDirectories(newPath.getParent());
        Files.move(oldPath, newPath);

        BibEntry entry2 = new BibEntry(StandardEntryType.Misc);
        entry2.addFile(new LinkedFile("", newPath.toString(), "PDF"));

        when(bibDatabaseContext.getDatabase()).thenReturn(new BibDatabase(List.of(entry, entry2)));
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(directory));

        assertTrue(entry.getFiles().stream().anyMatch(file -> file.getLink().equals(oldPath.toString())));
        assertTrue(entry2.getFiles().stream().anyMatch(file -> file.getLink().equals(newPath.toString())));
        viewModel.findAndFixBrokenLinks(directory);
        assertEquals(0, viewModel.resultListSize());
        assertTrue(entry.getFiles().stream().anyMatch(file -> file.getLink().equals(oldPath.toString())));
        assertTrue(entry2.getFiles().stream().anyMatch(file -> file.getLink().equals(newPath.toString())));
    }

    @Test
    void fixBrokenLinksOfMultipleFileTypes(@TempDir Path tempDir) throws IOException {
        // Entry has multiple broken links of different file types
        Path directory = tempDir.resolve("files");
        Path oldPdfPath = directory.resolve("old/document.pdf");
        Path oldTxtPath = directory.resolve("old/notes.txt");
        Path oldDocxPath = directory.resolve("old/report.docx");
        Files.createDirectories(oldPdfPath.getParent());
        Files.createFile(oldPdfPath);
        Files.createFile(oldTxtPath);
        Files.createFile(oldDocxPath);

        BibEntry entry = new BibEntry(StandardEntryType.Misc);
        entry.addFile(new LinkedFile("", oldPdfPath.toString(), "PDF"));
        entry.addFile(new LinkedFile("", oldTxtPath.toString(), "Text"));
        entry.addFile(new LinkedFile("", oldDocxPath.toString(), "Word"));

        Path newPdfPath = directory.resolve("new/document.pdf");
        Path newTxtPath = directory.resolve("new/notes.txt");
        Path newDocxPath = directory.resolve("new/report.docx");
        Files.createDirectories(newPdfPath.getParent());

        Files.move(oldPdfPath, newPdfPath);
        Files.move(oldTxtPath, newTxtPath);
        Files.move(oldDocxPath, newDocxPath);

        when(bibDatabaseContext.getDatabase()).thenReturn(new BibDatabase(List.of(entry)));
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(directory));

        // Should link each broken link to the correct new file based on type
        entry.getFiles().forEach(file -> {
            String link = file.getLink();
            if (link.endsWith(".pdf")) {
                assertEquals(oldPdfPath.toString(), link);
            } else if (link.endsWith(".txt")) {
                assertEquals(oldTxtPath.toString(), link);
            } else if (link.endsWith(".docx")) {
                assertEquals(oldDocxPath.toString(), link);
            }
        });
        viewModel.findAndFixBrokenLinks(directory);
        assertEquals(3, viewModel.resultListSize());
        entry.getFiles().forEach(file -> {
            String link = file.getLink();
            if (link.endsWith(".pdf")) {
                assertEquals("new/document.pdf", link);
            } else if (link.endsWith(".txt")) {
                assertEquals("new/notes.txt", link);
            } else if (link.endsWith(".docx")) {
                assertEquals("new/report.docx", link);
            }
        });
    }

    @Test
    void fixBrokenLinksOfMultiplePotentialMatches(@TempDir Path tempDir) throws IOException {
        Path directory = tempDir.resolve("files");
        Path oldPath = directory.resolve("old/minimal.pdf");
        Files.createDirectories(oldPath.getParent());
        Files.createFile(oldPath);

        BibEntry entry = new BibEntry(StandardEntryType.Misc);
        LinkedFile brokenLink = new LinkedFile("", oldPath.toString(), "PDF");
        entry.addFile(brokenLink);

        Path newPath = directory.resolve("new/minimal.pdf");
        Files.createDirectories(newPath.getParent());
        Files.move(oldPath, newPath);

        Path ambiguousPath = directory.resolve("new-1/minimal.pdf");
        Path ambiguousPath2 = directory.resolve("new-2/minimal.pdf");
        Path ambiguousPath3 = directory.resolve("new-3/minimal.txt");
        Files.createDirectories(ambiguousPath.getParent());
        Files.createDirectories(ambiguousPath2.getParent());
        Files.createDirectories(ambiguousPath3.getParent());
        Files.createFile(ambiguousPath);
        Files.createFile(ambiguousPath2);
        Files.createFile(ambiguousPath3);

        when(bibDatabaseContext.getDatabase()).thenReturn(new BibDatabase(List.of(entry)));
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(directory));

        // searching in the original directory, it should not link (because of multiple matches)
        assertTrue(entry.getFiles().stream().anyMatch(file -> file.getLink().equals(oldPath.toString())));
        viewModel.findAndFixBrokenLinks(directory);
        assertTrue(entry.getFiles().stream().anyMatch(file -> file.getLink().equals(oldPath.toString())));
        assertEquals(0, viewModel.resultListSize());

        // searching in a more restricted directory, it should link successfully (because only one match exists)
        viewModel.findAndFixBrokenLinks(newPath.getParent());
        assertTrue(entry.getFiles().stream().anyMatch(file -> file.getLink().equals("new/minimal.pdf")));
        assertEquals(1, viewModel.resultListSize());
    }

    @Test
    void fixBrokenLinksInMixedScenario(@TempDir Path tempDir) throws IOException {
        // Database with multiple entries:
        Path directory = tempDir.resolve("files");
        Path oldFixablePath = directory.resolve("old/fixable.pdf");
        Path oldUnFixablePath = directory.resolve("old/unFixable.pdf");
        Path untouchedPath = directory.resolve("old/untouched.pdf");
        Files.createDirectories(oldFixablePath.getParent());
        Files.createFile(oldFixablePath);
        Files.createFile(oldUnFixablePath);
        Files.createFile(untouchedPath);

        // - fixable broken links (single matches)
        BibEntry fixableEntry = new BibEntry(StandardEntryType.Misc);
        fixableEntry.addFile(new LinkedFile("", oldFixablePath.toString(), "PDF"));

        // - unfixable broken links (multiple/no matches)
        BibEntry unfixableEntry = new BibEntry(StandardEntryType.Misc);
        unfixableEntry.addFile(new LinkedFile("", oldUnFixablePath.toString(), "PDF"));

        // - valid links (should remain untouched)
        BibEntry untouchedEntry = new BibEntry(StandardEntryType.Misc);
        untouchedEntry.addFile(new LinkedFile("", untouchedPath.toString(), "PDF"));

        Path newFixablePath = directory.resolve("new/fixable.pdf");
        Path newUnFixablePath = directory.resolve("new/unFixable.pdf");
        Files.createDirectories(newFixablePath.getParent());
        Files.move(oldFixablePath, newFixablePath);
        Files.move(oldUnFixablePath, newUnFixablePath);

        Path ambiguousPath = directory.resolve("new-1/unFixable.pdf");
        Files.createDirectories(ambiguousPath.getParent());
        Files.createFile(ambiguousPath);

        when(bibDatabaseContext.getDatabase()).thenReturn(new BibDatabase(List.of(fixableEntry, unfixableEntry, untouchedEntry)));
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(directory));

        // Should only fix the fixable ones
        assertTrue(fixableEntry.getFiles().stream().anyMatch(file -> file.getLink().equals(oldFixablePath.toString())));
        assertTrue(unfixableEntry.getFiles().stream().anyMatch(file -> file.getLink().equals(oldUnFixablePath.toString())));
        assertTrue(untouchedEntry.getFiles().stream().anyMatch(file -> file.getLink().equals(untouchedPath.toString())));

        viewModel.findAndFixBrokenLinks(directory);

        assertEquals(1, viewModel.resultListSize());
        assertTrue(fixableEntry.getFiles().stream().anyMatch(file -> file.getLink().equals("new/fixable.pdf")));
        assertTrue(unfixableEntry.getFiles().stream().anyMatch(file -> file.getLink().equals(oldUnFixablePath.toString())));
        assertTrue(untouchedEntry.getFiles().stream().anyMatch(file -> file.getLink().equals(untouchedPath.toString())));
    }

    @Test
    void fixBrokenLinksInLargeDatabase(@TempDir Path tempDir) throws IOException {
        Path testRoot = tempDir.resolve("test");
        List<BibEntry> entries = new ArrayList<>();
        int entryCount = 100;
        int filesPerEntry = 3;

        for (int i = 0; i < entryCount; i++) {
            BibEntry entry = new BibEntry(StandardEntryType.Misc);

            for (int j = 0; j < filesPerEntry; j++) {
                Path dir = testRoot.resolve(String.format("category_%d/subcategory_%d/level_%d", i % 100, i % 50, i % 10));
                Path originalFile = dir.resolve(String.format("original_file_%d_%d.pdf", i, j));
                Files.createDirectories(originalFile.getParent());
                Files.createFile(originalFile);

                entry.addFile(new LinkedFile("", originalFile.toString(), "PDF"));

                Path newLocation = testRoot.resolve(String.format("new_location_%d/original_file_%d_%d.pdf", i % 200, i, j));
                Files.createDirectories(newLocation.getParent());
                Files.move(originalFile, newLocation);
            }
            entries.add(entry);
        }

        when(bibDatabaseContext.getDatabase()).thenReturn(new BibDatabase(entries));
        when(bibDatabaseContext.getFileDirectories(any())).thenReturn(List.of(testRoot));

        viewModel.findAndFixBrokenLinks(testRoot);

        assertEquals(entryCount * filesPerEntry, viewModel.resultListSize());
    }
}
