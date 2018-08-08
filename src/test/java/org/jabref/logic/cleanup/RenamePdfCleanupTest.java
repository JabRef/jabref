package org.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(TempDirectory.class)
public class RenamePdfCleanupTest {

    private BibDatabaseContext context;
    private BibEntry entry;

    private FileDirectoryPreferences fileDirPrefs;
    private LayoutFormatterPreferences layoutFormatterPreferences;

    @BeforeEach
    public void setUp(@TempDirectory.TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("test.bib");
        MetaData metaData = new MetaData();
        context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        context.setDatabaseFile(path.toFile());

        fileDirPrefs = mock(FileDirectoryPreferences.class);
        when(fileDirPrefs.isBibLocationAsPrimary()).thenReturn(true); //Set Biblocation as Primary Directory, otherwise the tmp folders won't be cleaned up correctly
        entry = new BibEntry();
        entry.setCiteKey("Toot");
        layoutFormatterPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    /**
     * Test for #466
     */
    @Test
    public void cleanupRenamePdfRenamesFileEvenIfOnlyDifferenceIsCase(@TempDirectory.TempDir Path testFolder) throws IOException {
        String fileNamePattern = "[bibtexkey]";
        Path path = testFolder.resolve("toot.tmp");
        Files.createFile(path);
        File tempFile = path.toFile();

        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                fileDirPrefs);
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot.tmp", "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesWithMultipleFiles(@TempDirectory.TempDir Path testFolder) throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        Path path = testFolder.resolve("Toot.tmp");
        Files.createFile(path);
        File tempFile = path.toFile();

        entry.setField("title", "test title");
        entry.setField("file", FileFieldWriter.getStringRepresentation(Arrays.asList(new LinkedFile("", "", ""),
                new LinkedFile("", tempFile.getAbsolutePath(), ""), new LinkedFile("", "", ""))));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                fileDirPrefs);
        cleanup.cleanup(entry);

        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", "Toot - test title.tmp", ""))),
                entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileStartingWithBibtexKey(@TempDirectory.TempDir Path testFolder) throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        Path path = testFolder.resolve("Toot.tmp");
        Files.createFile(path);

        File tempFile = path.toFile();
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                fileDirPrefs);
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot - test title.tmp", "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileInSameFolder(@TempDirectory.TempDir Path testFolder) throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        Path path = testFolder.resolve("Toot.pdf");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", "Toot.pdf", "PDF");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPrefs);
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupSingleField(@TempDirectory.TempDir Path testFolder) throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        Path path = testFolder.resolve("Toot.pdf");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", "Toot.pdf", "PDF");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");
        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                fileDirPrefs, fileField);

        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));

    }

    @Test
    public void cleanupGetTargetFilename(@TempDirectory.TempDir Path testFolder) throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        Path path = testFolder.resolve("Toot.pdf");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", "Toot.pdf", "PDF");
        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPrefs);
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        assertEquals("Toot - test title.pdf", cleanup.getTargetFileName(fileField, entry));
    }

}
