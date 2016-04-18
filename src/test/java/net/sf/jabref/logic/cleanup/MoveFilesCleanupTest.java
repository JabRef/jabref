package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
        Globals.prefs = JabRefPreferences.getInstance();

        pdfFolder = bibFolder.newFolder();
        MetaData metaData = new MetaData();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        databaseContext = new BibDatabaseContext(new BibDatabase(), metaData, bibFolder.newFile("test.bib"));

        cleanup = new MoveFilesCleanup(databaseContext);
    }

    @Test
    public void movesFileFromSubfolder() throws IOException {
        File subfolder = bibFolder.newFolder();
        File tempFile = new File(subfolder, "test.pdf");
        tempFile.createNewFile();
        assertTrue(new File(subfolder, "test.pdf").exists());

        BibEntry entry = new BibEntry();
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        cleanup.cleanup(entry);

        assertFalse(new File(subfolder, "test.pdf").exists());
        assertTrue(new File(pdfFolder, "test.pdf").exists());
    }
}