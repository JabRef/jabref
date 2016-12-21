package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class MoveFilesCleanupTest {

    @Rule
    public TemporaryFolder bibFolder = new TemporaryFolder();

    private File pdfFolder;
    private BibDatabaseContext databaseContext;
    private MoveFilesCleanup cleanup;
    private JabRefPreferences prefs;
    private BibEntry entry;

    @Before
    public void setUp() throws IOException {
        prefs = JabRefPreferences.getInstance();
        MetaData metaData = new MetaData();
        pdfFolder = bibFolder.newFolder();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        databaseContext = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        databaseContext.setDatabaseFile(bibFolder.newFile("test.bib"));
        entry = new BibEntry();
        entry.setCiteKey("Toot");
        entry.setField("title", "test title");

    }

    @Test
    public void movesFileFromSubfolder() throws IOException {
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");
        assertTrue(fileBefore.createNewFile());
        assertTrue(new File(subfolder, "test.pdf").exists());

        ParsedFileField fileField = new ParsedFileField("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        cleanup = new MoveFilesCleanup(databaseContext, "", prefs.getFileDirectoryPreferences(),
                mock(LayoutFormatterPreferences.class));
        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        File fileAfter = new File(pdfFolder, "test.pdf");
        assertTrue(fileAfter.exists());

        assertEquals(Optional.of(FileField.getStringRepresentation(new ParsedFileField("", fileAfter.getName(), ""))),
                entry.getField("file"));
    }

    @Test
    public void movesFileFromSubfolderMultiple() throws IOException {
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");
        assertTrue(fileBefore.createNewFile());
        assertTrue(fileBefore.exists());

        ParsedFileField fileField = new ParsedFileField("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(
                Arrays.asList(new ParsedFileField("", "", ""), fileField, new ParsedFileField("", "", ""))));

        cleanup = new MoveFilesCleanup(databaseContext, "", prefs.getFileDirectoryPreferences(),
                mock(LayoutFormatterPreferences.class));
        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        File fileAfter = new File(pdfFolder, "test.pdf");
        assertTrue(fileAfter.exists());

        assertEquals(
                Optional.of(FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("", "", ""),
                        new ParsedFileField("", fileAfter.getName(), ""), new ParsedFileField("", "", "")))),
                entry.getField("file"));
    }

    @Test
    public void movesFileFromSubfolderWithFileDirPattern() throws IOException {
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");

        assertTrue(fileBefore.createNewFile());
        assertTrue(new File(subfolder, "test.pdf").exists());

        ParsedFileField fileField = new ParsedFileField("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        cleanup = new MoveFilesCleanup(databaseContext, "\\EntryType", prefs.getFileDirectoryPreferences(),
                mock(LayoutFormatterPreferences.class));
        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        Path after = pdfFolder.toPath().resolve("Misc").resolve("test.pdf");
        assertTrue(Files.exists(after));

        assertEquals(Optional
                .of(FileField.getStringRepresentation(new ParsedFileField("", after.getFileName().toString(), ""))),
                entry.getField("file"));
    }
}
