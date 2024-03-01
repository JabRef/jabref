package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import javafx.collections.FXCollections;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoSetFileLinksUtilTest {
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
    public void setUp(@TempDir Path folder) throws Exception {
        path = folder.resolve("CiteKey.pdf");
        Files.createFile(path);
        entry.setCitationKey("CiteKey");
        when(filePreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
    }

    @Test
    public void testFindAssociatedNotLinkedFilesSuccess() throws Exception {
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(path.getParent()));
        List<LinkedFile> expected = Collections.singletonList(new LinkedFile("", Path.of("CiteKey.pdf"), "PDF"));
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, filePreferences, autoLinkPrefs);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(expected, actual);
    }

    @Test
    public void testFindAssociatedNotLinkedFilesForEmptySearchDir() throws Exception {
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, filePreferences, autoLinkPrefs);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(Collections.emptyList(), actual);
    }

        /* @BeforeEach
    public void setUp(@TempDir Path folder) throws Exception {
        A = folder.resolve("A");
        Files.createDirectory(A);
        B = folder.resolve("B");
        Files.createDirectory(B);
        path = A.resolve("CiteKey.pdf");
        Files.createFile(path);
        entry.setCitationKey("CiteKey");
        when(filePreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
    } */

    @Test
    public void testFileLinksAfterMoving() throws Exception {
        // Run "Automatically set file links" - check that the bib file was not modified
        path = path.getParent();

        Path A = path.resolve("A");
        Files.createDirectory(A);
        Path B = path.resolve("B");
        Files.createDirectory(B);
        Path filePath = A.resolve("Test.pdf");
        Files.createFile(filePath);
        entry.setCitationKey("Test");

        LinkedFile linkedFile = new LinkedFile("desc", filePath, "PDF");
        List<LinkedFile> listLinked = new ArrayList<>();
        listLinked.add(linkedFile);
        entry.setFiles(listLinked);

        Path destination = B.resolve("Test.pdf");
        Files.move(A.resolve("Test.pdf"), destination, StandardCopyOption.REPLACE_EXISTING);

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, filePreferences, autoLinkPrefs);
        AutoSetFileLinksUtil.RelinkedResults results = util.relinkingFiles(entry.getFiles());
        List<LinkedFile> list = results.relinkedFiles();
        List<String> exceptions = results.exceptions();
        assertEquals(Collections.emptyList(), exceptions);

        // Change Entry to match required result and run method on bib
        LinkedFile linkedDestFile = new LinkedFile("desc", B.resolve("Test.pdf"), "PDF");
        List<LinkedFile> listLinked2 = new ArrayList<>();
        listLinked2.add(linkedDestFile);
        entry.setFiles(listLinked2);
        assertEquals(entry.getFiles(), list);

        // After this point, tested different extension along with the case where document has multiple file references
        Path filePath2 = B.resolve("Test1.txt");
        Files.createFile(filePath2);
        linkedFile = new LinkedFile("desc", filePath2, "TXT");
        listLinked.add(linkedFile);
        entry.setFiles(listLinked);

        destination = A.resolve("Test1.txt");
        Files.move(B.resolve("Test1.txt"), destination, StandardCopyOption.REPLACE_EXISTING);
        util = new AutoSetFileLinksUtil(databaseContext, filePreferences, autoLinkPrefs);
        results = util.relinkingFiles(entry.getFiles());
        list = results.relinkedFiles();
        exceptions = results.exceptions();

        listLinked2.clear();
        listLinked2.add(new LinkedFile("desc", B.resolve("Test.pdf"), "PDF"));
        listLinked2.add(new LinkedFile("desc", A.resolve("Test1.txt"), "TXT"));

        assertEquals(listLinked2, list);
    }
}
