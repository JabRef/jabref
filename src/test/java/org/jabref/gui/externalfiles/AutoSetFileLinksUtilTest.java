package org.jabref.gui.externalfiles;

import java.io.IOException;
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
    private Path A;
    private Path B;

    @BeforeEach
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

    @Test
    public void testFileLinksAfterMoving() throws Exception {
        // Run "Automatically set file links" - check that the bib file was not modified
        LinkedFile linkedFile = new LinkedFile("desc", path, "PDF");
        List<LinkedFile> listLinked = new ArrayList<>();
        listLinked.add(linkedFile);
        entry.setFiles(listLinked);

        // Copy Bib file from A to B
        Path destination = B.resolve("CiteKey.pdf");
        Files.copy(A.resolve("CiteKey.pdf"), destination, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(A.resolve("CiteKey.pdf"));

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, filePreferences, autoLinkPrefs);
        AutoSetFileLinksUtil.RelinkedResults results = util.relinkingFiles(entry.getFiles());
        List<LinkedFile> list = results.relinkedFiles();
        List<IOException> exceptions = results.exceptions();
        assertEquals(Collections.emptyList(), exceptions);

        // Change Entry to match required result and run method on bib
        LinkedFile linkedDestFile = new LinkedFile("desc", B.resolve("CiteKey.pdf"), "PDF");
        List<LinkedFile> listLinked2 = new ArrayList<>();
        listLinked2.add(linkedDestFile);
        entry.setFiles(listLinked2);
        assertEquals(entry.getFiles(), list);
    }
}
