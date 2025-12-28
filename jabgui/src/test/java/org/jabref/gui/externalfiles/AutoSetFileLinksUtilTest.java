package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import javafx.collections.FXCollections;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoSetFileLinksUtilTest {

    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final AutoLinkPreferences autoLinkPrefs = mock(AutoLinkPreferences.class);
    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final BibEntry entry = new BibEntry(StandardEntryType.Article);
    private Path path = null;

    /**
     * Modified version of AutoLinkFilesAction.linkFilesTask.onLinkedFilesUpdated.
     */
    private final BiConsumer<List<LinkedFile>, BibEntry> onLinkedFilesUpdated = (newLinkedFiles, entry) -> {
        // Undo manager related code is removed

        // Directly update BibEntry model instead of doing it in UI thread by `UiTaskExecutor.runAndWaitInJavaFXThread`,
        // which is not properly set up and is out of the unit test scope
        entry.setFiles(newLinkedFiles);
    };

    @BeforeEach
    void setUp(@TempDir Path folder) throws IOException {
        path = folder.resolve("CiteKey.pdf");
        Files.createFile(path);
        entry.setCitationKey("CiteKey");
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        when(autoLinkPrefs.getRegularExpression()).thenReturn("");
        when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.START);
        when(autoLinkPrefs.getKeywordSeparator()).thenReturn(';');
    }

    @Test
    void findAssociatedNotLinkedFilesSuccess() throws IOException {
        when(databaseContext.getFileDirectories(any())).thenReturn(List.of(path.getParent()));
        List<LinkedFile> expected = List.of(new LinkedFile("", Path.of("CiteKey.pdf"), "PDF"));
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        Collection<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(expected, actual);
    }

    @Test
    void findAssociatedNotLinkedFilesForEmptySearchDir() throws IOException {
        when(databaseContext.getFileDirectories(any())).thenReturn(List.of());
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        Collection<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(List.of(), actual);
    }

    @Test
    void findOneAssociatedNotLinkedFile(@TempDir Path tempDir) throws IOException {
        Path directory = tempDir.resolve("files");
        Path oldPath = directory.resolve("old/minimal.pdf");
        Files.createDirectories(oldPath.getParent());
        Files.createFile(oldPath);

        LinkedFile stale = new LinkedFile("", oldPath.toString(), "PDF");
        BibEntry entry = new BibEntry(StandardEntryType.Misc);
        entry.addFile(stale);

        String newFile = "new/minimal.pdf";
        Path newPath = directory.resolve(newFile);
        Files.createDirectories(newPath.getParent());
        Files.move(oldPath, newPath);

        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(filePreferences)).thenReturn(List.of(directory));

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(
                context,
                externalApplicationsPreferences,
                filePreferences,
                autoLinkPrefs);

        Collection<LinkedFile> matches = util.findAssociatedNotLinkedFiles(entry);

        assertEquals(1, matches.size());
        assertEquals(newFile,
                matches.stream().findFirst().map(LinkedFile::getLink).orElse(""));
    }

    @Test
    void findAllAssociatedNotLinkedFilesInsteadOfTheFirstOne(@TempDir Path tempDir) throws IOException {
        Path directory = tempDir.resolve("files");
        Path oldPath = directory.resolve("old/minimal.pdf");
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withFiles(List.of(new LinkedFile("", oldPath.toString(), "PDF")));

        // Simulate a file move
        String newPath1String = "new1/minimal.pdf";
        Path newPath1 = directory.resolve(newPath1String);
        Files.createDirectories(newPath1.getParent());
        Files.createFile(newPath1);

        // Create a second copy of the file
        String newPath2String = "new2/minimal.pdf";
        Path newPath2 = directory.resolve(newPath2String);
        Files.createDirectories(newPath2.getParent());
        Files.copy(newPath1, newPath2);

        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(filePreferences)).thenReturn(List.of(directory));

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(
                context,
                externalApplicationsPreferences,
                filePreferences,
                autoLinkPrefs);

        Collection<LinkedFile> matchedFiles = util.findAssociatedNotLinkedFiles(entry);
        Set<LinkedFile> expected = Set.of(
                new LinkedFile("", newPath1String, "PDF"),
                new LinkedFile("", newPath2String, "PDF"));
        assertEquals(expected, Set.copyOf(matchedFiles));
    }

    @Test
    void findAllAssociatedNotLinkedFilesAndNotRepeated(@TempDir Path tempDir) throws IOException {
        when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.START);

        // File and folder
        Path subdir = tempDir.resolve("subdir");
        Files.createDirectory(subdir);
        Path fileA = tempDir.resolve("CK_A.pdf");
        Files.createFile(fileA);
        Path fileB = tempDir.resolve("CK_B.pdf");
        Files.createFile(fileB);
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withFiles(List.of(
                        new LinkedFile("", "subdir/CK_B.pdf", "PDF")
                ));
        entry.setCitationKey("CK");

        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(filePreferences)).thenReturn(List.of(tempDir));
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(
                context,
                externalApplicationsPreferences,
                filePreferences,
                autoLinkPrefs);

        // find by citation will return CK_A.pdf and CK_B.pdf
        // find by broken linked file name will return CK_B.pdf
        Collection<LinkedFile> matchedFiles = util.findAssociatedNotLinkedFiles(entry);
        Set<LinkedFile> expected = Set.of(
                new LinkedFile("", "CK_A.pdf", "PDF"),
                new LinkedFile("", "CK_B.pdf", "PDF"));
        assertEquals(expected, Set.copyOf(matchedFiles));
    }

    @Nested
    @DisplayName("linkAssociatedFiles")
    class linkAssociatedFiles {

        @Nested
        @DisplayName("byCitationKeyOnly")
        class byCitationKeyOnly {

            @Nested
            @DisplayName("configuredCitationKeyDependencyWithStart")
            class configuredCitationKeyDependencyWithStart {

                /**
                 * └── citationKeyxxx.pdf
                 */
                @Test
                void autoLinkByCitationKeyStartAtRootFolder(@TempDir Path root) throws Exception {
                    when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.START);
                    when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                    String citationKey = "thisIsACitationKey";

                    // File and folder before moving
                    String fileName = String.format("%s_foobar.pdf", citationKey);
                    Path fileA = root.resolve(fileName);
                    Files.createFile(fileA);

                    // Setup BibEntry without file
                    BibEntry entryA = new BibEntry(StandardEntryType.Article);
                    entryA.setCitationKey(citationKey);
                    List<BibEntry> entries = List.of(entryA);

                    // Run auto-link
                    AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                            externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                    util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                    // Check auto-link result
                    List<LinkedFile> expect = List.of(new LinkedFile("", Path.of(fileName), "PDF"));
                    List<LinkedFile> actual = entryA.getFiles();
                    assertEquals(expect, actual);
                }

                /**
                 * └── A
                 *     └── B
                 *         └── citationKeyxxx.pdf
                 */
                @Test
                void autoLinkByCitationKeyStartAtSubFolder(@TempDir Path root) throws Exception {
                    when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.START);
                    when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                    String citationKey = "thisIsACitationKey";

                    // File and folder before moving
                    Path folderA = root.resolve("A");
                    Files.createDirectory(folderA);
                    Path folderB = folderA.resolve("B");
                    Files.createDirectory(folderB);
                    String fileName = String.format("%s_foobar.pdf", citationKey);
                    Path fileA = folderB.resolve(fileName);
                    Files.createFile(fileA);

                    // Setup BibEntry without file
                    BibEntry entryA = new BibEntry(StandardEntryType.Article);
                    entryA.setCitationKey(citationKey);
                    List<BibEntry> entries = List.of(entryA);

                    // Run auto-link
                    AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                            externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                    util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                    // Check auto-link result
                    List<LinkedFile> expect = List.of(new LinkedFile("", Path.of(String.format("A/B/%s", fileName)), "PDF"));
                    List<LinkedFile> actual = entryA.getFiles();
                    assertEquals(expect, actual);
                }

                /**
                 * └── A
                 *     └── B
                 *         ├── citationKey.pdf
                 *         └── citationKeyxxx.pdf
                 */
                @Test
                void autoLinkByCitationKeyStartMatchingTwoFilesAtSubFolder(@TempDir Path root) throws Exception {
                    when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.START);
                    when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                    String citationKey = "thisIsACitationKey";

                    // File and folder before moving
                    Path folderA = root.resolve("A");
                    Files.createDirectory(folderA);
                    Path folderB = folderA.resolve("B");
                    Files.createDirectory(folderB);
                    String fileNameA = String.format("%s_foobar.pdf", citationKey);
                    Path fileA = folderB.resolve(fileNameA);
                    Files.createFile(fileA);
                    String fileNameB = String.format("%s.pdf", citationKey);
                    Path fileB = folderB.resolve(fileNameB);
                    Files.createFile(fileB);

                    // Setup BibEntry without file
                    BibEntry entryA = new BibEntry(StandardEntryType.Article);
                    entryA.setCitationKey(citationKey);
                    List<BibEntry> entries = List.of(entryA);

                    // Run auto-link
                    AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                            externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                    util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                    // Check auto-link result

                    List<LinkedFile> expect = List.of(
                            new LinkedFile("", Path.of(String.format("A/B/%s", fileNameA)), "PDF"),
                            new LinkedFile("", Path.of(String.format("A/B/%s", fileNameB)), "PDF")
                    );
                    List<LinkedFile> actual = entryA.getFiles();
                    assertEquals(Set.copyOf(expect), Set.copyOf(actual));
                }

                /**
                 * └── A
                 *     └── B
                 *         ├── citationKey.pdf
                 *         └── citationKeyxxx.pdf
                 */
                @Test
                void autoLinkByCitationKeyStartMatchingTwoFilesAtSubFolderAndOneMatchABrokenLinkedFileName(@TempDir Path root) throws Exception {
                    when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.START);
                    when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                    String citationKey = "thisIsACitationKey";

                    // File and folder before moving
                    Path folderA = root.resolve("A");
                    Files.createDirectory(folderA);
                    Path folderB = folderA.resolve("B");
                    Files.createDirectory(folderB);
                    String fileNameA = String.format("%s_foobar.pdf", citationKey);
                    Path fileA = folderB.resolve(fileNameA);
                    Files.createFile(fileA);
                    String fileNameB = String.format("%s.pdf", citationKey);
                    Path fileB = folderB.resolve(fileNameB);
                    Files.createFile(fileB);

                    // Setup BibEntry without file
                    BibEntry entryA = new BibEntry(StandardEntryType.Article);
                    entryA.setCitationKey(citationKey);
                    entryA.addFile(new LinkedFile("", fileNameB, "PDF"));
                    List<BibEntry> entries = List.of(entryA);

                    // Run auto-link
                    AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                            externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                    util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                    // Check auto-link result

                    List<LinkedFile> expect = List.of(
                            new LinkedFile("", Path.of(String.format("A/B/%s", fileNameA)), "PDF"),
                            new LinkedFile("", Path.of(String.format("A/B/%s", fileNameB)), "PDF")
                    );
                    List<LinkedFile> actual = entryA.getFiles();
                    assertEquals(Set.copyOf(expect), Set.copyOf(actual));
                }
            }

            @Nested
            @DisplayName("configuredCitationKeyDependencyWithExact")
            class configuredCitationKeyDependencyWithExact {

                /**
                 * └── citationKeyxxx.pdf
                 */
                @Test
                void autoLinkByCitationKeyExactAtRootFolderDoesNotWorkWithNotExactName(@TempDir Path root) throws Exception {
                    when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.EXACT);
                    when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                    String citationKey = "thisIsACitationKey";

                    // File and folder before moving
                    String fileName = String.format("%s_foobar.pdf", citationKey);
                    Path fileA = root.resolve(fileName);
                    Files.createFile(fileA);

                    // Setup BibEntry without file
                    BibEntry entryA = new BibEntry(StandardEntryType.Article);
                    entryA.setCitationKey(citationKey);
                    List<BibEntry> entries = List.of(entryA);

                    // Run auto-link
                    AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                            externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                    util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                    // Check auto-link result
                    List<LinkedFile> expect = List.of();
                    List<LinkedFile> actual = entryA.getFiles();
                    assertEquals(expect, actual);
                }

                /**
                 * └── citationKey.pdf
                 */
                @Test
                void autoLinkByCitationKeyExactAtRootFolder(@TempDir Path root) throws Exception {
                    when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.EXACT);
                    when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                    String citationKey = "thisIsACitationKey";

                    // File and folder before moving
                    String fileName = String.format("%s.pdf", citationKey);
                    Path fileA = root.resolve(fileName);
                    Files.createFile(fileA);

                    // Setup BibEntry without file
                    BibEntry entryA = new BibEntry(StandardEntryType.Article);
                    entryA.setCitationKey(citationKey);
                    List<BibEntry> entries = List.of(entryA);

                    // Run auto-link
                    AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                            externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                    util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                    // Check auto-link result
                    List<LinkedFile> expect = List.of(new LinkedFile("", Path.of(fileName), "PDF"));
                    List<LinkedFile> actual = entryA.getFiles();
                    assertEquals(expect, actual);
                }

                /**
                 * └── A
                 *     └── B
                 *         └── citationKey.pdf
                 */
                @Test
                void autoLinkByCitationKeyExactAtSubFolder(@TempDir Path root) throws Exception {
                    when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.EXACT);
                    when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                    String citationKey = "thisIsACitationKey";

                    // File and folder before moving
                    Path folderA = root.resolve("A");
                    Files.createDirectory(folderA);
                    Path folderB = folderA.resolve("B");
                    Files.createDirectory(folderB);
                    String fileName = String.format("%s.pdf", citationKey);
                    Path fileA = folderB.resolve(fileName);
                    Files.createFile(fileA);

                    // Setup BibEntry without file
                    BibEntry entryA = new BibEntry(StandardEntryType.Article);
                    entryA.setCitationKey(citationKey);
                    List<BibEntry> entries = List.of(entryA);

                    // Run auto-link
                    AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                            externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                    util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                    // Check auto-link result
                    List<LinkedFile> expect = List.of(new LinkedFile("", Path.of(String.format("A/B/%s", fileName)), "PDF"));
                    List<LinkedFile> actual = entryA.getFiles();
                    assertEquals(expect, actual);
                }
            }

            // configuredCitationKeyDependencyWithRegex omitted
        }

        @Nested
        @DisplayName("byBrokenLinkedFileNameOnly")
        class byBrokenLinkedFileNameOnly {
            /**
             * CK: WeDoNotCare
             * └── broken_file_name.doc
             */
            @Test
            void noAutoLinkByCitationKeyStartAtRootFolderWithSuffixMismatch(@TempDir Path root) throws Exception {
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                String citationKey = "WeDoNotCare";

                // File and folder
                String fileName = "broken_file_name.doc";
                Path fileA = root.resolve(fileName);
                Files.createFile(fileA);

                // Setup BibEntry with broken_file_name.pdf
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.addFile(new LinkedFile("", "broken_file_name.pdf", "PDF"));
                entryA.setCitationKey(citationKey);
                List<BibEntry> entries = List.of(entryA);

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                // it should not be updated
                List<LinkedFile> expect = List.of(new LinkedFile("", "broken_file_name.pdf", "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }

            /**
             * CK: WeDoNotCare
             * From
             * ├── A
             * └── A.pdf
             * to
             * └── A
             *     └── A.pdf
             */
            @Test
            void autoLinkMoveFileFromRootFolderToSubfolder(@TempDir Path root) throws Exception {
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                // File and folder before moving
                Path folderA = root.resolve("A");
                Files.createDirectory(folderA);
                Path fileA = root.resolve("A.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("WeDoNotCare");
                entryA.addFile(new LinkedFile("", "A.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the move
                Files.move(fileA, folderA.resolve("A.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }

            /**
             * CK: WeDoNotCare
             * From
             * └── A
             *     └── A.pdf
             * to
             * ├── A
             * └── A.pdf
             */
            @Test
            void autoLinkMoveFileFromSubfolderToRootFolder(@TempDir Path root) throws Exception {
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                // File and folder before moving
                Path folderA = root.resolve("A");
                Files.createDirectory(folderA);
                Path fileA = folderA.resolve("A.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("WeDoNotCare");
                entryA.addFile(new LinkedFile("", "A/A.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the move
                Files.move(fileA, root.resolve("A.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }

            /**
             * From
             * CK: WeDoNotCare
             * ├── A
             * │   └── A.pdf
             * └── B
             * to
             * ├── A
             * └── B
             *     └── A.pdf
             */
            @Test
            void autoLinkMoveFileFromSubfolderToSubfolder(@TempDir Path root) throws Exception {
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                // File and folder before moving
                Path folderA = root.resolve("A");
                Path folderB = root.resolve("B");
                Files.createDirectory(folderA);
                Files.createDirectory(folderB);
                Path fileA = folderA.resolve("A.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("WeDoNotCare");
                entryA.addFile(new LinkedFile("", "A/A.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the move
                Files.move(fileA, folderB.resolve("A.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("B/A.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }

            /**
             * CK: WeDoNotCare
             * From
             * ├── A.pdf
             * └── A
             * to
             * ├── A.pdf
             * └── A
             *     └── A.pdf
             */
            @Test
            void noAutoLinkCopyFileFromRootFolderToSubfolder(@TempDir Path root) throws Exception {
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                // File and folder before moving
                Path folderA = root.resolve("A");
                Files.createDirectory(folderA);
                Path fileA = root.resolve("A.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("WeDoNotCare");
                entryA.addFile(new LinkedFile("", "A.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the copy
                Files.copy(fileA, folderA.resolve("A.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }

            /**
             * CK: WeDoNotCare
             * From
             * └── A
             *     └── A.pdf
             * to
             * ├── A.pdf
             * └── A
             *     └── A.pdf
             */
            @Test
            void noAutoLinkCopyFileFromSubfolderToRootFolder(@TempDir Path root) throws Exception {
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                // File and folder before moving
                Path folderA = root.resolve("A");
                Files.createDirectory(folderA);
                Path fileA = folderA.resolve("A.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("WeDoNotCare");
                entryA.addFile(new LinkedFile("", "A/A.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the copy
                Files.copy(fileA, root.resolve("A.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }

            /**
             * CK: WeDoNotCare
             * From
             * ├── A
             * │   └── A.pdf
             * └── B
             * to
             * ├── A
             * │   └── A.pdf
             * └── B
             *     └── A.pdf
             */
            @Test
            void noAutoLinkCopyFileFromSubfolderToSubfolder(@TempDir Path root) throws Exception {
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                // File and folder before moving
                Path folderA = root.resolve("A");
                Path folderB = root.resolve("B");
                Files.createDirectory(folderA);
                Files.createDirectory(folderB);
                Path fileA = folderA.resolve("A.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("WeDoNotCare");
                entryA.addFile(new LinkedFile("", "A/A.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the move
                Files.copy(fileA, folderB.resolve("A.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }
        }

        @Nested
        @DisplayName("byCitationKeyAndBrokenLinkedFileName")
        class byCitationKeyAndBrokenLinkedFileName {

            /**
             * CK: AAA
             * From
             * ├── AAA.pdf
             * └── A
             * to
             * └── A
             *     └── AAA.pdf
             */
            @Test
            void autoLinkMoveFileFromRootFolderToSubFolderByBothBrokenLinkedFileNameAndCitationKey(@TempDir Path root) throws Exception {
                when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.EXACT);
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));
                // File and folder before moving
                Path folderA = root.resolve("A");
                Files.createDirectory(folderA);
                Path fileA = root.resolve("AAA.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("AAA");
                entryA.addFile(new LinkedFile("", "AAA.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the move
                Files.move(fileA, folderA.resolve("AAA.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/AAA.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }

            /**
             * CK: AAA
             * From
             * └── A
             *     └── AAA.pdf
             * to
             * ├── AAA.pdf
             * └── A
             */
            @Test
            void autoLinkMoveFileFromSubFolderToRootFolderByBothBrokenLinkedFileNameAndCitationKey(@TempDir Path root) throws Exception {
                when(autoLinkPrefs.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.EXACT);
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));
                // File and folder before moving
                Path folderA = root.resolve("A");
                Files.createDirectory(folderA);
                Path fileA = folderA.resolve("AAA.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("AAA");
                entryA.addFile(new LinkedFile("", "A/AAA.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the move
                Files.move(fileA, root.resolve("AAA.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("AAA.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }

            /**
             * CK: AAA
             * From
             * ├── A
             * │   └── AAA.pdf
             * └── B
             * to
             * ├── A
             * │   └── A.pdf
             * └── B
             *     └── AAA.pdf
             */
            @Test
            void noAutoLinkCopyFileFromSubfolderToSubfolderByBothBrokenLinkedFileNameAndCitationKey(@TempDir Path root) throws Exception {
                when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(root));

                // File and folder before moving
                Path folderA = root.resolve("A");
                Path folderB = root.resolve("B");
                Files.createDirectory(folderA);
                Files.createDirectory(folderB);
                Path fileA = folderA.resolve("AAA.pdf");
                Files.createFile(fileA);

                // Setup correct BibEntry
                BibEntry entryA = new BibEntry(StandardEntryType.Article);
                entryA.setCitationKey("AAA");
                entryA.addFile(new LinkedFile("", "A/AAA.pdf", "PDF"));
                List<BibEntry> entries = List.of(entryA);

                // Simulate the move
                Files.copy(fileA, folderB.resolve("AAA.pdf"));

                // Run auto-link
                AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext,
                        externalApplicationsPreferences, filePreferences, autoLinkPrefs);
                util.linkAssociatedFiles(entries, onLinkedFilesUpdated);

                // Check auto-link result
                List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/AAA.pdf"), "PDF"));
                List<LinkedFile> actual = entryA.getFiles();
                assertEquals(expect, actual);
            }
        }
    }
}
