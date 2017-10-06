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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoveFilesCleanupTest {

    @Rule public TemporaryFolder bibFolder = new TemporaryFolder();

    private File pdfFolder;
    private BibDatabaseContext databaseContext;
    private MoveFilesCleanup cleanup;
    private BibEntry entry;
    private FileDirectoryPreferences fileDirPrefs;

    @Before
    public void setUp() throws IOException {
        MetaData metaData = new MetaData();
        pdfFolder = bibFolder.newFolder();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        databaseContext = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        databaseContext.setDatabaseFile(bibFolder.newFile("test.bib"));
        entry = new BibEntry();
        entry.setCiteKey("Toot");
        entry.setField("title", "test title");

        fileDirPrefs = mock(FileDirectoryPreferences.class);
        when(fileDirPrefs.isBibLocationAsPrimary()).thenReturn(false); //Biblocation as Primary overwrites all other dirs, therefore we set it to false here
    }

    @Test
    public void movesFileFromSubfolder() throws IOException {
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");
        assertTrue(fileBefore.createNewFile());
        assertTrue(new File(subfolder, "test.pdf").exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        cleanup = new MoveFilesCleanup(databaseContext, "", fileDirPrefs,
                mock(LayoutFormatterPreferences.class));
        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        File fileAfter = new File(pdfFolder, "test.pdf");
        assertTrue(fileAfter.exists());

        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", fileAfter.getName(), ""))),
                entry.getField("file"));
    }

    @Test
    public void movesFileFromSubfolderMultiple() throws IOException {
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");
        assertTrue(fileBefore.createNewFile());
        assertTrue(fileBefore.exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(
                Arrays.asList(new LinkedFile("", "", ""), fileField, new LinkedFile("", "", ""))));

        cleanup = new MoveFilesCleanup(databaseContext, "", fileDirPrefs,
                mock(LayoutFormatterPreferences.class));
        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        File fileAfter = new File(pdfFolder, "test.pdf");
        assertTrue(fileAfter.exists());

        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", fileAfter.getName(), ""))),
                entry.getField("file"));
    }

    @Test
    public void movesFileFromSubfolderWithFileDirPattern() throws IOException {
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");

        assertTrue(fileBefore.createNewFile());
        assertTrue(new File(subfolder, "test.pdf").exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        cleanup = new MoveFilesCleanup(databaseContext, "[entrytype]", fileDirPrefs,
                mock(LayoutFormatterPreferences.class));
        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        Path after = pdfFolder.toPath().resolve("Misc").resolve("test.pdf");
        Path relativefileDir = pdfFolder.toPath().relativize(after);
        assertTrue(Files.exists(after));

        assertEquals(Optional
                .of(FileFieldWriter.getStringRepresentation(new LinkedFile("", relativefileDir.toString(), ""))),
                entry.getField("file"));
    }

    @Test
    public void movesFileFromSubfolderWithSubdirPattern() throws IOException {
        BibEntry local_entry = (BibEntry) entry.clone();
        local_entry.setField("year", "1989");
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");

        assertTrue(fileBefore.createNewFile());
        assertTrue(new File(subfolder, "test.pdf").exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        local_entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        cleanup = new MoveFilesCleanup(databaseContext, "[year]", fileDirPrefs,
                mock(LayoutFormatterPreferences.class));
        cleanup.cleanup(local_entry);

        assertFalse(fileBefore.exists());
        Path after = pdfFolder.toPath().resolve("1989").resolve("test.pdf");
        Path relativefileDir = pdfFolder.toPath().relativize(after);
        assertTrue(Files.exists(after));

        assertEquals(Optional
                .of(FileFieldWriter.getStringRepresentation(new LinkedFile("", relativefileDir.toString(), ""))),
                local_entry.getField("file"));
    }

    @Test
    public void movesFileFromSubfolderWithDeepSubdirPattern() throws IOException {
        BibEntry local_entry = (BibEntry) entry.clone();
        local_entry.setField("year", "1989");
        local_entry.setField("author", "O. Kitsune");
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");

        assertTrue(fileBefore.createNewFile());
        assertTrue(new File(subfolder, "test.pdf").exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        local_entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        cleanup = new MoveFilesCleanup(databaseContext, "[entrytype]/[year]/[auth]", fileDirPrefs,
                mock(LayoutFormatterPreferences.class));
        cleanup.cleanup(local_entry);

        assertFalse(fileBefore.exists());
        Path after = pdfFolder.toPath().resolve("Misc").resolve("1989").resolve("Kitsune").resolve("test.pdf");
        Path relativefileDir = pdfFolder.toPath().relativize(after);
        assertTrue(Files.exists(after));

        assertEquals(Optional
                .of(FileFieldWriter.getStringRepresentation(new LinkedFile("", relativefileDir.toString(), ""))),
                local_entry.getField("file"));
    }
}
