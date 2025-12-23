package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import javafx.collections.FXCollections;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileUtil;
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
    void setUp(@TempDir Path folder) throws IOException {
        path = folder.resolve("CiteKey.pdf");
        Files.createFile(path);
        entry.setCitationKey("CiteKey");
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
    }

    @Test
    void findAssociatedNotLinkedFilesSuccess() throws IOException {
        when(databaseContext.getFileDirectories(any())).thenReturn(List.of(path.getParent()));
        List<LinkedFile> expected = List.of(new LinkedFile("", Path.of("CiteKey.pdf"), "PDF"));
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(expected, actual);
    }

    @Test
    void findAssociatedNotLinkedFilesForEmptySearchDir() throws IOException {
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, externalApplicationsPreferences, filePreferences, autoLinkPrefs);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(List.of(), actual);
    }

    @Test
    void relinksMovedFile(@TempDir Path tempDir) throws IOException {
        Path directory = tempDir.resolve("files");
        Path oldPath = directory.resolve("old/minimal.pdf");
        Files.createDirectories(oldPath.getParent());
        Files.createFile(oldPath);

        LinkedFile stale = new LinkedFile("", oldPath.toString(), "PDF");
        BibEntry entry = new BibEntry(StandardEntryType.Misc);
        entry.addFile(stale);

        Path newPath = directory.resolve("new/minimal.pdf");
        Files.createDirectories(newPath.getParent());
        Files.move(oldPath, newPath);

        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(filePreferences)).thenReturn(List.of(directory));

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(
                context,
                externalApplicationsPreferences,
                filePreferences,
                autoLinkPrefs);

        List<LinkedFile> matches = util.findAssociatedNotLinkedFiles(entry);

        assertEquals(1, matches.size());
        assertEquals(
                FileUtil.relativize(newPath, List.of(directory)),
                Path.of(matches.getFirst().getLink())
        );
    }

    @Test
    void findAllAssociatedNotLinkedFilesInsteadOfTheFirstOne(@TempDir Path tempDir) throws IOException {
        Path directory = tempDir.resolve("files");
        Path oldPath = directory.resolve("old/minimal.pdf");
        Files.createDirectories(oldPath.getParent());
        Files.createFile(oldPath);

        LinkedFile stale = new LinkedFile("", oldPath.toString(), "PDF");
        BibEntry entry = new BibEntry(StandardEntryType.Misc);
        entry.addFile(stale);

        // Simulate a file move
        String newPath1String = "new1/minimal.pdf";
        Path newPath1 = directory.resolve(newPath1String);
        Files.createDirectories(newPath1.getParent());
        Files.move(oldPath, newPath1);

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

        List<String> matchedFiles = util.findAssociatedNotLinkedFiles(entry)
                                        .stream().map(LinkedFile::getLink).toList();
        List<String> expected = List.of(newPath1String, newPath2String);
        // findAssociatedNotLinkedFiles does not guarantee how the returned files are ordered
        // so here we compare equality without considering order
        assertEquals(new HashSet<>(matchedFiles), new HashSet<>(expected));
    }
}
