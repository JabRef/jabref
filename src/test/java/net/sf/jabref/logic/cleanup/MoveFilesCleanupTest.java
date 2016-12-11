package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

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

public class MoveFilesCleanupTest {

    @Rule
    public TemporaryFolder bibFolder = new TemporaryFolder();

    private File pdfFolder;
    private BibDatabaseContext databaseContext;
    private MoveFilesCleanup cleanup;

    @Before
    public void setUp() throws IOException {
        pdfFolder = bibFolder.newFolder();
        MetaData metaData = new MetaData();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        databaseContext = new BibDatabaseContext(new BibDatabase(), metaData, bibFolder.newFile("test.bib"));

        cleanup = new MoveFilesCleanup(databaseContext, JabRefPreferences.getInstance().getFileDirectoryPreferences());
    }

    @Test
    public void movesFileFromSubfolder() throws IOException {
        File subfolder = bibFolder.newFolder();
        File fileBefore = new File(subfolder, "test.pdf");
        assertTrue(fileBefore.createNewFile());
        assertTrue(new File(subfolder, "test.pdf").exists());

        BibEntry entry = new BibEntry();
        ParsedFileField fileField = new ParsedFileField("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

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

        BibEntry entry = new BibEntry();
        ParsedFileField fileField = new ParsedFileField("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("","",""), fileField, new ParsedFileField("","",""))));

        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        File fileAfter = new File(pdfFolder, "test.pdf");
        assertTrue(fileAfter.exists());

        assertEquals(
                Optional.of(FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("", "", ""),
                        new ParsedFileField("", fileAfter.getName(), ""), new ParsedFileField("", "", "")))),
                entry.getField("file"));
    }
}
