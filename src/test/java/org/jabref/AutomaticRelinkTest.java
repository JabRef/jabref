package org.jabref;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutomaticRelinkTest {

    private Path main;
    private Path B;
    private Path A;
    private Path beforeFilePath;
    private BibEntry entry;
    private FilePreferences filePreferences;
    private BibDatabaseContext databaseContext;
    private String defaultDirectory;

    /**
     * `MoveFilesCleanupTest` Used as an example of how to do this
     *
     * @param bibFolder
     * @throws IOException
     */
    @BeforeEach
    void setUp(@TempDir Path bibFolder) throws IOException {
        // Create Main
        main = bibFolder.resolve("main");
        Files.createDirectory(main);

        // Create B Directory
        B = main.resolve("B");
        Files.createDirectory(B);

        // Create A Directory & test file
        A = main.resolve("A");
        Files.createDirectory(A);
        beforeFilePath = A.resolve("test.pdf");
        Files.createFile(beforeFilePath);

        MetaData metaData = new MetaData();
        defaultDirectory = main.toAbsolutePath().toString();
        metaData.setDefaultFileDirectory(defaultDirectory);
        databaseContext = new BibDatabaseContext(new BibDatabase(), metaData);
        Files.createFile(bibFolder.resolve("test.bib"));
        databaseContext.setDatabasePath(bibFolder.resolve("test.bib"));

        entry = new BibEntry();
        LinkedFile linkedFile = new LinkedFile("desc", beforeFilePath, "");
        List<LinkedFile> listLinked = new ArrayList<>();
        listLinked.add(linkedFile);
        entry.setFiles(listLinked);

        filePreferences = mock(FilePreferences.class);
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false); // Biblocation as Primary overwrites all other dirs, therefore we set it to false here
    }

    /**
     * Run
     * on
     * empty
     * Bib
     * Entry
     */
    @Test
    void checkEmpty() {
        System.out.println(beforeFilePath);
        BibEntry bib = new BibEntry();
        AutomaticRelink.relink(bib, defaultDirectory);
        BibEntry empty = new BibEntry();
        assertEquals(empty, bib);
    }

    /**
     * Check that relink returns the same entries
     */
    @Test
    void checkSame() {
        BibEntry bib = new BibEntry();
        LinkedFile linkedFile = new LinkedFile("desc", beforeFilePath, "");
        List<LinkedFile> listLinked = new ArrayList<>();
        listLinked.add(linkedFile);
        bib.setFiles(listLinked);
        AutomaticRelink.relink(bib, defaultDirectory);
        assertEquals(entry, bib);
    }

    /**
     * Check that it stays the same once copied
     */
    @Test
    void checkCopy() {
        // Run "Automatically set file links" - check that the bib file was not modified
        BibEntry bib = new BibEntry();
        LinkedFile linkedFile = new LinkedFile("desc", beforeFilePath, "");
        List<LinkedFile> listLinked = new ArrayList<>();
        listLinked.add(linkedFile);
        bib.setFiles(listLinked);
        AutomaticRelink.relink(bib, defaultDirectory);
        assertEquals(entry, bib);

        // Copy Bib file from A to B
        Path destination = B.resolve("test.pdf");
        try {
            Files.copy(A.resolve("test.pdf"), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (
                IOException e) {
            e.printStackTrace();
            fail("File copy failed!");
        }

        AutomaticRelink.relink(bib, defaultDirectory);
        assertEquals(entry, bib);
    }

    /**
     * Check that the file was relinked once moved
     */
    @Test
    void relinkTest() {
        // Run "Automatically set file links" - check that the bib file was not modified
        BibEntry bib = new BibEntry();
        LinkedFile linkedFile = new LinkedFile("desc", beforeFilePath, "");
        List<LinkedFile> listLinked = new ArrayList<>();
        listLinked.add(linkedFile);
        bib.setFiles(listLinked);
        AutomaticRelink.relink(bib, defaultDirectory);
        assertEquals(entry, bib, "1");

        // Copy Bib file from A to B
        Path destination = B.resolve("test.pdf");
        try {
            Files.copy(A.resolve("test.pdf"), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (
                IOException e) {
            e.printStackTrace();
            fail("File copy failed!");
        }
        // Delete old file
        try {
            Files.deleteIfExists(A.resolve("test.pdf"));
        } catch (
                IOException e) {
            e.printStackTrace();
            fail("Delete File Failed");
        }

        // Change Entry to match required result and run method on bib
        LinkedFile linkedDestFile = new LinkedFile("desc", B.resolve("test.pdf"), "");
        List<LinkedFile> listLinked2 = new ArrayList<>();
        listLinked2.add(linkedDestFile);
        entry.setFiles(listLinked2);
        AutomaticRelink.relink(bib, defaultDirectory);
        assertEquals(entry, bib, "Something went wrong");
    }
}
