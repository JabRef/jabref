package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RenamePdfCleanupTest {

    private BibEntry entry;

    private FilePreferences filePreferences;
    private RenamePdfCleanup cleanup;

    @BeforeEach
    void setUp(@TempDir Path testFolder) {
        Path path = testFolder.resolve("test.bib");
        MetaData metaData = new MetaData();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        context.setDatabaseFile(path);

        entry = new BibEntry();
        entry.setCiteKey("Toot");

        filePreferences = mock(FilePreferences.class);
        when(filePreferences.isBibLocationAsPrimary()).thenReturn(true); //Set Biblocation as Primary Directory, otherwise the tmp folders won't be cleaned up correctly
        cleanup = new RenamePdfCleanup(false, context, filePreferences);
    }

    /**
     * Test for #466
     */
    @Test
    void cleanupRenamePdfRenamesFileEvenIfOnlyDifferenceIsCase(@TempDir Path testFolder) throws IOException {
        Path path = testFolder.resolve("toot.tmp");
        Files.createFile(path);

        LinkedFile fileField = new LinkedFile("", path.toAbsolutePath().toString(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot.tmp", "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    void cleanupRenamePdfRenamesWithMultipleFiles(@TempDir Path testFolder) throws IOException {
        Path path = testFolder.resolve("Toot.tmp");
        Files.createFile(path);

        entry.setField("title", "test title");
        entry.setField("file", FileFieldWriter.getStringRepresentation(
                Arrays.asList(new LinkedFile("", "", ""), new LinkedFile("", path.toAbsolutePath().toString(), ""), new LinkedFile("", "", ""))));

        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey] - [fulltitle]");
        cleanup.cleanup(entry);

        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(
                        Arrays.asList(new LinkedFile("", "", ""), new LinkedFile("", "Toot - test title.tmp", ""), new LinkedFile("", "", "")))),
                entry.getField("file"));
    }

    @Test
    void cleanupRenamePdfRenamesFileStartingWithBibtexKey(@TempDir Path testFolder) throws IOException {
        Path path = testFolder.resolve("Toot.tmp");
        Files.createFile(path);

        LinkedFile fileField = new LinkedFile("", path.toAbsolutePath().toString(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey] - [fulltitle]");
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot - test title.tmp", "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    void cleanupRenamePdfRenamesFileInSameFolder(@TempDir Path testFolder) throws IOException {
        Path path = testFolder.resolve("Toot.pdf");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", "Toot.pdf", "PDF");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey] - [fulltitle]");
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }
}
