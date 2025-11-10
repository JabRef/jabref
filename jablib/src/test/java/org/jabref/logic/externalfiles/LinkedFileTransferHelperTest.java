package org.jabref.logic.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFileTransferHelperTest {
    private BibDatabaseContext sourceContext;
    private BibDatabaseContext targetContext;
    private Path sourceDir;
    private Path targetDir;
    private Path testFile;
    private BibEntry sourceEntry;
    private BibEntry targetEntry;
    private final FilePreferences filePreferences = mock(FilePreferences.class);

    @Test
    void targetDirIsParentOfSourceDir(@TempDir Path tempDir) throws Exception {
        FileTestConfiguration fileTestConfiguration = FileTestConfigurationBuilder.fileTestConfiguration()
                                    .tempDir(tempDir)
                                    .filePreferences(filePreferences)
                                    .sourceDir("lit/subdir")
                                    .sourceFile("test.pdf")
                                    .targetDir("lit")
                                    .shouldStoreFilesRelativeToBibFile(true)
                                    .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                                    .build();

        Set<BibEntry> returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(
                fileTestConfiguration.sourceContext,
                fileTestConfiguration.targetContext,
                filePreferences);

        BibEntry expectedEntry = new BibEntry()
                .withFiles(List.of(new LinkedFile("", "subdir/test.pdf", "PDF")));

        assertEquals(Set.of(expectedEntry), returnedEntries);
    }

    // region Case 2: Directory not reachable - file copying with same relative paths

    @Test
    void fileNotReachableShouldCopyFile(@TempDir Path tempDir) throws Exception {
        sourceDir = tempDir.resolve("source/targetfiles");
        targetDir = tempDir.resolve("target/sourcefiles");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);

        testFile = sourceDir.resolve("test.pdf");
        Files.createDirectories(testFile.getParent());
        Files.createFile(testFile);

        sourceContext = new BibDatabaseContext(new BibDatabase());
        sourceContext.setDatabasePath(sourceDir.resolve("personal.bib"));
        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        sourceEntry = new BibEntry();
        LinkedFile linkedFile = new LinkedFile("Test", "test.pdf", "PDF");

        sourceEntry.setFiles(List.of(linkedFile));
        targetEntry = new BibEntry(sourceEntry);
        targetEntry.setFiles(List.of(linkedFile));

        sourceContext.getDatabase().insertEntry(sourceEntry);
        targetContext.getDatabase().insertEntry(targetEntry);

        Set<BibEntry> returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
                filePreferences);

        BibEntry expectedEntry = new BibEntry();
        LinkedFile expectedLinkedFile = new LinkedFile("Test", "test.pdf", "PDF");
        expectedEntry.setFiles(List.of(expectedLinkedFile));

        Set<BibEntry> expectedEntries = Set.of(expectedEntry);

        assertEquals(expectedEntries, returnedEntries);
    }

    // endregion

    // region Case 3: Directory not reachable with different paths - file copying with directory structure

    @Test
    void fileNotReachableAndPathsDifferShouldCopyFileAndCreateDirectory(@TempDir Path tempDir) throws Exception {
        sourceDir = tempDir.resolve("source");
        targetDir = tempDir.resolve("target");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);

        testFile = sourceDir.resolve("sourcefiles/test.pdf");
        Files.createDirectories(testFile.getParent());
        Files.createFile(testFile);

        sourceContext = new BibDatabaseContext(new BibDatabase());
        sourceContext.setDatabasePath(sourceDir.resolve("personal.bib"));
        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        sourceEntry = new BibEntry();
        LinkedFile linkedFile = new LinkedFile("Test", "sourcefiles/test.pdf", "PDF");

        sourceEntry.setFiles(List.of(linkedFile));
        targetEntry = new BibEntry(sourceEntry);
        targetEntry.setFiles(List.of(linkedFile));

        sourceContext.getDatabase().insertEntry(sourceEntry);
        targetContext.getDatabase().insertEntry(targetEntry);

        Set<BibEntry> returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
                filePreferences);

        BibEntry expectedEntry = new BibEntry();
        LinkedFile expectedLinkedFile = new LinkedFile("Test", "sourcefiles/test.pdf", "PDF");
        expectedEntry.setFiles(List.of(expectedLinkedFile));

        Set<BibEntry> expectedEntries = Set.of(expectedEntry);

        assertEquals(expectedEntries, returnedEntries);

        Path expectedFile = targetDir.resolve("sourcefiles/test.pdf");
        assertTrue(Files.exists(expectedFile));
    }

    // endregion

    // region BibFile-specific directory (relative to .bib file)

    @Test
    void shouldStoreFilesRelativeToBibFile(@TempDir Path tempDir) throws Exception {
        sourceDir = tempDir.resolve("source");
        targetDir = tempDir.resolve("target");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);

        testFile = sourceDir.resolve("test.pdf");
        Files.createFile(testFile);

        sourceContext = new BibDatabaseContext(new BibDatabase());
        sourceContext.setDatabasePath(sourceDir.resolve("personal.bib"));
        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        sourceEntry = new BibEntry();
        LinkedFile linkedFile = new LinkedFile("Test", "test.pdf", "PDF");

        sourceEntry.setFiles(List.of(linkedFile));
        targetEntry = new BibEntry(sourceEntry);
        targetEntry.setFiles(List.of(linkedFile));

        sourceContext.getDatabase().insertEntry(sourceEntry);
        targetContext.getDatabase().insertEntry(targetEntry);

        Set<BibEntry> returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
                filePreferences);

        BibEntry expectedEntry = new BibEntry();
        LinkedFile expectedLinkedFile = new LinkedFile("Test", "test.pdf", "PDF");
        expectedEntry.setFiles(List.of(expectedLinkedFile));

        Set<BibEntry> expectedEntries = Set.of(expectedEntry);

        assertEquals(expectedEntries, returnedEntries);

        Path expectedFile = targetDir.resolve("test.pdf");
        assertTrue(Files.exists(expectedFile));
    }

    // endregion

    // region Global latex directory tests

    @Test
    void fileInGlobalLatexDirectoryShouldBeAccessible(@TempDir Path tempDir) throws Exception {
        targetDir = tempDir.resolve("target");
        Path globalLatexDir = tempDir.resolve("global_latex");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        when(filePreferences.getMainFileDirectory()).thenReturn(java.util.Optional.of(globalLatexDir));
        when(filePreferences.getUserAndHost()).thenReturn("testuser@testhost");
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(targetDir);
        Files.createDirectories(globalLatexDir);

        testFile = globalLatexDir.resolve("test.pdf");
        Files.createFile(testFile);

        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        LinkedFile linkedFile = new LinkedFile("Test", "test.pdf", "PDF");

        Optional<Path> foundPath = linkedFile.findIn(targetContext, filePreferences);
        assertTrue(foundPath.isPresent());
        assertTrue(foundPath.get().toString().contains("global_latex"));

        Optional<Path> primaryPath = LinkedFileTransferHelper.getPrimaryPath(targetContext, filePreferences);
        assertTrue(primaryPath.isPresent());
        assertTrue(primaryPath.get().toString().contains("global_latex"));
    }

    // endregion

    // region Library-specific directory tests

    @Test
    void fileInLibrarySpecificDirectoryShouldBeAccessible(@TempDir Path tempDir) throws Exception {
        targetDir = tempDir.resolve("target");
        Path librarySpecificDir = tempDir.resolve("library_specific");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getUserAndHost()).thenReturn("testuser@testhost");
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(targetDir);
        Files.createDirectories(librarySpecificDir);

        testFile = librarySpecificDir.resolve("test.pdf");
        Files.createFile(testFile);

        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        MetaData targetMetaData = targetContext.getMetaData();
        targetMetaData.setLibrarySpecificFileDirectory(librarySpecificDir.toString());

        LinkedFile linkedFile = new LinkedFile("Test", "test.pdf", "PDF");

        Optional<Path> foundPath = linkedFile.findIn(targetContext, filePreferences);
        assertTrue(foundPath.isPresent());
        assertTrue(foundPath.get().toString().contains("library_specific"));

        List<Path> fileDirectories = targetContext.getFileDirectories(filePreferences);
        assertTrue(fileDirectories.stream().anyMatch(path -> path.toString().contains("library_specific")));
    }

    // endregion

    // region User-specific directory tests

    @Test
    void fileInUserSpecificDirectoryShouldBeAccessible(@TempDir Path tempDir) throws Exception {
        targetDir = tempDir.resolve("target");
        Path userSpecificDir = tempDir.resolve("user_specific");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getUserAndHost()).thenReturn("testuser@testhost");
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(targetDir);
        Files.createDirectories(userSpecificDir);

        testFile = userSpecificDir.resolve("test.pdf");
        Files.createFile(testFile);

        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        MetaData targetMetaData = targetContext.getMetaData();
        targetMetaData.setUserFileDirectory("testuser@testhost", userSpecificDir.toString());

        LinkedFile linkedFile = new LinkedFile("Test", "test.pdf", "PDF");

        Optional<Path> foundPath = linkedFile.findIn(targetContext, filePreferences);
        assertTrue(foundPath.isPresent());
        assertTrue(foundPath.get().toString().contains("user_specific"));

        List<Path> fileDirectories = targetContext.getFileDirectories(filePreferences);
        assertTrue(fileDirectories.stream().anyMatch(path -> path.toString().contains("user_specific")));
    }

    // endregion

    // region Directory precedence tests

    @Test
    void userSpecificDirectoryHasHighestPrecedence(@TempDir Path tempDir) throws Exception {
        targetDir = tempDir.resolve("target");
        Path librarySpecificDir = tempDir.resolve("library_specific");
        Path userSpecificDir = tempDir.resolve("user_specific");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getUserAndHost()).thenReturn("testuser@testhost");
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(targetDir);
        Files.createDirectories(librarySpecificDir);
        Files.createDirectories(userSpecificDir);

        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        MetaData targetMetaData = targetContext.getMetaData();
        targetMetaData.setLibrarySpecificFileDirectory(librarySpecificDir.toString());
        targetMetaData.setUserFileDirectory("testuser@testhost", userSpecificDir.toString());

        Optional<Path> primaryPath = LinkedFileTransferHelper.getPrimaryPath(targetContext, filePreferences);
        assertTrue(primaryPath.isPresent());
        assertTrue(primaryPath.get().toString().contains("user_specific"));

        List<Path> fileDirectories = targetContext.getFileDirectories(filePreferences);
        assertEquals(3, fileDirectories.size());
        assertTrue(fileDirectories.getFirst().toString().contains("user_specific"));
        assertTrue(fileDirectories.get(1).toString().contains("library_specific"));
        assertTrue(fileDirectories.get(2).toString().contains("target"));
    }

    @Test
    void librarySpecificDirectoryHasHigherPrecedenceThanBibFileDirectory(@TempDir Path tempDir) throws Exception {
        targetDir = tempDir.resolve("target");
        Path librarySpecificDir = tempDir.resolve("library_specific");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getUserAndHost()).thenReturn("testuser@testhost");
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(targetDir);
        Files.createDirectories(librarySpecificDir);

        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        MetaData targetMetaData = targetContext.getMetaData();
        targetMetaData.setLibrarySpecificFileDirectory(librarySpecificDir.toString());

        Optional<Path> primaryPath = LinkedFileTransferHelper.getPrimaryPath(targetContext, filePreferences);
        assertTrue(primaryPath.isPresent());
        assertTrue(primaryPath.get().toString().contains("library_specific"));

        List<Path> fileDirectories = targetContext.getFileDirectories(filePreferences);
        assertEquals(2, fileDirectories.size());
        assertTrue(fileDirectories.getFirst().toString().contains("library_specific"));
        assertTrue(fileDirectories.get(1).toString().contains("target"));
    }

    @Test
    void globalLatexDirectoryTakesPrecedenceWhenConfigured(@TempDir Path tempDir) throws Exception {
        targetDir = tempDir.resolve("target");
        Path globalLatexDir = tempDir.resolve("global_latex");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(globalLatexDir));
        when(filePreferences.getUserAndHost()).thenReturn("testuser@testhost");
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(true);

        Files.createDirectories(targetDir);
        Files.createDirectories(globalLatexDir);

        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        Optional<Path> primaryPath = LinkedFileTransferHelper.getPrimaryPath(targetContext, filePreferences);
        assertTrue(primaryPath.isPresent());
        assertTrue(primaryPath.get().toString().contains("global_latex"));

        List<Path> fileDirectories = targetContext.getFileDirectories(filePreferences);
        assertEquals(1, fileDirectories.size());
        assertTrue(fileDirectories.getFirst().toString().contains("global_latex"));
    }

    // endregion

    // region adjustOrCopyLinkedFilesOnTransfer disabled tests

    @Test
    void shouldReturnEmptySetWhenLinkedFileTransferDisabled(@TempDir Path tempDir) throws Exception {
        sourceDir = tempDir.resolve("source");
        targetDir = tempDir.resolve("target");

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(false);

        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);

        testFile = sourceDir.resolve("sourcefiles/test.pdf");
        Files.createDirectories(testFile.getParent());
        Files.createFile(testFile);

        sourceContext = new BibDatabaseContext(new BibDatabase());
        sourceContext.setDatabasePath(sourceDir.resolve("personal.bib"));
        targetContext = new BibDatabaseContext(new BibDatabase());
        targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

        sourceEntry = new BibEntry();
        LinkedFile linkedFile = new LinkedFile("Test", "sourcefiles/test.pdf", "PDF");

        sourceEntry.setFiles(List.of(linkedFile));
        targetEntry = new BibEntry(sourceEntry);
        targetEntry.setFiles(List.of(linkedFile));

        sourceContext.getDatabase().insertEntry(sourceEntry);
        targetContext.getDatabase().insertEntry(targetEntry);

        Set<BibEntry> returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
                filePreferences);

        Path expectedFile = targetDir.resolve("sourcefiles/test.pdf");

        assertTrue(returnedEntries.isEmpty());
        assertFalse(Files.exists(expectedFile));
    }

    // endregion
}
