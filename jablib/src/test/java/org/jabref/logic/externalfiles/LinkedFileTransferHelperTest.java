package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.FileTestConfiguration.TestFileLinkMode;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

// Assumption in all tests: if not contained in a library directory, paths are absolute
class LinkedFileTransferHelperTest {
    private static int testNumber = 0;

    private @TempDir Path tempDir;

    @ParameterizedTest
    // @CsvSource could also be used, but there is no strong typing
    @MethodSource
    void check(BibTestConfigurationBuilder sourceBibTestConfigurationBuilder,
               BibTestConfigurationBuilder targetBibTestConfigurationBuilder,
               FileTestConfigurationBuilder fileTestConfigurationBuilder) throws IOException {
        FilePreferences filePreferences = mock(FilePreferences.class);
        BibTestConfiguration sourceBibTestConfiguration = sourceBibTestConfigurationBuilder.tempDir(tempDir).build();
        BibTestConfiguration targetBibTestConfiguration = targetBibTestConfigurationBuilder.tempDir(tempDir).build();
        FileTestConfiguration fileTestConfiguration = fileTestConfigurationBuilder
                .sourceBibTestConfiguration(sourceBibTestConfiguration)
                .targetBibTestConfiguration(targetBibTestConfiguration)
                .tempDir(tempDir)
                .filePreferences(filePreferences)
                .build();
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

    static Stream<Arguments> check() throws IOException {
        return Stream.of(
                // region shouldStoreFilesRelativeToBibFile

                // file next to .bib file should be copied
                Arguments.of(
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("source-dir")
                                .pdfFileDir("source-dir")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB),
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("target-dir")
                                .pdfFileDir("target-dir")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB),
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .number(testNumber++)
                                .shouldStoreFilesRelativeToBibFile(true)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                ),

                // Directory not reachable with different paths - file copying with directory structure
                Arguments.of(
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("source-dir")
                                .pdfFileDir("source-dir/nested")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB),
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("target-dir")
                                .pdfFileDir("target-dir/nested")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB),
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .number(testNumber++)
                                .shouldStoreFilesRelativeToBibFile(true)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                ),

                // targetDirIsParentOfSourceDir
                Arguments.of(
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("lit/sub-dir")
                                .pdfFileDir("lit/sub-dir")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB),
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("lit")
                                .pdfFileDir("lit/sub-dir")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB),
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .number(testNumber++)
                                .shouldStoreFilesRelativeToBibFile(true)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                ),
                // endregion

                // region not shouldStoreFilesRelativeToBibFile

                // File in main file directory linked as is
                Arguments.of(
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("source-bib-dir")
                                .pdfFileDir("main-file-dir")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_MAIN_FILE_DIR),
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("target-bib-dir")
                                .pdfFileDir("main-file-dir/sub-dir")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_MAIN_FILE_DIR),
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .number(testNumber++)
                                .mainFileDir("main-file-dir")
                                .shouldStoreFilesRelativeToBibFile(false)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                ),

                // same library-specific directory
                Arguments.of(
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("source-bib-dir")
                                .librarySpecificFileDir("library-specific")
                                .pdfFileDir("library-specific")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR),
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("target-bib-dir")
                                .librarySpecificFileDir("library-specific")
                                .pdfFileDir("library-specific")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR),
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .number(testNumber++)
                                .mainFileDir("main-file-dir")
                                .shouldStoreFilesRelativeToBibFile(false)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)

                ),

                // same user-specific file-directory
                Arguments.of(
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("source-bib-dir")
                                .librarySpecificFileDir("library-specific")
                                .userSpecificFileDir("user-specific")
                                .pdfFileDir("user-specific")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_USER_SPECIFIC_DIR),
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("target-bib-dir")
                                .librarySpecificFileDir("library-specific")
                                .userSpecificFileDir("user-specific")
                                .pdfFileDir("user-specific")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_USER_SPECIFIC_DIR),
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .number(testNumber++)
                                .mainFileDir("main-file-dir")
                                .shouldStoreFilesRelativeToBibFile(false)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                ),

                // copied from (now unreachable) library-specific dir to other library-specific dir
                Arguments.of(
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("source-bib-dir")
                                .librarySpecificFileDir("library-specific-source")
                                .userSpecificFileDir("user-specific-source") // functionality should be independent of configured or not
                                .pdfFileDir("library-specific-source")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR),
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("target-bib-dir")
                                .librarySpecificFileDir("library-specific-target")
                                .pdfFileDir("library-specific-target")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR),
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .number(testNumber++)
                                .mainFileDir("main-file-dir")
                                .shouldStoreFilesRelativeToBibFile(false)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                ),

                // copied from (unreachable) user-specific dir to other library-specific dir (no user-specific existing there)
                Arguments.of(
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("source-bib-dir")
                                .librarySpecificFileDir("library-specific-source")
                                .userSpecificFileDir("user-specific-source")
                                .pdfFileDir("user-specific-source")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_USER_SPECIFIC_DIR),
                        BibTestConfigurationBuilder
                                .bibTestConfiguration()
                                .bibDir("target-bib-dir")
                                .librarySpecificFileDir("library-specific-target")
                                .pdfFileDir("library-specific-target")
                                .fileLinkMode(TestFileLinkMode.RELATIVE_TO_LIBRARY_SPECIFIC_DIR),
                        FileTestConfigurationBuilder
                                .fileTestConfiguration()
                                .number(testNumber++)
                                .mainFileDir("main-file-dir")
                                .shouldStoreFilesRelativeToBibFile(false)
                                .shouldAdjustOrCopyLinkedFilesOnTransfer(true)
                )
                // endregion
        );
    }
}
