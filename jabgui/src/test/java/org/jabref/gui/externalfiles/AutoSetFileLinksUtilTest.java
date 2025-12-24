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
    private final BiConsumer<LinkedFile, BibEntry> onLinkedFile = (linkedFile, entry) -> {
        // omit undo related logic
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

    //******************************************************************************************************
    //*********************************** Test linkAssociatedFiles *****************************************
    //******************************************************************************************************

    //******************************************************************************************************
    //************************* Part 1. test auto link only by citation key ********************************
    //******************************************************************************************************

    //******************************************************************************************************
    //************* Part 1.1 test auto link only by citation key - CitationKeyDependency.START *************
    //******************************************************************************************************

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
        util.linkAssociatedFiles(entries, onLinkedFile);

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
        util.linkAssociatedFiles(entries, onLinkedFile);

        // Check auto-link result
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of(String.format("A/B/%s", fileName)), "PDF"));
        List<LinkedFile> actual = entryA.getFiles();
        assertEquals(expect, actual);
    }

    //******************************************************************************************************
    //************* Part 1.2 test auto link only by citation key - CitationKeyDependency.EXACT *************
    //******************************************************************************************************

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
        util.linkAssociatedFiles(entries, onLinkedFile);

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
        util.linkAssociatedFiles(entries, onLinkedFile);

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
        util.linkAssociatedFiles(entries, onLinkedFile);

        // Check auto-link result
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of(String.format("A/B/%s", fileName)), "PDF"));
        List<LinkedFile> actual = entryA.getFiles();
        assertEquals(expect, actual);
    }

    //******************************************************************************************************
    //************* Part 1.3 test auto link only by citation key - CitationKeyDependency.REGEX *************
    //******************************************************************************************************

    // omitted

    //******************************************************************************************************
    //*************************** Part 2. test auto link only by file name *********************************
    //******************************************************************************************************

    /**
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
        util.linkAssociatedFiles(entries, onLinkedFile);

        // Check auto-link result
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"));
        List<LinkedFile> actual = entryA.getFiles();
        assertEquals(expect, actual);
    }

    /**
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
        util.linkAssociatedFiles(entries, onLinkedFile);

        // Check auto-link result
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A.pdf"), "PDF"));
        List<LinkedFile> actual = entryA.getFiles();
        assertEquals(expect, actual);
    }

    /**
     * From
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
        util.linkAssociatedFiles(entries, onLinkedFile);

        // Check auto-link result
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("B/A.pdf"), "PDF"));
        List<LinkedFile> actual = entryA.getFiles();
        assertEquals(expect, actual);
    }

    /**
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
        util.linkAssociatedFiles(entries, onLinkedFile);

        // Check auto-link result
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A.pdf"), "PDF"));
        List<LinkedFile> actual = entryA.getFiles();
        assertEquals(expect, actual);
    }

    /**
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
        util.linkAssociatedFiles(entries, onLinkedFile);

        // Check auto-link result
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"));
        List<LinkedFile> actual = entryA.getFiles();
        assertEquals(expect, actual);
    }

    /**
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
        util.linkAssociatedFiles(entries, onLinkedFile);

        // Check auto-link result
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"));
        List<LinkedFile> actual = entryA.getFiles();
        assertEquals(expect, actual);
    }

    //******************************************************************************************************
    //********************* Part 3. test auto link by file name and citation key ***************************
    //******************************************************************************************************
}
