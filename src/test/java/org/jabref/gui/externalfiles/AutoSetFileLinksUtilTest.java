package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final AutoLinkPreferences autoLinkPrefs = new AutoLinkPreferences(
            AutoLinkPreferences.CitationKeyDependency.START,
            "",
            false,
            ';');
    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final BibEntry entry = new BibEntry(StandardEntryType.Article);
    private Path path = null;

    @BeforeEach
    void setUp(@TempDir Path folder) throws Exception {
        path = folder.resolve("CiteKey.pdf");
        Files.createFile(path);
        entry.setCitationKey("CiteKey");
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
    }

    @Test
    void findAssociatedNotLinkedFilesSuccess() throws Exception {
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(path.getParent()));
        List<LinkedFile> expected = Collections.singletonList(new LinkedFile("", Path.of("CiteKey.pdf"), "PDF"));
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(expected, actual);
    }

    @Test
    void findAssociatedNotLinkedFilesForEmptySearchDir() throws Exception {
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(Collections.emptyList(), actual);
    }

    @Test
    void relinkMoveFileFromRootFolderToSubfolder(@TempDir Path folder) throws Exception {
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(folder));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        List<BibEntry> entries = new ArrayList<>();
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        Path folderA = folder.resolve("A");
        Files.createDirectory(folderA);
        Path fileA = folder.resolve("A.pdf");
        Files.createFile(fileA);
        BibEntry entryA = new BibEntry(StandardEntryType.Article);
        entryA.setCitationKey("A");
        entries.add(entryA);
        final BiConsumer<LinkedFile, BibEntry> onLinkedFile = (linkedFile, entry) -> entry.addFile(linkedFile);
        util.linkAssociatedFiles(entries, onLinkedFile);
        Files.move(fileA, folderA.resolve("A.pdf"), StandardCopyOption.REPLACE_EXISTING);
        util.linkAssociatedFiles(entries, onLinkedFile);
        List<LinkedFile> actual = entryA.getFiles();
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"));
        assertEquals(actual, expect);
    }

    @Test
    void relinkMoveFileFromSubfolderToRootFolder(@TempDir Path folder) throws Exception {
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(folder));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        List<BibEntry> entries = new ArrayList<>();
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        Path folderA = folder.resolve("A");
        Files.createDirectory(folderA);
        Path fileA = folderA.resolve("A.pdf");
        Files.createFile(fileA);
        BibEntry entryA = new BibEntry(StandardEntryType.Article);
        entryA.setCitationKey("A");
        entries.add(entryA);
        final BiConsumer<LinkedFile, BibEntry> onLinkedFile = (linkedFile, entry) -> entry.addFile(linkedFile);
        util.linkAssociatedFiles(entries, onLinkedFile);
        Files.move(fileA, folder.resolve("A.pdf"), StandardCopyOption.REPLACE_EXISTING);
        util.linkAssociatedFiles(entries, onLinkedFile);
        List<LinkedFile> actual = entryA.getFiles();
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A.pdf"), "PDF"));
        assertEquals(actual, expect);
    }

    @Test
    void relinkMoveFileFromSubfolderToSubfolder(@TempDir Path folder) throws Exception {
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(folder));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        List<BibEntry> entries = new ArrayList<>();
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        Path folderA = folder.resolve("A");
        Path folderB = folder.resolve("B");
        Files.createDirectory(folderA);
        Files.createDirectory(folderB);
        Path fileA = folderA.resolve("A.pdf");
        Files.createFile(fileA);
        BibEntry entryA = new BibEntry(StandardEntryType.Article);
        entryA.setCitationKey("A");
        entries.add(entryA);
        final BiConsumer<LinkedFile, BibEntry> onLinkedFile = (linkedFile, entry) -> entry.addFile(linkedFile);
        util.linkAssociatedFiles(entries, onLinkedFile);
        Files.move(fileA, folderB.resolve("A.pdf"), StandardCopyOption.REPLACE_EXISTING);
        util.linkAssociatedFiles(entries, onLinkedFile);
        List<LinkedFile> actual = entryA.getFiles();
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("B/A.pdf"), "PDF"));
        assertEquals(actual, expect);
    }

    @Test
    void noRelinkCopyFileFromRootFolderToSubfolder(@TempDir Path folder) throws Exception {
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(folder));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        List<BibEntry> entries = new ArrayList<>();
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        Path folderA = folder.resolve("A");
        Files.createDirectory(folderA);
        Path fileA = folder.resolve("A.pdf");
        Files.createFile(fileA);
        BibEntry entryA = new BibEntry(StandardEntryType.Article);
        entryA.setCitationKey("A");
        entries.add(entryA);
        final BiConsumer<LinkedFile, BibEntry> onLinkedFile = (linkedFile, entry) -> entry.addFile(linkedFile);
        util.linkAssociatedFiles(entries, onLinkedFile);
        Files.copy(fileA, folderA.resolve("A.pdf"), StandardCopyOption.REPLACE_EXISTING);
        util.linkAssociatedFiles(entries, onLinkedFile);
        List<LinkedFile> actual = entryA.getFiles();
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A.pdf"), "PDF"), new LinkedFile("", Path.of("A/A.pdf"), "PDF"));
        assertEquals(actual, expect);
    }

    @Test
    void noRelinkCopyFileFromSubfolderToRootFolder(@TempDir Path folder) throws Exception {
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(folder));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        List<BibEntry> entries = new ArrayList<>();
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        Path folderA = folder.resolve("A");
        Files.createDirectory(folderA);
        Path fileA = folderA.resolve("A.pdf");
        Files.createFile(fileA);
        BibEntry entryA = new BibEntry(StandardEntryType.Article);
        entryA.setCitationKey("A");
        entries.add(entryA);
        final BiConsumer<LinkedFile, BibEntry> onLinkedFile = (linkedFile, entry) -> entry.addFile(linkedFile);
        util.linkAssociatedFiles(entries, onLinkedFile);
        Files.copy(fileA, folder.resolve("A.pdf"), StandardCopyOption.REPLACE_EXISTING);
        util.linkAssociatedFiles(entries, onLinkedFile);
        List<LinkedFile> actual = entryA.getFiles();
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"),new LinkedFile("", Path.of("A.pdf"), "PDF"));
        assertEquals(actual, expect);
    }

    @Test
    void noRelinkCopyFileFromSubfolderToSubfolder(@TempDir Path folder) throws Exception {
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(folder));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        List<BibEntry> entries = new ArrayList<>();
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        Path folderA = folder.resolve("A");
        Path folderB = folder.resolve("B");
        Files.createDirectory(folderA);
        Files.createDirectory(folderB);
        Path fileA = folderA.resolve("A.pdf");
        Files.createFile(fileA);
        BibEntry entryA = new BibEntry(StandardEntryType.Article);
        entryA.setCitationKey("A");
        entries.add(entryA);
        final BiConsumer<LinkedFile, BibEntry> onLinkedFile = (linkedFile, entry) -> entry.addFile(linkedFile);
        util.linkAssociatedFiles(entries, onLinkedFile);
        Files.copy(fileA, folderB.resolve("A.pdf"), StandardCopyOption.REPLACE_EXISTING);
        util.linkAssociatedFiles(entries, onLinkedFile);
        List<LinkedFile> actual = entryA.getFiles();
        List<LinkedFile> expect = List.of(new LinkedFile("", Path.of("A/A.pdf"), "PDF"), new LinkedFile("", Path.of("B/A.pdf"), "PDF"));
        assertEquals(actual, expect);
    }
}
