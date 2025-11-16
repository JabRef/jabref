package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jabref.logic.externalfiles.FileTestConfiguration.TestFileLinkMode;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

// Assumption in all tests: if not contained in a library directory, paths are absolute
class LinkedFileTransferHelperTest {
    private static @TempDir Path tempDir;
    private static @TempDir Path currentTempDir;

    private static FilePreferences filePreferences = mock(FilePreferences.class);

    @ParameterizedTest
    // @CsvSource could also be used, but there is no strong typing
    @MethodSource
    void check(FileTestConfiguration fileTestConfiguration) {
        BibEntry actualEntry = new BibEntry(fileTestConfiguration.sourceContext.getEntries().getFirst());
        BibDatabaseContext actualBibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(actualEntry)));
        actualBibDatabaseContext.setDatabasePath(fileTestConfiguration.targetContext.getDatabasePath().get());

        LinkedFileTransferHelper
                .adjustLinkedFilesForTarget(
                        filePreferences, fileTestConfiguration.sourceContext,
                        actualBibDatabaseContext,
                        actualEntry
                );

        // expectedLink = fileTestConfiguration.targetContext.getDatabase().getEntries().getFirst().getFiles();

        assertEquals(fileTestConfiguration.targetContext.getDatabase().getEntries().getFirst(), actualEntry);
    }

    static Path getNextTempDir() {
        currentTempDir = tempDir.resolve(Integer.toString(ThreadLocalRandom.current().nextInt()));
        return currentTempDir;
    }

    static Stream<FileTestConfiguration> check() throws IOException {
        return Stream.of(
                // region shouldStoreFilesRelativeToBibFile

                // file next to .bib file should be copied
                FileTestConfigurationBuilder
                        .fileTestConfiguration()
                        .tempDir(getNextTempDir())
                        .filePreferences(filePreferences)
                        .shouldStoreFilesRelativeToBibFile(true)
                        .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                        .sourceBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("source-dir")
                                        .pdfFileDir("source-dir")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB)
                                        .build()
                        )
                        .targetBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("target-dir")
                                        .pdfFileDir("target-dir")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB)
                                        .build()
                        )
                        .build(),

                // Directory not reachable with different paths - file copying with directory structure
                FileTestConfigurationBuilder
                        .fileTestConfiguration()
                        .tempDir(getNextTempDir())
                        .filePreferences(filePreferences)
                        .shouldStoreFilesRelativeToBibFile(true)
                        .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                        .sourceBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("source-dir")
                                        .pdfFileDir("source-dir/nested")
                                        .fileLinkMode(FileTestConfiguration.TestFileLinkMode.RELATIVE_TO_BIB)
                                        .build()
                        )
                        .targetBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("target-dir")
                                        .pdfFileDir("target-dir/nested")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB)
                                        .build()
                        )
                        .build(),

                // targetDirIsParentOfSourceDir
                FileTestConfigurationBuilder
                        .fileTestConfiguration()
                        .tempDir(getNextTempDir())
                        .filePreferences(filePreferences)
                        .shouldStoreFilesRelativeToBibFile(true)
                        .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                        .sourceBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("lit/sub-dir")
                                        .pdfFileDir("lit/sub-dir")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB)
                                        .build()
                        )
                        .targetBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("lit")
                                        .pdfFileDir("lit/sub-dir")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB)
                                        .build()
                        )
                        .build(),

                // endregion

                // region not shouldStoreFilesRelativeToBibFile

                // File in main file directory linked as is
                FileTestConfigurationBuilder
                        .fileTestConfiguration()
                        .tempDir(getNextTempDir())
                        .filePreferences(filePreferences)
                        .mainFileDir("main-file-dir")
                        .shouldStoreFilesRelativeToBibFile(false)
                        .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                        .sourceBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("source-bib-dir")
                                        .pdfFileDir("main-file-dir")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_MAIN_FILE_DIR)
                                        .build()
                        )
                        .targetBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("target-bib-dir")
                                        .pdfFileDir("main-file-dir/sub-dir")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_MAIN_FILE_DIR)
                                        .build()
                        )
                        .build()
                                        /*
                // same library-specific directory
                FileTestConfigurationBuilder
                        .fileTestConfiguration()
                        .tempDir(getNextTempDir())
                        .filePreferences(filePreferences)
                        .mainFileDir("main-file-dir")
                        .shouldStoreFilesRelativeToBibFile(false)
                        .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                        .sourceBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("source-bib-dir")
                                        .librarySpecificFileDir("library-specific")
                                        .pdfFileDir("library-specific")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR)
                                        .build()
                        )
                        .targetBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("target-bib-dir")
                                        .librarySpecificFileDir("library-specific")
                                        .pdfFileDir("library-specific")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR)
                                        .build()
                        )
                        .build(),

                // same user-specific file-directory
                FileTestConfigurationBuilder
                        .fileTestConfiguration()
                        .tempDir(getNextTempDir())
                        .filePreferences(filePreferences)
                        .mainFileDir("main-file-dir")
                        .shouldStoreFilesRelativeToBibFile(false)
                        .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                        .sourceBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("source-bib-dir")
                                        .librarySpecificFileDir("library-specific")
                                        .userSpecificFileDir("user-specific")
                                        .pdfFileDir("user-specific")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_USER_SPECIFIC_DIR)
                                        .build()
                        )
                        .targetBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("target-bib-dir")
                                        .librarySpecificFileDir("library-specific")
                                        .userSpecificFileDir("user-specific")
                                        .pdfFileDir("user-specific")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_USER_SPECIFIC_DIR)
                                        .build()
                        )
                        .build(),

                // copied from (now unreachable) library-specific dir to other library-specific dir
                FileTestConfigurationBuilder
                        .fileTestConfiguration()
                        .tempDir(getNextTempDir())
                        .filePreferences(filePreferences)
                        .mainFileDir("main-file-dir")
                        .shouldStoreFilesRelativeToBibFile(false)
                        .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                        .sourceBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("source-bib-dir")
                                        .librarySpecificFileDir("library-specific-source")
                                        .userSpecificFileDir("user-specific-source") // functionality should be independent of configured or not
                                        .pdfFileDir("library-specific-source")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR)
                                        .build()
                        )
                        .targetBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("target-bib-dir")
                                        .librarySpecificFileDir("library-specific-target")
                                        .pdfFileDir("library-specific-target")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR)
                                        .build()
                        )
                        .build(),

                // copied from (unreachable) user-specific dir to other library-specific dir (no user-specific existing there)
                FileTestConfigurationBuilder
                        .fileTestConfiguration()
                        .tempDir(getNextTempDir())
                        .filePreferences(filePreferences)
                        .mainFileDir("main-file-dir")
                        .shouldStoreFilesRelativeToBibFile(false)
                        .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                        .sourceBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("source-bib-dir")
                                        .librarySpecificFileDir("library-specific-source")
                                        .userSpecificFileDir("user-specific-source")
                                        .pdfFileDir("user-specific-source")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_USER_SPECIFIC_DIR)
                                        .build()
                        )
                        .targetBibTestConfiguration(
                                BibTestConfigurationBuilder
                                        .bibTestConfiguration()
                                        .tempDir(currentTempDir)
                                        .bibDir("target-bib-dir")
                                        .librarySpecificFileDir("library-specific-target")
                                        .pdfFileDir("library-specific-target")
                                        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR)
                                        .build()
                        )
                        .build()

                // endregion
                */
        );
    }
}
