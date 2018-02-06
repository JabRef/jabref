package org.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RenamePdfCleanupTest {

    @Rule public TemporaryFolder testFolder = new TemporaryFolder();
    private BibDatabaseContext context;
    private BibEntry entry;

    private FileDirectoryPreferences fileDirPrefs;
    private LayoutFormatterPreferences layoutFormatterPreferences;

    @Before
    public void setUp() throws Exception {
        MetaData metaData = new MetaData();
        context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        context.setDatabaseFile(testFolder.newFile("test.bib"));

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
    public void cleanupRenamePdfRenamesFileEvenIfOnlyDifferenceIsCase() throws IOException {
        String fileNamePattern = "[bibtexkey]";
        File tempFile = testFolder.newFile("toot.tmp");
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                mock(LayoutFormatterPreferences.class), fileDirPrefs);
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot.tmp", "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesWithMultipleFiles() throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        File tempFile = testFolder.newFile("Toot.tmp");

        entry.setField("title", "test title");
        entry.setField("file", FileFieldWriter.getStringRepresentation(Arrays.asList(new LinkedFile("", "", ""),
                new LinkedFile("", tempFile.getAbsolutePath(), ""), new LinkedFile("", "", ""))));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                mock(LayoutFormatterPreferences.class), fileDirPrefs);
        cleanup.cleanup(entry);

        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", "Toot - test title.tmp", ""))),
                entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileStartingWithBibtexKey() throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";

        File tempFile = testFolder.newFile("Toot.tmp");
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                mock(LayoutFormatterPreferences.class), fileDirPrefs);
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot - test title.tmp", "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileInSameFolder() throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        testFolder.newFile("Toot.pdf");
        LinkedFile fileField = new LinkedFile("", "Toot.pdf", "PDF");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                layoutFormatterPreferences,
                fileDirPrefs);
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupSingleField() throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        testFolder.newFile("Toot.pdf");
        LinkedFile fileField = new LinkedFile("", "Toot.pdf", "PDF");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");
        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                layoutFormatterPreferences,
                fileDirPrefs, fileField);

        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));

    }

    @Test
    public void cleanupGetTargetFilename() throws IOException {
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        testFolder.newFile("Toot.pdf");
        LinkedFile fileField = new LinkedFile("", "Toot.pdf", "PDF");
        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                layoutFormatterPreferences,
                fileDirPrefs);
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        assertEquals("Toot - test title.pdf", cleanup.getTargetFileName(fileField, entry));
    }

}
