package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Assumption in all tests: if not contained in a library directory, paths are absolute
class LinkedFileTransferHelperTest {
    private static @TempDir Path tempDir;
    private static FilePreferences filePreferences = mock(FilePreferences.class);

    private BibDatabaseContext sourceContext;
    private BibDatabaseContext targetContext;
    private Path sourceDir;
    private Path targetDir;
    private Path testFile;
    private BibEntry sourceEntry;
    private BibEntry targetEntry;

    @ParameterizedTest
    // @CsvSource could also be used, but there is no strong typing
    @MethodSource
    void check(FileTestConfiguration fileTestConfiguration, String expectedLink, boolean keepExpectedRelative) {
        Set<BibEntry> returnedEntries = LinkedFileTransferHelper
                .adjustLinkedFilesForTarget(
                        fileTestConfiguration.sourceContext,
                        fileTestConfiguration.targetContext,
                        filePreferences);

        if (!keepExpectedRelative) {
            expectedLink = tempDir.resolve(expectedLink).toString();
        }

        BibEntry expectedEntry = new BibEntry()
                .withFiles(
                        List.of(new LinkedFile("", expectedLink, "PDF")));

        assertEquals(Set.of(expectedEntry), returnedEntries);
    }

    static Stream<Arguments> check() throws IOException {
        return Stream.of(
                // region shouldStoreFilesRelativeToBibFile

                // file next to .bib file should be copied
                Arguments.of(
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .tempDir(tempDir)
                                .filePreferences(filePreferences)
                                .shouldStoreFilesRelativeToBibFile(true)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                                .sourceBibDir("source-dir")
                                .sourceFileDir("source-dir")
                                .storeSourceFileRelative(true)
                                .targetBibDir("target-dir")
                                .build(),
                        "test.pdf",
                        true
                ),

                // Directory not reachable with different paths - file copying with directory structure
                Arguments.of(
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .tempDir(tempDir)
                                .filePreferences(filePreferences)
                                .shouldStoreFilesRelativeToBibFile(true)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                                .sourceBibDir("source-dir")
                                .sourceFileDir("source-dir/nested")
                                .storeSourceFileRelative(true)
                                .targetBibDir("target-dir")
                                .build(),
                        "nested/test.pdf",
                        true
                ),

                // targetDirIsParentOfSourceDir
                Arguments.of(
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .tempDir(tempDir)
                                .filePreferences(filePreferences)
                                .shouldStoreFilesRelativeToBibFile(true)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                                .sourceBibDir("lit/sub-dir")
                                .sourceFileDir("lit/sub-dir")
                                .storeSourceFileRelative(true)
                                .targetBibDir("lit")
                                .build(),
                        "sub-dir/test.pdf",
                        true
                ),

                // endregion

                // region not shouldStoreFilesRelativeToBibFile

                // File in main file directory linked as is
                Arguments.of(
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .tempDir(tempDir)
                                .filePreferences(filePreferences)
                                .mainFileDirectory("main-file-dir")
                                .shouldStoreFilesRelativeToBibFile(false)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                                .sourceBibDir("source-dir")
                                .sourceFileDir("main-file-dir")
                                .storeSourceFileRelative(false)
                                .targetBibDir("target-dir")
                                .build(),
                        "main-file-dir/test.pdf",
                        false
                )
                // endregion
        );
    }

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
